package site.vnstyz.myblog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import site.vnstyz.myblog.entity.Article;
import site.vnstyz.myblog.entity.User;
import site.vnstyz.myblog.mapper.ArticleMapper;
import site.vnstyz.myblog.mapper.UserMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MarkdownService markdownService;

    public List<Article> getAllArticles() {
        return articleMapper.findAll();
    }

    public Article getArticleById(Long id) {
        return articleMapper.findById(id);
    }

    /**
     * 获取已渲染为 HTML 的文章详情。
     */
    public Article getRenderedArticleById(Long id) {
        Article article = articleMapper.findById(id);
        if (article != null) {
            article.setContentHtml(markdownService.renderToHtml(article.getContent()));
        }
        return article;
    }

    public Article createArticle(Article article) {
        LocalDateTime now = LocalDateTime.now();
        article.setCreatedTime(now);
        article.setUpdateTime(now);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        if (article.getStatus() == null) {
            article.setStatus(0);
        }

        // 首次发布时设置发布时间
        if (Integer.valueOf(1).equals(article.getStatus())) {
            article.setPublishedTime(now);
        }

        // 设置默认分类
        if (article.getCategoryId() == null) {
            article.setCategoryId(1L);
        }

        // 设置当前登录用户为作者
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            article.setCreatedBy(currentUserId);
        }

        articleMapper.insert(article);
        return article;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return null;
        }
        User user = userMapper.findByUsername(username);
        return user != null ? user.getId() : null;
    }

    public Article updateArticle(Long id, Article article) {
        Article existing = articleMapper.findById(id);
        if (existing == null) {
            return null;
        }

        article.setId(id);
        LocalDateTime now = LocalDateTime.now();
        article.setUpdateTime(now);

        // 从草稿变为发布时，若尚未设置发布时间则自动设置
        if (Integer.valueOf(1).equals(article.getStatus())
                && !Integer.valueOf(1).equals(existing.getStatus())
                && existing.getPublishedTime() == null) {
            article.setPublishedTime(now);
        }

        articleMapper.update(article);
        return article;
    }

    public void deleteArticle(Long id) {
        articleMapper.deleteById(id);
    }

    public List<Article> getPublishedArticles() {
        return articleMapper.findByStatus(1);
    }

    public void incrementViewCount(Long id) {
        articleMapper.updateViewCount(id, 1);
    }

    public void incrementLikeCount(Long id) {
        articleMapper.updateLikeCount(id, 1);
    }

    /**
     * 获取首页统计数据
     */
    public Map<String, Object> getStats() {
        return articleMapper.getStats();
    }

    /**
     * 获取热门文章
     */
    public List<Article> getHotArticles(int limit) {
        return articleMapper.findHotArticles(limit);
    }
}
