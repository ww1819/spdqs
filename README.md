# SPDQS 内部档案工单系统

Spring Boot + Thymeleaf + MySQL 内部管理系统。

## 功能

- 档案管理：项目档案 CRUD，自动计算维保状态
- 工单管理：工单 CRUD，项目下拉取自档案，维保到期提醒
- 用户登录/注册，初始账号：王威 / wangwei

## 数据库

使用 MySQL 连接 **spdqs**：

| 配置项 | 值 |
|--------|-----|
| URL | `jdbc:mysql://localhost:3306/spdqs` |
| 用户名 | `spdqs` |
| 密码 | 默认 `root`（可通过环境变量或 `application-local.yml` 覆盖） |

首次使用前在 MySQL 中创建库：

```sql
CREATE DATABASE spdqs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'spdqs'@'localhost' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON spdqs.* TO 'spdqs'@'localhost';
FLUSH PRIVILEGES;
```

应用启动时会自动检测并创建全部表（`DatabaseMigrationService`），无需手工执行建表脚本。SQL 文件 [`src/main/resources/sql/table.sql`](src/main/resources/sql/table.sql) 仅供手工参考。

## 启动前准备

1. 配置数据库密码（二选一）：
   - 复制 `application-local.yml.example` 为 `application-local.yml`，填写密码
   - 或设置环境变量：`$env:DB_PASSWORD = "你的密码"`

2. 启动应用（需 Java 17+）：

```powershell
$env:JAVA_HOME = "D:\jdk24"
cd E:\spdqs
mvn spring-boot:run
```

3. 访问 http://localhost:8080 ，使用 **王威 / wangwei** 登录。

## 档案状态规则

| 状态 | 条件 |
|------|------|
| 上线中 | 当前日期 < 上线时间 |
| 维保到期 | 当前日期 > 维保到期时间 |
| 维保到期在三个月内 | 距到期 ≤ 90 天 |
| 维保中 | 其他情况 |
