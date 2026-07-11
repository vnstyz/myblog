package site.vnstyz.myblog.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import site.vnstyz.myblog.entity.Article;
import site.vnstyz.myblog.service.ArticleService;
import site.vnstyz.myblog.service.TrafficService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private TrafficService trafficService;

    @GetMapping
    public String dashboard(Model model) {
        Map<String, Object> stats = articleService.getStats();
        List<Article> recentArticles = articleService.getAllArticles();

        model.addAttribute("articleCount", stats != null ? stats.getOrDefault("articleCount", 0) : 0);
        model.addAttribute("totalViews", trafficService.getSiteTotalViews());
        model.addAttribute("totalLikes", stats != null ? stats.getOrDefault("totalLikes", 0) : 0);
        model.addAttribute("recentArticles", recentArticles);
        model.addAttribute("title", "后台管理");

        return "admin/dashboard";
    }
}
