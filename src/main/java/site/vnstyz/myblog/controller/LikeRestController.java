package site.vnstyz.myblog.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.vnstyz.myblog.entity.Article;
import site.vnstyz.myblog.service.ArticleService;
import site.vnstyz.myblog.service.LikeService;
import site.vnstyz.myblog.util.IpUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 公开点赞接口（匿名用户可访问）。
 *
 * <p>基于「文章 ID + 客户端 IP」维度做冷却限流，防止同一 IP 短时间重复刷赞。
 */
@RestController
@RequestMapping("/api/like")
public class LikeRestController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private LikeService likeService;

    /**
     * 为指定文章点赞。
     *
     * @return 成功 200 {@code {success:true, likeCount:N}}；
     *         冷却中 429 {@code {success:false, message}}；
     *         文章不存在/未发布 404 {@code {success:false, message}}。
     */
    @PostMapping("/{articleId}")
    public ResponseEntity<Map<String, Object>> like(@PathVariable Long articleId) {
        Map<String, Object> result = new HashMap<>();

        // 校验文章存在且已发布
        Article article = articleService.getArticleById(articleId);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            result.put("success", false);
            result.put("message", "文章不存在或未发布");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }

        String ip = IpUtils.getClientIp();
        Optional<Long> likeCount = likeService.like(articleId, ip);

        if (likeCount.isEmpty()) {
            result.put("success", false);
            result.put("message", "您已点赞过，请稍后再试");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(result);
        }

        result.put("success", true);
        result.put("likeCount", likeCount.get());
        return ResponseEntity.ok(result);
    }
}
