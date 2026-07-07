---
name: fix-new-article-save-failure
overview: "修复 MyBlog 后台“新建文章”无法保存的问题，根因是 ArticleMapper 中 insert/update 参数使用了 @Param(\"article\")，但 XML 中却用未限定属性名（如 #{title}）导致 MyBatis 绑定异常。方案将移除这两个方法上的 @Param 注解，并验证 CSRF 与测试配置。"
todos:
  - id: fix-article-mapper
    content: 修复 ArticleMapper 中 insert/update 的 @Param 参数绑定
    status: completed
  - id: fix-test-csrf
    content: 为 ArticleFlowIntegrationTest 的写操作请求添加 csrf() 验证
    status: completed
    dependencies:
      - fix-article-mapper
  - id: verify-save-flow
    content: 运行集成测试并验证后台新建/编辑文章可正常保存
    status: completed
    dependencies:
      - fix-test-csrf
---

## 用户问题

后台管理页面点击“新建文章”并保存后，文章无法写入数据库，保存失败。

## 产品概述

基于 Spring Boot + MyBatis + Thymeleaf 的个人博客，已接入后台 Markdown 编辑器与前端 Markdown 渲染。当前新建与编辑文章时服务端因参数绑定异常导致数据无法持久化，需要修复保存链路。

## 核心功能

- 修复 MyBatis 参数绑定错误，使 `createArticle` 与 `updateArticle` 能正常写入数据库。
- 修正集成测试中的 CSRF 验证，确保测试能真实覆盖保存流程。
- 不改动现有 UI、数据库表结构及业务逻辑。

## 技术栈

- 后端：Spring Boot 3.5.15 + MyBatis + Spring Security 6 + Thymeleaf
- 构建工具：Maven
- 数据库：MySQL

## 问题定位

1. **MyBatis 参数绑定异常**：`ArticleMapper` 接口中 `insert` 与 `update` 方法使用了 `@Param("article") Article article`，但 `ArticleMapper.xml` 中直接使用 `#{title}`、`#{content}` 等属性名。MyBatis 对单参数对象添加 `@Param` 后会将其包装为 `ParamMap`，导致无法解析 `#{title}`，抛出 `BindingException`，新建和编辑保存均失败。
2. **集成测试 CSRF 缺失**：`ArticleFlowIntegrationTest` 对 `POST/PUT` 请求未加 `.with(csrf())`，在 Spring Security 默认启用 CSRF 的情况下会返回 403，无法有效验证保存逻辑。

## 实现方案

- 移除 `ArticleMapper` 中 `insert` 与 `update` 方法的 `@Param("article")` 注解，使其回归单参数对象映射方式；`ArticleMapper.xml` 无需修改。
- 在 `ArticleFlowIntegrationTest` 中为 `POST/PUT` 请求添加 `csrf()` 处理器，恢复测试对保存链路的有效验证。
- 保持 `ArticleService`、`ArticleRestController`、前端 `admin.js` 及模板不变，控制改动范围。

## 关键代码结构

```java
// site.vnstyz.myblog.mapper.ArticleMapper
// 修改后：去掉 @Param("article")，与 XML 中的 #{property} 直接映射
int insert(Article article);
int update(Article article);
```

## 目录结构

```
e:/Temp/myblog/
└── src/
    ├── main/java/site/vnstyz/myblog/mapper/ArticleMapper.java  # [MODIFY] 移除 insert/update 的 @Param 注解
    └── test/java/site/vnstyz/myblog/controller/ArticleFlowIntegrationTest.java  # [MODIFY] 为写操作请求补充 csrf()
```

## 实现注意事项

- **影响范围控制**：仅修复 Mapper 接口参数绑定与测试写法，不改动前端、数据库、服务层逻辑。
- **向后兼容**：移除 `@Param` 后 `parameterType="Article"` 的 XML 映射保持不变，对其它查询/删除方法无影响。
- **验证方式**：运行 `ArticleFlowIntegrationTest` 应全部通过；同时手动在后台新建/编辑一篇文章，确认能成功保存并在首页/详情页正确展示。