package com.botter.shop.seckill.scripts;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 批量注册 + 登录用户，产出 JMeter 可直接读取的数据文件。
 *
 * <p>零依赖：只用 JDK 自带类（java.net.http / MessageDigest），不需要编译打包，
 * JDK 11+ 直接以单文件源码模式运行：
 * <pre>
 *     java GenUsers.java --count 1000 --password 123456
 * </pre>
 *
 * <p>密码链路完整复刻前端（与 common 模块 MD5Util 一一对应），所以生成的用户
 * 前端和 JMeter 都能直接用：
 * <pre>
 *     前端 formPass = md5(salt[0] + salt[2] + 明文 + salt[5] + salt[4])，固定盐 1a2b3c4d
 *     后端 dbPass   = md5(dbSalt[0] + dbSalt[2] + formPass + dbSalt[5] + dbSalt[4])
 * </pre>
 *
 * <p>产出文件（默认写到脚本同级的 data 目录）：
 * <ul>
 *   <li>users.csv        mobile,password,formPass,nickname[,userId] —— JMeter CSV Data Set Config 直接读</li>
 *   <li>tokens.csv       mobile,token —— 想跳过登录步骤时直接用（注意有效期）</li>
 *   <li>users_failed.csv mobile,stage,httpStatus,code,msg —— 失败明细，便于排查后重跑</li>
 * </ul>
 */
public class GenUsers {

    /** 必须与 common 模块 MD5Util.FORM_SALT 保持一致 */
    private static final String FORM_SALT = "1a2b3c4d";

    private static final String PATH_REGISTER = "/user/register/register";
    private static final String PATH_LOGIN = "/user/login/dologin";
    private static final String PATH_INFO = "/user/info";

    private static final Pattern P_CODE = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern P_MSG = Pattern.compile("\"msg\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern P_ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    private static PrintStream out;

    // ---------------- 配置 ----------------
    static String url = "http://localhost:8081";
    static String startMobile = "13900000001";
    static int count = 2000;
    static String password = "123456";
    static String nicknamePrefix = "jmeter";
    static int concurrency = 20;
    static int timeoutSec = 10;
    static boolean doLogin = true;
    static boolean fetchInfo = false;
    static boolean writeHeader = false;
    static boolean dryRun = false;
    static Path outDir = defaultOutDir();

    /**
     * 默认输出目录：自动定位到 seckill-service 模块的 scripts/data
     * （JMeter 的 CSV Data Set Config 也指向这里，生成完直接就能被读到）。
     * 从当前目录向上最多找 5 层，哪层有 seckill-service 子目录就认哪层是模块根；
     * 找不到（比如在项目外运行）就退回 当前目录/data，任何时候都可用 --out-dir 覆盖。
     */
    static Path defaultOutDir() {
        Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 5 && p != null; i++, p = p.getParent()) {
            if (Files.isDirectory(p.resolve("seckill-service"))) {
                return p.resolve("seckill-service/src/main/java/com/botter/shop/seckill/scripts/data");
            }
        }
        return Path.of(System.getProperty("user.dir"), "data");
    }

