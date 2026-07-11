package site.vnstyz.myblog.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import site.vnstyz.myblog.mapper.ArticleMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 Redis 的浏览量计数器服务。
 *
 * <p>采用「Redis 存绝对值 + MySQL 定时批量落库」模式：
 * <ul>
 *     <li>{@code article:views:{id}} 记录每篇文章累计浏览量；</li>
 *     <li>{@code site:views:total} 记录全站累计浏览量；</li>
 *     <li>{@code site:article:ids} 用 Set 记录需落库的文章 id，避免在生产用 {@code KEYS} 阻塞 Redis。</li>
 * </ul>
 * 访问时原子自增；每 5 分钟及应用停机时把 Redis 绝对值批量写回 MySQL，保证计数连续且高性能。
 */
@Service
@EnableScheduling
public class TrafficService {

    private static final Logger log = LoggerFactory.getLogger(TrafficService.class);

    private static final String ARTICLE_VIEW_KEY = "article:views:";
    private static final String SITE_TOTAL_KEY = "site:views:total";
    private static final String SITE_ARTICLE_IDS = "site:article:ids";

    private final StringRedisTemplate redisTemplate;
    private final ArticleMapper articleMapper;

    @Autowired
    public TrafficService(StringRedisTemplate redisTemplate, ArticleMapper articleMapper) {
        this.redisTemplate = redisTemplate;
        this.articleMapper = articleMapper;
    }

    /**
     * 应用启动时从 MySQL 载入各文章浏览量基准到 Redis，保证计数器连续。
     */
    @PostConstruct
    public void warmUp() {
        try {
            List<Map<String, Object>> rows = articleMapper.selectIdAndViews();
            long siteTotal = 0L;
            for (Map<String, Object> row : rows) {
                Long id = ((Number) row.get("id")).longValue();
                long views = row.get("view_count") == null ? 0L : ((Number) row.get("view_count")).longValue();
                redisTemplate.opsForValue().set(ARTICLE_VIEW_KEY + id, Long.toString(views));
                redisTemplate.opsForSet().add(SITE_ARTICLE_IDS, id.toString());
                siteTotal += views;
            }
            redisTemplate.opsForValue().set(SITE_TOTAL_KEY, Long.toString(siteTotal));
            log.info("Redis 浏览量计数器预热完成，共 {} 篇文章，全站总量 {}", rows.size(), siteTotal);
        } catch (Exception e) {
            // Redis 不可用时降级为直读数据库，不阻断启动主流程
            log.error("Redis 浏览量计数器预热失败，将降级为直读数据库", e);
        }
    }

    /**
     * 记录一次文章浏览（调用方负责会话去重）。原子自增文章与全站计数，并登记文章 id。
     */
    public void recordView(Long articleId) {
        try {
            redisTemplate.opsForValue().increment(ARTICLE_VIEW_KEY + articleId);
            redisTemplate.opsForValue().increment(SITE_TOTAL_KEY);
            redisTemplate.opsForSet().add(SITE_ARTICLE_IDS, articleId.toString());
        } catch (Exception e) {
            log.warn("Redis 记录浏览量失败（articleId={}）", articleId, e);
        }
    }

    /**
     * 读取文章实时浏览量；键缺失时回落读数据库并回填。
     */
    public long getArticleViews(Long articleId) {
        try {
            String v = redisTemplate.opsForValue().get(ARTICLE_VIEW_KEY + articleId);
            if (v == null) {
                Long base = articleMapper.findViewCountById(articleId);
                long views = base == null ? 0L : base;
                redisTemplate.opsForValue().set(ARTICLE_VIEW_KEY + articleId, Long.toString(views));
                redisTemplate.opsForSet().add(SITE_ARTICLE_IDS, articleId.toString());
                return views;
            }
            return Long.parseLong(v);
        } catch (Exception e) {
            log.warn("Redis 读取文章浏览量失败，回落数据库（articleId={}）", articleId, e);
            Long base = articleMapper.findViewCountById(articleId);
            return base == null ? 0L : base;
        }
    }

    /**
     * 读取全站实时总浏览量；键缺失时回落数据库汇总。
     */
    public long getSiteTotalViews() {
        try {
            String v = redisTemplate.opsForValue().get(SITE_TOTAL_KEY);
            if (v == null) {
                return getSiteTotalFromDb();
            }
            return Long.parseLong(v);
        } catch (Exception e) {
            log.warn("Redis 读取全站浏览量失败，回落数据库", e);
            return getSiteTotalFromDb();
        }
    }

    private long getSiteTotalFromDb() {
        Map<String, Object> stats = articleMapper.getStats();
        Object total = stats == null ? null : stats.get("totalViews");
        return total == null ? 0L : ((Number) total).longValue();
    }

    /**
     * 将 Redis 中的累计浏览量批量写回 MySQL（绝对值），并刷新全站总量。
     * 由定时任务（每 5 分钟）与应用停机兜底各调用一次。
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void flush() {
        try {
            Set<String> ids = redisTemplate.opsForSet().members(SITE_ARTICLE_IDS);
            if (ids == null || ids.isEmpty()) {
                return;
            }
            long siteTotal = 0L;
            for (String idStr : ids) {
                String v = redisTemplate.opsForValue().get(ARTICLE_VIEW_KEY + idStr);
                if (v == null) {
                    continue;
                }
                long views = Long.parseLong(v);
                articleMapper.setViewCount(Long.parseLong(idStr), views);
                siteTotal += views;
            }
            redisTemplate.opsForValue().set(SITE_TOTAL_KEY, Long.toString(siteTotal));
            log.info("Redis 浏览量已落库，涉及 {} 篇文章，全站总量 {}", ids.size(), siteTotal);
        } catch (Exception e) {
            log.error("Redis 浏览量落库失败", e);
        }
    }

    /**
     * 应用停机兜底落库，避免计数丢失。
     */
    @PreDestroy
    public void flushOnShutdown() {
        log.info("应用停机，执行浏览量兜底落库");
        flush();
    }
}
