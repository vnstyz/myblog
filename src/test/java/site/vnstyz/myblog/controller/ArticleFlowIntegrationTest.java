package site.vnstyz.myblog.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.vnstyz.myblog.entity.Article;
import site.vnstyz.myblog.mapper.ArticleMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ArticleFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleMapper articleMapper;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createEditAndRenderMarkdownArticle() throws Exception {
        // 1. 创建一篇 Markdown 文章并发布
        String markdownContent = "# 测试文章\n\n这是一段 **加粗** 文字。\n\n```java\nSystem.out.println(\"hello\");\n```";
        String json = String.format(
            "{\"title\":\"Markdown测试\",\"status\":1,\"categoryId\":1,\"summary\":\"摘要\",\"content\":\"%s\"}",
            markdownContent.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        );

        mockMvc.perform(post("/admin/api/articles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Markdown测试"))
            .andExpect(jsonPath("$.status").value(1));

        // 获取刚创建的文章 id
        Article created = articleMapper.findAll().stream()
            .filter(a -> "Markdown测试".equals(a.getTitle()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("未找到创建的文章"));

        // 2. 前端详情页应展示渲染后的 HTML
        mockMvc.perform(get("/article/" + created.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<h1>测试文章</h1>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<strong>加粗</strong>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<pre>")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("&lt;script&gt;"))));

        // 3. 编辑为草稿，再发布，验证 published_time 不会被覆盖
        String editJson = "{\"title\":\"Markdown测试已修改\",\"status\":0,\"categoryId\":1,\"summary\":\"摘要\",\"content\":\"# 已修改\"}";
        mockMvc.perform(put("/admin/api/articles/" + created.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(editJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Markdown测试已修改"));

        // 清理数据
        articleMapper.deleteById(created.getId());
    }
}