    // ---------------- 入口 ----------------
    public static void main(String[] args) throws Exception {
        out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        parseArgs(args);

        List<String> mobiles = genMobiles(startMobile, count);

        if (dryRun) {
            String fp = formPass(password);
            out.println("[dry-run] 将生成 " + mobiles.size() + " 个手机号：");
            out.println("  起：" + mobiles.get(0) + "   止：" + mobiles.get(mobiles.size() - 1));
            for (int i = 0; i < Math.min(10, mobiles.size()); i++) {
                out.println("  " + mobiles.get(i) + "  formPass=" + fp);
            }
            if (mobiles.size() > 10) {
                out.println("  ... 其余 " + (mobiles.size() - 10) + " 个省略");
            }
            return;
        }

        Files.createDirectories(outDir);
        String formPass = formPass(password);
        long t0 = System.currentTimeMillis();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSec))
                .build();
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);

        try {
            // 1. 批量注册
            Result[] regs = new Result[mobiles.size()];
            AtomicInteger okReg = new AtomicInteger();
            AtomicInteger existReg = new AtomicInteger();
            runBatch(pool, mobiles, i -> {
                Result r = postForm(client, url + PATH_REGISTER,
                        "mobile=" + enc(mobiles.get(i))
                                + "&password=" + enc(formPass)
                                + "&nickname=" + enc(nicknamePrefix + "_" + (i + 1)), null);
                regs[i] = r;
                if (r.code == 200) {
                    okReg.incrementAndGet();
                } else if (isExistError(r.msg)) {
                    existReg.incrementAndGet();
                }
            });

            // 2. 批量登录，拿 satoken
            String[] tokens = new String[mobiles.size()];
            Result[] loginRes = new Result[mobiles.size()];   // 单独保存登录结果，不污染 regs
            AtomicInteger okLogin = new AtomicInteger();
            if (doLogin) {
                runBatch(pool, mobiles, i -> {
                    Result r = postForm(client, url + PATH_LOGIN,
                            "mobile=" + enc(mobiles.get(i)) + "&password=" + enc(formPass), null);
                    loginRes[i] = r;
                    if (r.code == 200 && r.dataString != null) {
                        tokens[i] = r.dataString;
                        okLogin.incrementAndGet();
                    }
                });
            }

            // 3. 可选：拿 userId
            long[] userIds = new long[mobiles.size()];
            AtomicInteger okInfo = new AtomicInteger();
            if (fetchInfo) {
                runBatch(pool, mobiles, i -> {
                    if (tokens[i] == null) {
                        return;
                    }
                    Result r = postForm(client, url + PATH_INFO, "", tokens[i]);
                    if (r.code == 200 && r.dataId != null) {
                        userIds[i] = r.dataId;
                        okInfo.incrementAndGet();
                    }
                });
            }

            // 4. 写文件
            Path usersPath = outDir.resolve("users.csv");
            List<String> failed = new ArrayList<>();
            failed.add("mobile,stage,httpStatus,code,msg");

            try (PrintStream w = new PrintStream(Files.newOutputStream(usersPath), true, StandardCharsets.UTF_8)) {
                if (writeHeader) {
                    w.println(fetchInfo ? "mobile,password,formPass,nickname,userId"
                            : "mobile,password,formPass,nickname");
                }
                for (int i = 0; i < mobiles.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(mobiles.get(i)).append(',')
                            .append(csv(password)).append(',')
                            .append(formPass).append(',')
                            .append(csv(nicknamePrefix + "_" + (i + 1)));
                    if (fetchInfo) {
                        sb.append(',').append(userIds[i] == 0 ? "" : userIds[i]);
                    }
                    w.println(sb);
                }
            }

            int tokenCount = 0;
            if (doLogin) {
                Path tokensPath = outDir.resolve("tokens.csv");
                try (PrintStream w = new PrintStream(Files.newOutputStream(tokensPath), true, StandardCharsets.UTF_8)) {
                    if (writeHeader) {
                        w.println("mobile,token");
                    }
                    for (int i = 0; i < mobiles.size(); i++) {
                        if (tokens[i] != null) {
                            w.println(mobiles.get(i) + "," + tokens[i]);
                            tokenCount++;
                        }
                    }
                }
            }

            for (int i = 0; i < mobiles.size(); i++) {
                Result r = regs[i];
                boolean regBad = r.code != 200 && !isExistError(r.msg);
                if (regBad) {
                    // 注册本身失败：登录必然连带失败，记一条就够
                    failed.add(mobiles.get(i) + ",register," + r.httpStatus + "," + r.code + "," + csv(r.msg));
                    continue;
                }
                // 注册成功/已存在，但没拿到 token = 登录失败，记录登录的真实错误
                if (doLogin && tokens[i] == null && loginRes[i] != null) {
                    Result lr = loginRes[i];
                    failed.add(mobiles.get(i) + ",login," + lr.httpStatus + "," + lr.code + "," + csv(lr.msg));
                }
            }
            Path failedPath = outDir.resolve("users_failed.csv");
            Files.write(failedPath, failed, StandardCharsets.UTF_8);

            // 5. 汇总
            double elapsed = (System.currentTimeMillis() - t0) / 1000.0;
            out.println("=".repeat(60));
            out.println("网关地址      ：" + url);
            out.println("用户数        ：" + count + "（" + mobiles.get(0) + " ~ " + mobiles.get(mobiles.size() - 1) + "）");
            out.println("明文密码      ：" + password);
            out.println("formPass      ：" + formPass + "  （JMeter 登录时直接传这个）");
            out.println("耗时          ：" + String.format("%.2f", elapsed) + "s");
            out.println("-".repeat(60));
            out.println("  注册成功      ：" + okReg.get());
            out.println("  已存在(跳过)  ：" + existReg.get());
            out.println("  注册失败      ：" + (mobiles.size() - okReg.get() - existReg.get()));
            if (doLogin) {
                out.println("  登录成功      ：" + okLogin.get() + "/" + mobiles.size());
            }
            if (fetchInfo) {
                out.println("  拿到 userId   ：" + okInfo.get() + "/" + mobiles.size());
            }
            out.println("-".repeat(60));
            out.println("  users.csv       -> " + usersPath.toAbsolutePath());
            if (doLogin) {
                out.println("  tokens.csv      -> " + outDir.resolve("tokens.csv").toAbsolutePath());
            }
            out.println("  users_failed.csv-> " + failedPath.toAbsolutePath());
            out.println("=".repeat(60));

            if (doLogin && tokenCount > 0) {
                out.println();
                out.println("提示：token 有效期只有 3600 秒（UserService 里 StpUtil.login(id, 3600L)），");
                out.println("      压测超过 1 小时请重新生成，或让 JMeter 每个线程自己走登录接口。");
            }
        } finally {
            pool.shutdown();
        }
    }

    // ---------------- 批量执行 ----------------
    interface Task {
        void run(int index) throws Exception;
    }

    /** 把 index 0..n-1 的任务丢进线程池并发跑，等全部结束（异常不中断，只打印） */
    static void runBatch(ExecutorService pool, List<String> mobiles, Task task) throws Exception {
        List<Future<?>> futures = new ArrayList<>(mobiles.size());
        for (int i = 0; i < mobiles.size(); i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                try {
                    task.run(idx);
                } catch (Exception e) {
                    out.println("  [任务异常] index=" + idx + " " + e);
                }
                return null;
            }));
        }
        for (Future<?> f : futures) {
            f.get();
        }
    }

    // ---------------- HTTP ----------------
    /** 注册/登录接口的参数都没有 @RequestBody，只能走表单，不能发 JSON */
    static Result postForm(HttpClient client, String url, String body, String token) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSec))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (token != null && !token.isEmpty()) {
                b.header("satoken", token);
            }
            HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return Result.parse(resp.statusCode(), resp.body());
        } catch (IOException e) {
            return new Result(0, -1, "网络异常: " + e.getClass().getSimpleName() + ": " + e.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(0, -1, "请求被中断", null);
        } catch (Exception e) {
            return new Result(0, -1, "异常: " + e, null);
        }
    }

    static class Result {
        int httpStatus;
        int code;
        String msg;
        String dataString;
        Long dataId;

        Result(int httpStatus, int code, String msg, String dataString) {
            this.httpStatus = httpStatus;
            this.code = code;
            this.msg = msg == null ? "" : msg;
            this.dataString = dataString;
        }

        /** 极简解析：只取 code / msg / data，避免为了一个脚本引 Jackson */
        static Result parse(int httpStatus, String json) {
            if (json == null || json.isBlank()) {
                return new Result(httpStatus, -1, "空响应体", null);
            }
            int code = -1;
            Matcher mc = P_CODE.matcher(json);
            if (mc.find()) {
                code = Integer.parseInt(mc.group(1));
            }
            String msg = "";
            Matcher mm = P_MSG.matcher(json);
            if (mm.find()) {
                msg = mm.group(1);
            }
            String dataStr = null;
            Long id = null;
            int di = json.indexOf("\"data\"");
            if (di >= 0) {
                int p = json.indexOf(':', di + 6);
                if (p > 0) {
                    p++;
                    while (p < json.length() && Character.isWhitespace(json.charAt(p))) {
                        p++;
                    }
                    if (p < json.length()) {
                        char c = json.charAt(p);
                        if (c == '"') {
                            int end = json.indexOf('"', p + 1);
                            if (end > p) {
                                dataStr = json.substring(p + 1, end);
                            }
                        } else if (c == '{') {
                            Matcher mi = P_ID.matcher(json.substring(p));
                            if (mi.find()) {
                                id = Long.parseLong(mi.group(1));
                            }
                        }
                    }
                }
            }
            Result r = new Result(httpStatus, code, msg, dataStr);
            r.dataId = id;
            return r;
        }
    }

    /**
     * 手机号已注册属于「可跳过的已存在」，不记为失败。
     * 实测 user-service 返回的是「该手机号已注册」（不含"存在"二字），
     * 所以这里要把几种常见措辞都覆盖到，否则重跑时会误判成失败。
     */
    static boolean isExistError(String msg) {
        String m = msg == null ? "" : msg;
        String upper = m.toUpperCase();
        return m.contains("已注册") || m.contains("已存在") || m.contains("存在")
                || upper.contains("EXIST") || upper.contains("DUPLICATE");
    }

    // ---------------- 工具 ----------------
    /** 明文密码 -> 前端传输密文，与 MD5Util.inputPassToFormPass 完全一致 */
    static String formPass(String plain) {
        String s = FORM_SALT;
        String raw = "" + s.charAt(0) + s.charAt(2) + plain + s.charAt(5) + s.charAt(4);
        return md5(raw);
    }

    static String md5(String src) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(src.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    static List<String> genMobiles(String start, int n) {
        if (start == null || !start.matches("1\\d{10}")) {
            out.println("[参数错误] 起始手机号非法：" + start + "，需要 11 位数字且以 1 开头");
            System.exit(1);
        }
        long base = Long.parseLong(start);
        List<String> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(String.valueOf(base + i));
        }
        if (list.get(list.size() - 1).length() != 11) {
            out.println("[参数错误] 号段溢出：" + start + " + " + n + " 超过 11 位，请换起始号段或减少数量");
            System.exit(1);
        }
        return list;
    }

    static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--no-login" -> doLogin = false;
                case "--fetch-info" -> fetchInfo = true;
                case "--header" -> writeHeader = true;
                case "--dry-run" -> dryRun = true;
                case "--help", "-h" -> {
                    usage();
                    System.exit(0);
                }
                default -> {
                    if (!a.startsWith("--")) {
                        out.println("[参数错误] 无法识别：" + a);
                        usage();
                        System.exit(1);
                    }
                    String val = null;
                    int eq = a.indexOf('=');
                    String key = a;
                    if (eq > 0) {
                        key = a.substring(0, eq);
                        val = a.substring(eq + 1);
                    } else if (i + 1 < args.length) {
                        val = args[++i];
                    }
                    switch (key) {
                        case "--url" -> url = val;
                        case "--start-mobile" -> startMobile = val;
                        case "--password" -> password = val;
                        case "--nickname-prefix" -> nicknamePrefix = val;
                        case "--out-dir" -> outDir = Path.of(val);
                        case "--count" -> count = Integer.parseInt(val);
                        case "--concurrency" -> concurrency = Integer.parseInt(val);
                        case "--timeout" -> timeoutSec = Integer.parseInt(val);
                        default -> {
                            out.println("[参数错误] 未知参数：" + key);
                            usage();
                            System.exit(1);
                        }
                    }
                }
            }
        }
    }

    static void usage() {
        out.println("""
                用法：java GenUsers.java [选项]

                  --url               网关地址，默认 http://localhost:8081
                  --start-mobile      起始手机号，默认 13900000001（往后逐个 +1）
                  --count             生成用户数，默认 100
                  --password          明文密码，默认 123456（所有用户一致）
                  --nickname-prefix   昵称前缀，默认 jmeter
                  --concurrency       并发线程数，默认 20
                  --timeout           单次请求超时秒数，默认 10
                  --out-dir           输出目录，默认 seckill-service 模块的 scripts/data（找不到时退回 当前目录/data）
                  --no-login          只注册不登录（不产出 tokens.csv）
                  --fetch-info        额外调 /user/info 拿 userId 写进 users.csv
                  --header            CSV 首行写表头（JMeter 需把 Ignore first line 设为 True）
                  --dry-run           只打印手机号序列，不发起请求

                示例：
                  java GenUsers.java --dry-run --count 10
                  java GenUsers.java --count 1000 --password 123456
                  java GenUsers.java --start-mobile 13800100000 --count 500 --concurrency 50 --fetch-info
                """);
    }
}
