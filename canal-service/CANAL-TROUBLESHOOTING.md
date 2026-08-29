# Canal 监听 MySQL 变更 — 问题排查手册

## 一、本次代码层根因（已修复）

| 问题 | 说明 | 修复 |
|---|---|---|
| starter 与 Spring Boot 3 不兼容 | `com.xpand:starter-canal:0.0.1-SNAPSHOT` 是 2018 年的 starter，基于 `javax.*` + `spring.factories` 自动装配。项目用的是 Spring Boot 3.5.16 + JDK 21（jakarta 命名空间，`spring.factories` 装配机制已移除），**Canal 客户端从未被启动** | 替换为官方 `canal.client` / `canal.protocol` 1.1.7，自研客户端 `CanalClientRunner` |
| 过滤器正则转义错误 | `filter: goods\\.goods` 在 YAML 纯标量中就是两个反斜杠，正则含义变成「字面量反斜杠 + 任意字符」，匹配不到 `goods.goods` | 改用单引号 `'goods\..*'`，语义正确 |
| 监听注解失效 | `@CanalEventListener` / `@ListenPoint` 来自废弃 starter | 改为 `MysqlListener.onEvent()` 普通回调，自行按 schema/table 过滤 |

代码修复涉及文件：

- `pom.xml`：依赖替换
- `CanalClientRunner.java`（新增）：连接、订阅、拉取、ack/rollback、断线重连
- `CanalClientProperties.java`（新增）：`canal.client.*` 配置绑定
- `MysqlListener.java`（重写）：普通 `@Component` 回调
- `CanalApp.java`：移除 `@EnableCanalClient`
- `application.yml`：`canal.client` 扁平化配置

## 二、环境层排查清单（代码之外必须逐项核对）

即使代码正确，下面任何一项不满足都收不到数据：

### 1. MySQL 开启 ROW 格式 binlog

```sql
SHOW VARIABLES LIKE 'log_bin';        -- 必须为 ON
SHOW VARIABLES LIKE 'binlog_format';  -- 必须为 ROW
SHOW VARIABLES LIKE 'server_id';      -- 不能为 0
```

Windows 的 `my.ini`（MySQL 8 默认在 `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`）：

```ini
[mysqld]
log-bin=mysql-bin
binlog_format=ROW
server_id=1
```

改完必须**重启 MySQL**。注意：MySQL 8 默认已开启 binlog 且为 ROW，MySQL 5.7 默认关闭。

### 2. canal 账号权限

```sql
CREATE USER canal IDENTIFIED BY 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;
```

### 3. canal 服务端（deployer）实例配置

检查 `canal.deployer/conf/example/instance.properties`：

```properties
# 不能和 MySQL 的 server_id 重复
canal.instance.mysql.slaveId=1234
canal.instance.dbUsername=canal
canal.instance.dbPassword=canal
canal.instance.connectionCharset=UTF-8
# 数据库.表 的正则；只监听 goods 库
canal.instance.filter.regex=goods\\..*
```

关键点：
- `conf/` 下的**目录名**就是 destination（`example` 目录 → destination 为 `example`），客户端 `application.yml` 中的 `destination: example` 必须与之完全一致。
- 过滤规则同时存在于**服务端**（instance.properties）和**客户端**（subscribe-filter），两者是"与"关系——只要有一处把表过滤掉了就收不到。
- MySQL 8 用户注意：canal 服务端版本必须 ≥ 1.1.5 才完整支持 MySQL 8 的认证方式。

### 4. 服务端启动与端口验证

```powershell
# 查看 11111 端口是否监听
Test-NetConnection 127.0.0.1 -Port 11111
```

查看 `logs/example/example.log`，正常应看到类似：

```
start successful....
find a demonstrate point, it will dump from position: mysql-bin.000003:154
```

### 5. 端到端验证

1. 启动 canal deployer → 启动 canal-service；
2. canal-service 日志中应出现 `Canal 客户端已连接 127.0.0.1:11111`；
3. 执行 `UPDATE goods.goods SET ... WHERE id = 1;`；
4. canal-service 控制台应输出 `[UPDATE] goods 表数据变更, id is ...`。

## 三、常见故障对照表

| 现象 | 原因 |
|---|---|
| 客户端日志无任何 canal 相关输出 | 旧 starter 未生效（本次根因）；或新客户端 Bean 未被扫描 |
| 连接报 `connection refused` | canal deployer 未启动或端口不对 |
| 连接成功但收不到数据 | binlog 未开启 / 非 ROW 格式；服务端或客户端过滤规则写错；destination 不匹配 |
| 服务端日志报认证失败 | dbUsername/dbPassword 错误，或 canal 账号缺少 REPLICATION 权限 |
| 服务端日志报 `parse binlog error` | canal 服务端版本过低连 MySQL 8（需 ≥ 1.1.5） |
| 只能收到 INSERT 收不到 UPDATE | 用 `INSERT ... ON DUPLICATE KEY UPDATE` 时事件类型为 QUERY，需单独处理 |
