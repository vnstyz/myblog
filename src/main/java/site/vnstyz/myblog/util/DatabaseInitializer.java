package site.vnstyz.myblog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    /**
     * 初始管理员密码。生产环境请通过环境变量 ADMIN_PASSWORD 注入。
     * 若未提供，则在首次创建账号时生成随机强密码并打印到日志（仅此一次）。
     */
    @Value("${app.admin.password:}")
    private String adminPassword;

    public DatabaseInitializer(DataSource dataSource, PasswordEncoder passwordEncoder) {
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化数据库表结构...");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // 创建表（数据库已通过 createDatabaseIfNotExist=true 自动创建）
            createTables(statement);
            log.info("表结构初始化完成");

            // 确保 role 列存在（兼容旧表结构）
            ensureRoleColumn(connection);
            log.info("role 列检查完成");

            // 插入初始数据
            insertInitialData(connection, statement);
            log.info("初始数据检查完成");
        } catch (Exception e) {
            // 避免向控制台打印完整堆栈，防止敏感信息泄露
            log.error("数据库初始化失败: {}", e.getMessage());
            log.debug("数据库初始化失败详情", e);
        }
    }

    private void createTables(Statement statement) throws Exception {
        // 创建用户表
        String createUserTable = """
            CREATE TABLE IF NOT EXISTS user (
                id BIGINT NOT NULL AUTO_INCREMENT,
                username VARCHAR(50) NOT NULL,
                password VARCHAR(255) NOT NULL,
                nickname VARCHAR(50) DEFAULT NULL,
                email VARCHAR(100) DEFAULT NULL,
                avatar VARCHAR(255) DEFAULT NULL,
                status TINYINT DEFAULT 1,
                role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
                created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                UNIQUE KEY uk_username (username),
                UNIQUE KEY uk_email (email)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;
        statement.execute(createUserTable);

        // 创建分类表
        String createCategoryTable = """
            CREATE TABLE IF NOT EXISTS category (
                id BIGINT NOT NULL AUTO_INCREMENT,
                name VARCHAR(50) NOT NULL,
                description VARCHAR(200) DEFAULT NULL,
                created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                UNIQUE KEY uk_name (name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;
        statement.execute(createCategoryTable);

        // 创建文章表
        String createArticleTable = """
            CREATE TABLE IF NOT EXISTS article (
                id BIGINT NOT NULL AUTO_INCREMENT,
                title VARCHAR(200) NOT NULL,
                content LONGTEXT,
                summary VARCHAR(500) DEFAULT NULL,
                view_count BIGINT DEFAULT 0,
                like_count BIGINT DEFAULT 0,
                comment_count BIGINT DEFAULT 0,
                status TINYINT DEFAULT 0,
                category_id BIGINT DEFAULT NULL,
                created_by BIGINT NOT NULL,
                created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                published_time DATETIME DEFAULT NULL,
                PRIMARY KEY (id),
                KEY idx_category_id (category_id),
                KEY idx_created_by (created_by),
                KEY idx_created_time (created_time),
                KEY idx_status (status),
                FOREIGN KEY (category_id) REFERENCES category(id),
                FOREIGN KEY (created_by) REFERENCES user(id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;
        statement.execute(createArticleTable);

        // 创建评论表
        String createCommentTable = """
            CREATE TABLE IF NOT EXISTS comment (
                id BIGINT NOT NULL AUTO_INCREMENT,
                article_id BIGINT NOT NULL,
                author_name VARCHAR(30) NOT NULL,
                content VARCHAR(500) NOT NULL,
                ip VARCHAR(45) DEFAULT NULL,
                status TINYINT DEFAULT 1,
                created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                KEY idx_article_id (article_id),
                KEY idx_created_time (created_time),
                FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;
        statement.execute(createCommentTable);
    }

    private void ensureRoleColumn(Connection connection) throws Exception {
        var metaData = connection.getMetaData();
        try (var rs = metaData.getColumns(null, null, "user", "role")) {
            if (!rs.next()) {
                try (Statement alterStatement = connection.createStatement()) {
                    alterStatement.execute("""
                        ALTER TABLE user ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'ADMIN' COMMENT '角色'
                    """);
                    log.info("已自动添加 user.role 列");
                }
            }
        }
    }

    private void insertInitialData(Connection connection, Statement statement) throws Exception {
        // 检查是否已有数据
        try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM category")) {
            rs.next();
            if (rs.getInt(1) != 0) {
                log.info("数据库已有数据，跳过插入");
                return;
            }
        }

        // 插入分类数据
        statement.execute("""
            INSERT INTO category (name, description) VALUES
            ('技术分享', '记录技术学习和分享'),
            ('生活随笔', '生活中的点点滴滴'),
            ('学习笔记', '学习过程中的记录');
        """);

        // 生成初始管理员账号：优先使用环境变量密码，否则生成随机强密码
        boolean generated = false;
        String rawPassword = adminPassword;
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = generateRandomPassword();
            generated = true;
        }
        String hashed = passwordEncoder.encode(rawPassword);

        // 使用 PreparedStatement 参数化插入，避免 SQL 注入
        String insertUser = "INSERT INTO user (username, password, nickname, email, status, role) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertUser)) {
            ps.setString(1, adminUsername);
            ps.setString(2, hashed);
            ps.setString(3, "博主");
            ps.setString(4, "admin@example.com");
            ps.setInt(5, 1);
            ps.setString(6, "ADMIN");
            ps.executeUpdate();
        }

        if (generated) {
            log.warn("========================================================");
            log.warn("未检测到 ADMIN_PASSWORD 环境变量，已生成随机管理员密码：");
            log.warn("  用户名: {}", adminUsername);
            log.warn("  密码  : {}", rawPassword);
            log.warn("请立即登录并修改密码，此密码不会再次显示。");
            log.warn("========================================================");
        } else {
            log.info("已根据环境变量创建初始管理员账号: {}", adminUsername);
        }

        // 插入文章数据（内容以 Markdown 存储，展示时经过 sanitize）
        statement.execute("""
            INSERT INTO article (title, content, summary, status, category_id, created_by, published_time) VALUES
            ('欢迎使用我的博客', '## 欢迎来到我的个人博客！\n\n这里将记录我的技术学习笔记、生活随笔和一些思考。\n\n这是一个基于 Spring Boot 和 MyBatis 构建的简单博客系统。', '欢迎使用我的博客，一个简单的技术博客系统。', 1, 1, 1, NOW()),
            ('Spring Boot入门', '## Spring Boot简介\n\nSpring Boot makes it easy to create stand-alone, production-grade Spring based Applications that you can "just run".\n\n主要特性：\n\n- 独立的Spring应用程序\n- 内嵌Servlet容器\n- 自动配置Spring\n- 提供生产级特性', 'Spring Boot入门指南', 1, 1, 1, NOW());
        """);

        log.info("初始数据已插入");
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
