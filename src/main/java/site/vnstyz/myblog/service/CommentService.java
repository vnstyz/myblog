package site.vnstyz.myblog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.vnstyz.myblog.entity.Comment;
import site.vnstyz.myblog.mapper.ArticleMapper;
import site.vnstyz.myblog.mapper.CommentMapper;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 评论服务：匿名评论的昵称生成、按 IP 冷却限流、落库与评论数自增。
 *
 * <p>限流沿用 LikeService 思路，以 {@code article:comment:lock:{ip}} 为 key 执行
 * {@code setIfAbsent(key, "1", TTL)} 原子操作实现「同一 IP 冷却期内仅可评论一次」：
 * <ul>
 *     <li>返回 {@code true}：获取冷却锁成功，生成访客昵称并落库；</li>
 *     <li>返回 {@code false}：冷却期内已评论，拒绝重复评论；</li>
 *     <li>Redis 异常：记录警告并安全拒绝（写操作拒绝比放行更稳妥）。</li>
 * </ul>
 * 冷却时长通过 {@code blog.comment.cooldown-seconds} 配置，默认 60 秒。
 */
@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private static final String COMMENT_LOCK_KEY_PREFIX = "article:comment:lock:";

    /** 访客昵称前缀 */
    private static final String GUEST_PREFIX = "访客";

    /** 随机后缀字符集：剔除易混淆的 0/O/1/l/I/o，避免用户误读 */
    private static final String RANDOM_ALPHABET = "23456789abcdefghjkmnpqrstuvwxyz";

    /** 随机后缀长度 */
    private static final int RANDOM_SUFFIX_LENGTH = 5;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final long cooldownSeconds;

    @Autowired
    public CommentService(
            StringRedisTemplate redisTemplate,
            CommentMapper commentMapper,
            ArticleMapper articleMapper,
            @Value("${blog.comment.cooldown-seconds:60}") long cooldownSeconds) {
        this.redisTemplate = redisTemplate;
        this.commentMapper = commentMapper;
        this.articleMapper = articleMapper;
        this.cooldownSeconds = cooldownSeconds;
    }

    /**
     * 发表评论（含按 IP 冷却限流）。
     *
     * @param articleId 文章 ID
     * @param content   评论内容（已脱敏长度，服务端再次兜底）
     * @param ip        客户端 IP（限流维度）
     * @return 成功返回 {@code Optional.of(保存后的评论)}；冷却中、参数非法或异常返回 {@code Optional.empty()}
     */
    public Optional<Comment> createComment(Long articleId, String content, String ip) {
        if (articleId == null || content == null || content.isBlank()
                || ip == null || ip.isBlank()) {
            return Optional.empty();
        }

        String lockKey = COMMENT_LOCK_KEY_PREFIX + ip;

        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(cooldownSeconds));
            if (Boolean.FALSE.equals(acquired)) {
                // 冷却期内已评论，拒绝重复评论
                return Optional.empty();
            }
        } catch (Exception e) {
            // Redis 不可用：安全拒绝，避免无限流刷量
            log.warn("Redis 评论冷却锁获取失败，安全拒绝评论（articleId={}, ip={}）", articleId, maskIp(ip), e);
            return Optional.empty();
        }

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setAuthorName(generateGuestName());
        comment.setContent(content);
        comment.setIp(ip);
        comment.setStatus(1);
        comment.setCreatedTime(LocalDateTime.now());

        try {
            commentMapper.insert(comment);
        } catch (Exception e) {
            log.error("评论写库失败（articleId={}, ip={}）", articleId, maskIp(ip), e);
            return Optional.empty();
        }

        // 文章评论数自增（与 like_count 模式一致）
        try {
            articleMapper.updateCommentCount(articleId, 1);
        } catch (Exception e) {
            log.error("评论数自增失败（articleId={}）", articleId, e);
        }

        return Optional.of(comment);
    }

    /**
     * 查询某篇文章的可见评论（按时间正序）。
     */
    public List<Comment> findVisibleByArticleId(Long articleId) {
        if (articleId == null) {
            return List.of();
        }
        return commentMapper.findVisibleByArticleId(articleId);
    }

    /**
     * 生成「访客 + 5位随机字符」昵称，例如「访客d4sz3」。
     */
    private String generateGuestName() {
        StringBuilder sb = new StringBuilder(GUEST_PREFIX);
        for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
            sb.append(RANDOM_ALPHABET.charAt(SECURE_RANDOM.nextInt(RANDOM_ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * 对 IP 做脱敏，仅保留用于排查的前缀，避免日志泄露完整地址。
     */
    private String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        int idx = ip.lastIndexOf(ip.contains(":") ? ':' : '.');
        return idx > 0 ? ip.substring(0, idx + 1) + "*" : ip;
    }
}
