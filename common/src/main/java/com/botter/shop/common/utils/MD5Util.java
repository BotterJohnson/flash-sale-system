package com.botter.shop.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-31 13:09
 * @Description 密码两次 MD5 加盐工具类
 * 加密链路：
 * 1. 前端：inputPass --(md5 + 固定盐)--> formPass，传输的是 formPass，避免明文密码走网络
 * 2. 后端：formPass --(md5 + 数据库随机盐)--> dbPass，入库存储的是 dbPass
 * 登录校验时后端只需拿库里的盐对 formPass 再做一次同样加密，与库中密文比对即可
 */
public class MD5Util {

    /**
     * 前端固定盐，必须与 web-app 中 common.js 的 g_passsword_salt 保持一致
     */
    private static final String FORM_SALT = "1a2b3c4d";
    

    /**
     * 基础 md5，输出 32 位小写十六进制字符串
     */
    public static String md5(String src) {
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
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * 明文密码 -> 前端传输密码（一次 md5 + 固定盐）
     * 加盐规则与前端一致：salt[0] + salt[2] + 密码 + salt[5] + salt[4]
     */
    public static String inputPassToFormPass(String inputPass) {
        String str = "" + FORM_SALT.charAt(0) + FORM_SALT.charAt(2)
                + inputPass + FORM_SALT.charAt(5) + FORM_SALT.charAt(4);
        return md5(str);
    }

    /**
     * 前端传输密码 -> 数据库存储密码（二次 md5 + 数据库随机盐）
     */
    public static String formPassToDBPass(String formPass, String saltDB) {
        String str = "" + saltDB.charAt(0) + saltDB.charAt(2)
                + formPass + saltDB.charAt(5) + saltDB.charAt(4);
        return md5(str);
    }

    /**
     * 明文密码 -> 数据库存储密码，测试工具方法
     */
    public static String inputPassToDBPass(String inputPass, String saltDB) {
        return formPassToDBPass(inputPassToFormPass(inputPass), saltDB);
    }

    /**
     * 生成 8 位随机盐（长度必须 >= 6，因为加密时取到了下标 5）
     */
    public static String randomSalt() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
