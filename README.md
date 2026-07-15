# myblog

基于 Spring Boot 3.5 + MyBatis + Thymeleaf 的个人博客系统。

## 技术栈

- Spring Boot 3.5 / Java 21
- MyBatis + Druid + MySQL 8
- Redis（文章实时浏览量计数）
- Thymeleaf + Spring Security

## Docker 部署

项目已提供多阶段 `Dockerfile` 与 `docker-compose.yml`，可一键拉起 `app + mysql + redis` 三个容器，数据通过命名卷持久化。

### 构建并启动

```bash
# 构建镜像并后台启动
docker compose up --build -d

# 查看应用日志（首次启动会看到自动生成的管理员密码，若未设置 ADMIN_PASSWORD）
docker compose logs -f app
```

启动后访问 http://localhost:8080 。

### 停止与清理

```bash
docker compose down            # 停止并移除容器（数据卷保留）
docker compose down -v         # 同时删除数据卷（会清空 MySQL / Redis 数据）
```

### 自定义配置（可选）

在当前目录创建 `.env` 文件覆盖默认值，例如：

```env
MYSQL_ROOT_PASSWORD=yourStrongPassword
ADMIN_USERNAME=admin
ADMIN_PASSWORD=yourAdminPassword
LOG_LEVEL=info
```

### 环境变量清单

| 变量 | 作用 | 默认值 |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码，app 的 `DB_PASSWORD` 与之保持一致 | `root` |
| `ADMIN_USERNAME` | 初始管理员用户名（首次启动由 `DatabaseInitializer` 创建） | `admin` |
| `ADMIN_PASSWORD` | 初始管理员密码；留空则由应用自动生成随机强密码并打印日志 | 空（自动生成） |
| `LOG_LEVEL` | 应用日志级别 | `info` |

应用连接串说明（已在 `docker-compose.yml` 中注入，无需改动源码）：
- `SPRING_DATASOURCE_URL` 指向 `mysql:3306/myblog`，覆盖 `application.yml` 中硬编码的 `localhost`
- `REDIS_HOST=redis`，由 `application.yml` 的 `REDIS_HOST` 环境变量支持

### 数据初始化

`src/main/resources/db/migration/001_initial_schema.sql` 会在 MySQL 首次启动时自动执行，完成建库、建表并插入分类与示例文章。默认管理员账号由应用在启动时创建。

### 持久化

- MySQL 数据：`mysql_data` 命名卷（`/var/lib/mysql`）
- Redis 数据：`redis_data` 命名卷（`/data`，开启 AOF）

### 本地（非 Docker）运行

需本机安装 MySQL 8 与 Redis，并在 `application.yml` 已支持的 `DB_USERNAME` / `DB_PASSWORD` / `REDIS_HOST` 等环境变量中配置连接信息，然后：

```bash
mvn clean package -DskipTests
java -jar target/myblog-0.0.1-SNAPSHOT.jar
```
