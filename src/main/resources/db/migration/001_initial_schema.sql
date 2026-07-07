-- 创建数据库
CREATE DATABASE IF NOT EXISTS myblog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE myblog;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `role` VARCHAR(50) NOT NULL DEFAULT 'ADMIN' COMMENT '角色: ADMIN-管理员, USER-普通用户',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 文章表
CREATE TABLE IF NOT EXISTS `article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` LONGTEXT COMMENT '内容',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
    `view_count` BIGINT DEFAULT 0 COMMENT '浏览数',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `comment_count` BIGINT DEFAULT 0 COMMENT '评论数',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-草稿, 1-发布',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `created_by` BIGINT NOT NULL COMMENT '创建者ID',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `published_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_created_time` (`created_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- 插入测试数据
INSERT INTO `category` (`name`, `description`) VALUES
('技术分享', '记录技术学习和分享'),
('生活随笔', '生活中的点点滴滴'),
('学习笔记', '学习过程中的记录');

-- 插入默认用户（密码：123456，实际使用时请修改）
INSERT INTO `user` (`username`, `password`, `nickname`, `email`, `status`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVYITy', '博主', 'admin@example.com', 1, 'ADMIN');

-- 插入测试文章
INSERT INTO `article` (`title`, `content`, `summary`, `status`, `category_id`, `created_by`, `published_time`) VALUES
('欢迎使用我的博客', '<h2>欢迎来到我的个人博客！</h2><p>这里将记录我的技术学习笔记、生活随笔和一些思考。</p><p>这是一个基于Spring Boot和MyBatis构建的简单博客系统。</p>', '欢迎使用我的博客，一个简单的技术博客系统。', 1, 1, 1, CURRENT_TIMESTAMP),
('Spring Boot入门', '<h2>Spring Boot简介</h2><p>Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications that you can "just run".</p><p>主要特性：</p><ul><li>独立的Spring应用程序</li><li>内嵌Servlet容器</li><li>自动配置Spring</li><li>提供生产级特性</li></ul>', 'Spring Boot入门指南', 1, 1, 1, CURRENT_TIMESTAMP);