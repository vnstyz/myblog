package site.vnstyz.myblog.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import site.vnstyz.myblog.entity.Article;
import site.vnstyz.myblog.service.ArticleService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class HomeController {

    @Autowired
    private ArticleService articleService;

    @GetMapping
    public String home(Model model) {
        // 获取已发布的文章列表
        List<Article> articles = articleService.getPublishedArticles();

        // 获取统计数据
        Map<String, Object> stats = articleService.getStats();
        Object articleCount = stats != null ? stats.getOrDefault("articleCount", 0) : 0;
        Object totalViews = stats != null ? stats.getOrDefault("totalViews", 0) : 0;
        Object totalLikes = stats != null ? stats.getOrDefault("totalLikes", 0) : 0;

        // 获取热门文章
        List<Article> hotArticles = articleService.getHotArticles(5);

        model.addAttribute("articles", articles);
        model.addAttribute("articleCount", articleCount);
        model.addAttribute("totalViews", totalViews);
        model.addAttribute("totalLikes", totalLikes);
        model.addAttribute("hotArticles", hotArticles);
        model.addAttribute("title", "首页");

        return "index";
    }

    @GetMapping("/article/{id}")
    public String articleDetail(@PathVariable Long id, Model model) {
        Article article = articleService.getRenderedArticleById(id);
        if (article == null) {
            return "error/404";
        }

        // 增加浏览数
        articleService.incrementViewCount(id);
        article.setViewCount(article.getViewCount() + 1);

        model.addAttribute("article", article);
        model.addAttribute("title", article.getTitle());
        return "article/detail";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "关于我");
        return "about";
    }
}
