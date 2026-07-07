package site.vnstyz.myblog.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("开始初始化数据库表结构...");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // 创建表（数据库已通过 createDatabaseIfNotExist=true 自动创建）
            createTables(statement);
            System.out.println("表结构初始化完成");

            // 确保 role 列存在（兼容旧表结构）
            ensureRoleColumn(connection);
            System.out.println("role 列检查完成");

            // 插入初始数据
            insertInitialData(statement);
            System.out.println("初始数据检查完成");
        } catch (Exception e) {
            System.err.println("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
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
    }

    private void ensureRoleColumn(Connection connection) throws Exception {
        var metaData = connection.getMetaData();
        try (var rs = metaData.getColumns(null, null, "user", "role")) {
            if (!rs.next()) {
                try (Statement alterStatement = connection.createStatement()) {
                    alterStatement.execute("""
                        ALTER TABLE user ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'ADMIN' COMMENT '角色'
                    """);
                    System.out.println("已自动添加 user.role 列");
                }
            }
        }
    }

    private void insertInitialData(Statement statement) throws Exception {
        // 检查是否已有数据
        var rs = statement.executeQuery("SELECT COUNT(*) FROM category");
        rs.next();
        if (rs.getInt(1) == 0) {
            // 插入分类数据
            statement.execute("""
                INSERT INTO category (name, description) VALUES
                ('技术分享', '记录技术学习和分享'),
                ('生活随笔', '生活中的点点滴滴'),
                ('学习笔记', '学习过程中的记录');
            """);

            // 插入用户数据（密码：123456）
            statement.execute("""
                INSERT INTO user (username, password, nickname, email, status, role) VALUES
                ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '博主', 'admin@example.com', 1, 'ADMIN');
            """);

            // 插入文章数据
            statement.execute("""
                INSERT INTO article (title, content, summary, status, category_id, created_by, published_time) VALUES
                ('欢迎使用我的博客', '<h2>欢迎来到我的个人博客！</h2><p>这里将记录我的技术学习笔记、生活随笔和一些思考。</p><p>这是一个基于Spring Boot和MyBatis构建的简单博客系统。</p>', '欢迎使用我的博客，一个简单的技术博客系统。', 1, 1, 1, NOW()),
                ('Spring Boot入门', '<h2>Spring Boot简介</h2><p>Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications that you can "just run".</p><p>主要特性：</p><ul><li>独立的Spring应用程序</li><li>内嵌Servlet容器</li><li>自动配置Spring</li><li>提供生产级特性</li></ul>', 'Spring Boot入门指南', 1, 1, 1, NOW());
            """);

            System.out.println("初始数据已插入");
        } else {
            System.out.println("数据库已有数据，跳过插入");
        }
    }
}
