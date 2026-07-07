---
name: about-page-reuse-index-header
overview: 让 about 页面复用 index 的 Hero 顶部、导航和页脚，统一视觉风格，并增加联系入口。
todos:
  - id: create-fragments
    content: 创建 fragments.html 提取导航栏、Hero、页脚公共片段
    status: completed
  - id: refactor-index
    content: 重构 index.html 应用公共片段并验证首页渲染
    status: completed
    dependencies:
      - create-fragments
  - id: refactor-about
    content: 重构 about.html 复用 Hero 与页脚，并添加联系按钮
    status: completed
    dependencies:
      - create-fragments
  - id: adjust-css
    content: 调整 blog.css 中联系按钮与 about 页 Hero 衔接样式
    status: completed
    dependencies:
      - refactor-about
---

## 产品概述

优化关于（about）页面的视觉一致性与结构复用，使其与首页保持统一的顶部导航、Hero 区域和页脚，并在顶部提供明显的联系入口。

## 核心功能

- about 页面顶部复用 index 同款的 Hero 区域（星空/网格背景、头像、渐变标题、副标题、动效）
- about 页面导航栏补充“联系”链接，与 index 保持一致
- about 页面 Hero 区域增加“联系我”按钮，点击平滑滚动到页脚联系方式
- about 页面页脚与 index 对齐，并添加 `id="footer"` 以支持联系锚点跳转
- 通过 Thymeleaf 片段（fragment）抽离导航栏、Hero、页脚，减少 index 与 about 的重复代码

## 技术栈

- 后端/模板：Spring Boot + Thymeleaf
- 样式：原生 CSS（blog.css）
- 交互：原生 JavaScript（blog.js，已存在平滑滚动、星空、技能条动画等）

## 实现方案

采用 Thymeleaf `th:fragment` 提取公共模板片段，让 index 与 about 共用同一份导航栏、Hero、页脚源码；about 页通过参数化 Hero 片段渲染“关于我”文案并显式展示“联系我”按钮。该方案在实现复用的同时保留了两页内容的差异化（首页展示文章统计，关于页展示联系入口），后续维护只需修改一处即可同步两页。

### 关键决策

- **使用 Thymeleaf fragment 而非直接复制**：真正消除重复代码，符合 Spring Boot + Thymeleaf 项目惯例，避免 index/about 今后因分别修改而再次不一致。
- **Hero 参数化设计**：通过 `active`、`title`、`subtitle`、`showStats`、`showContactBtn` 等参数，让同一个 fragment 同时服务于首页和关于页。
- **不改动 article/detail.html**：本次需求聚焦 about 页，控制爆炸半径；fragment 建立后detail页可后续再统一接入。
- **联系按钮使用现有平滑滚动能力**：按钮指向 `#footer`，复用 blog.js 中已有的 `scrollIntoView({ behavior: 'smooth' })` 逻辑，无需新增脚本。

### 性能与兼容性

- 片段化后最终渲染产物与原来一致，无额外运行时开销。
- 仅新增一个模板文件，CSS 仅增加按钮样式，对构建产物大小影响可忽略。
- 保持响应式布局与现有动画（`fade-up`、技能条、星空）不变。

## 目录结构

```
src/main/resources/templates/
├── fragments.html          # [NEW] 公共 Thymeleaf 片段：siteHead、siteNavbar、heroSection、siteFooter
├── index.html              # [MODIFY] 引入 fragments，替换写死的 navbar/hero/footer
└── about.html              # [MODIFY] 引入 fragments，复用 Hero 与 footer，新增联系按钮
src/main/resources/static/css/
└── blog.css                # [MODIFY] 增加 .hero-contact-btn 等联系按钮样式，微调 about 页承接 Hero 后的间距
```

### 关键片段接口（Thymeleaf）

```html
<!-- 导航栏，active 参数：home | about -->
<div th:fragment="siteNavbar(active)">...</div>

<!-- Hero 区域，可配置标题、副标题、统计、联系按钮 -->
<section th:fragment="heroSection(title, subtitle, showStats, showContactBtn)">...</section>

<!-- 统一页脚，id="footer" 用于锚点跳转 -->
<footer th:fragment="siteFooter">...</footer>
```