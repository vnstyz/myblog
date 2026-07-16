package site.vnstyz.myblog.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.vnstyz.myblog.dto.CommentRequest;
import site.vnstyz.myblog.entity.Article;
import site.vnstyz.myblog.entity.Comment;
import site.vnstyz.myblog.mapper.ArticleMapper;
import site.vnstyz.myblog.service.ArticleService;
import site.vnstyz.myblog.service.CommentService;
import site.vnstyz.myblog.util.IpUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 公开评论接口（匿名用户可访问）。
 *
 * <ul>
 *     <li>{@code GET /api/comments/{articleId}}：拉取某篇文章可见评论列表；</li>
 *     <li>{@code POST /api/comments/{articleId}}：发表评论，由服务端生成「访客」昵称，
 *         并按 IP 做冷却限流（冷却中返回 429）。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/comments")
public class CommentRestController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentService commentService;

    /**
     * 获取文章可见评论列表（按时间正序）。
     */
    @GetMapping("/{articleId}")
    public ResponseEntity<List<Comment>> list(@PathVariable Long articleId) {
        List<Comment> comments = commentService.findVisibleByArticleId(articleId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 发表评论。
     *
     * @return 成功 200 {@code {success:true, comment, commentCount}}；
     *         冷却中 429 {@code {success:false, message}}；
     *         文章不存在/未发布 404 {@code {success:false, message}}。
     */
    @PostMapping("/{articleId}")
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable Long articleId,
            @Valid @RequestBody CommentRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 校验文章存在且已发布
        Article article = articleService.getArticleById(articleId);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            result.put("success", false);
            result.put("message", "文章不存在或未发布");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }

        String ip = IpUtils.getClientIp();
        Optional<Comment> commentOpt = commentService.createComment(articleId, request.getContent(), ip);

        if (commentOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "评论太快了，请稍后再试");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(result);
        }

        Comment comment = commentOpt.get();
        // 回读最新评论数，保证前端统计准确
        Long commentCount = articleMapper.findCommentCountById(articleId);

        result.put("success", true);
        result.put("comment", comment);
        result.put("commentCount", commentCount == null ? 0L : commentCount);
        return ResponseEntity.ok(result);
    }
}
