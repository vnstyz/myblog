---
name: admin-login
overview: 为现有的 Spring Boot + Thymeleaf 博客项目设计并实现管理员登录功能，包括登录页、认证逻辑、登出及受保护的后台入口。
design:
  architecture:
    framework: html
  styleKeywords:
    - Glassmorphism
    - Dark Mode
    - Gradient Accents
    - Micro-interactions
  fontSystem:
    fontFamily: Inter
    heading:
      size: 2rem
      weight: 800
    subheading:
      size: 1.1rem
      weight: 600
    body:
      size: 0.95rem
      weight: 400
  colorSystem:
    primary:
      - "#6366f1"
      - "#818cf8"
      - "#8b5cf6"
    background:
      - "#0a0a0f"
      - "#111118"
      - rgba(25, 25, 40, 0.7)
    text:
      - "#e8e8f0"
      - "#9898b0"
      - "#6b6b80"
    functional:
      - "#22d3ee"
      - "#f87171"
      - "#fbbf24"
todos:
  - id: explore-patterns
    content: 使用 [subagent:code-explorer] 确认现有 controller/service/mapper 与模板约定
    status: completed
  - id: security-deps-user-domain
    content: 在 pom.xml 添加 Spring Security 依赖，创建 User 实体、Mapper 与 XML，并更新数据库初始化脚本增加 role 字段
    status: completed
    dependencies:
      - explore-patterns
  - id: security-config
    content: 实现 SecurityConfig 与 CustomUserDetailsService，配置表单登录与 /admin/** 权限控制
    status: completed
    dependencies:
      - security-deps-user-domain
  - id: login-ui
    content: 创建 login.html 登录页并修改 fragments.html 导航栏显示后台/退出入口
    status: completed
    dependencies:
      - security-config
  - id: admin-dashboard
    content: 创建 AdminController 与 admin/dashboard.html 后台仪表盘
    status: completed
    dependencies:
      - login-ui
---

## 产品概述

为现有的 Spring Boot 个人博客补充管理员登录认证功能。`admin` 用户可通过用户名与密码登录系统，登录成功后进入后台仪表盘；未登录访问后台页面将自动重定向到登录页，现有公开页面保持无需登录。

## 核心功能

- 管理员登录页 `/login`：GET 展示登录表单，POST 由 Spring Security 处理认证
- 基于 `user` 表进行身份校验，使用 BCrypt 校验密码
- 登录成功后跳转 `/admin` 后台仪表盘
- 未登录访问 `/admin/**` 被拦截并重定向 `/login`
- 顶部导航栏根据登录状态显示“后台管理”与“退出”入口
- 保持首页、文章详情、关于等公开页面无访问限制

## 技术栈

- Spring Boot 3.5.15 + Spring Security 6（表单登录）
- MyBatis + MySQL
- Thymeleaf + thymeleaf-extras-springsecurity6
- BCryptPasswordEncoder

## 实现思路

在 `pom.xml` 中引入 Spring Security 与 Thymeleaf Security Extras。新增 `User` 实体、`UserMapper` 及基于数据库查询的 `UserDetailsService` 实现，从 `user` 表加载状态为启用的用户并映射角色。通过 `SecurityFilterChain` 配置自定义登录页 `/login`、保护 `/admin/**`，使用 BCrypt 匹配密码，成功后跳转 `/admin`。新增 `LoginController` 渲染登录页、`AdminController` 展示仪表盘，并更新 `fragments.html` 显示后台入口与退出按钮。`user` 表新增 `role` 字段，默认管理员角色设为 `ADMIN`，便于后续扩展普通用户。

## 架构设计

- **安全层**：`SecurityConfig` 定义过滤链与 `PasswordEncoder`；`CustomUserDetailsService` 加载用户并分配角色。
- **数据层**：`User` 实体 + `UserMapper`（按用户名查询）。
- **Web 层**：`LoginController`（GET `/login`）、`AdminController`（GET `/admin`）。
- **表现层**：`login.html` 登录页、`admin/dashboard.html` 仪表盘，复用现有 `fragments.html` 导航。

## 关键代码结构

```java
// entity/User.java
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String avatar;
    private Integer status;
    private String role;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
```

```java
// config/SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().permitAll())
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/admin", true)
            .permitAll())
        .logout(logout -> logout
            .logoutSuccessUrl("/")
            .permitAll());
    return http.build();
}
```

## 目录结构

```
e:/Temp/myblog/
├── pom.xml                                          # [MODIFY] 添加 spring-boot-starter-security、thymeleaf-extras-springsecurity6
├── src/main/java/site/vnstyz/myblog/
│   ├── config/
│   │   ├── SecurityConfig.java                      # [NEW] 安全过滤链、密码编码器
│   │   └── CustomUserDetailsService.java            # [NEW] 从数据库加载用户详情
│   ├── controller/
│   │   ├── LoginController.java                     # [NEW] 渲染登录页
│   │   └── AdminController.java                     # [NEW] 后台仪表盘控制器
│   ├── entity/User.java                             # [NEW] 用户实体
│   ├── mapper/UserMapper.java                       # [NEW] 用户 Mapper 接口
│   └── service/AdminService.java                    # [NEW] 可选：仪表盘数据聚合
├── src/main/resources/
│   ├── mapper/UserMapper.xml                        # [NEW] 按用户名查询用户
│   ├── templates/login.html                         # [NEW] 登录页
│   ├── templates/admin/dashboard.html               # [NEW] 后台仪表盘
│   ├── templates/fragments.html                     # [MODIFY] 导航栏增加后台/登录/退出入口
│   ├── db/migration/001_initial_schema.sql          # [MODIFY] user 表增加 role 字段并设置 admin 为 ADMIN
│   └── application.yml                              # [MODIFY] 可选：Spring Security 调试日志
└── src/main/java/site/vnstyz/myblog/util/DatabaseInitializer.java  # [MODIFY] user 表增加 role 字段并设置 admin 为 ADMIN
```

## 设计风格

登录页与后台仪表盘沿用博客现有的深色玻璃拟态（Glassmorphism）风格。登录页使用全屏星空粒子与网格背景，中央悬浮一个磨砂玻璃质感的登录卡片，顶部为渐变品牌标识，下方为带聚焦辉光的输入框与渐变主按钮。后台仪表盘采用两栏布局：顶部统计卡片 + 最近文章列表，所有卡片使用统一的圆角、边框与悬停抬升效果，保持与前台页面一致的视觉语言。交互上包含输入框聚焦发光、按钮悬停阴影加深、登录按钮加载状态等微动效。

## Agent Extensions

### SubAgent

- **code-explorer**
- Purpose: 在实现用户/管理员模块前，确认现有 controller、service、mapper 与 Thymeleaf 模板片段的命名与使用约定。
- Expected outcome: 明确代码风格与集成点，确保新增模块与现有架构保持一致。