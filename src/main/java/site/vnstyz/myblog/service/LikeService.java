package site.vnstyz.myblog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.vnstyz.myblog.mapper.ArticleMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * 文章点赞服务：基于 Redis 的「按文章 + 客户端 IP」冷却限流。
 *
 * <p>点赞时以 {@code article:like:lock:{articleId}:{ip}} 为 key 执行
 * {@code setIfAbsent(key, "1", TTL)} 原子操作：
 * <ul>
 *     <li>返回 {@code true}：获取冷却锁成功，执行点赞自增并回读最新计数；</li>
 *     <li>返回 {@code false}：冷却期内已点赞，拒绝重复点赞；</li>
 *     <li>Redis 异常：记录警告并安全拒绝（点赞为写操作，无限流时拒绝更稳妥）。</li>
 * </ul>
 * 冷却时长通过 {@code blog.like.cooldown-minutes} 配置，默认 60 分钟。
 */
@Service
public class LikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeService.class);

    private static final String LIKE_LOCK_KEY_PREFIX = "article:like:lock:";

    private final StringRedisTemplate redisTemplate;
    private final ArticleMapper articleMapper;
    private final long cooldownMinutes;

    @Autowired
    public LikeService(
            StringRedisTemplate redisTemplate,
            ArticleMapper articleMapper,
            @Value("${blog.like.cooldown-minutes:60}") long cooldownMinutes) {
        this.redisTemplate = redisTemplate;
        this.articleMapper = articleMapper;
        this.cooldownMinutes = cooldownMinutes;
    }

    /**
     * 执行点赞（含按文章 + IP 冷却限流）。
     *
     * @param articleId 文章 ID
     * @param ip        客户端 IP（作为限流维度之一）
     * @return 成功返回 {@code Optional.of(最新点赞数)}；冷却中或 Redis 异常返回 {@code Optional.empty()}
     */
    public Optional<Long> like(Long articleId, String ip) {
        if (articleId == null || ip == null || ip.isBlank()) {
            return Optional.empty();
        }

        String lockKey = LIKE_LOCK_KEY_PREFIX + articleId + ":" + ip;

        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofMinutes(cooldownMinutes));
            if (Boolean.FALSE.equals(acquired)) {
                // 冷却期内已点赞，拒绝重复点赞
                return Optional.empty();
            }
        } catch (Exception e) {
            // Redis 不可用：安全拒绝，避免无限流刷量
            log.warn("Redis 点赞冷却锁获取失败，安全拒绝点赞（articleId={}, ip={}）", articleId, ip, e);
            return Optional.empty();
        }

        // 锁获取成功：自增计数并回读最新值
        try {
            articleMapper.updateLikeCount(articleId, 1);
            Long likeCount = articleMapper.findLikeCountById(articleId);
            return Optional.of(likeCount == null ? 0L : likeCount);
        } catch (Exception e) {
            log.error("点赞计数写库失败（articleId={}, ip={}）", articleId, ip, e);
            return Optional.empty();
        }
    }
}
