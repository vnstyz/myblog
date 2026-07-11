package site.vnstyz.myblog.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于内存的登录失败限制服务，用于缓解暴力破解。
 * 生产环境建议替换为基于 Redis 的分布式实现。
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_MS = 15 * 60 * 1000L; // 15 分钟

    private static final class Attempt {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long lockUntil = 0L;
    }

    private final ConcurrentHashMap<String, Attempt> cache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key != null) {
            cache.remove(key);
        }
    }

    public void loginFailed(String key) {
        if (key == null) {
            return;
        }
        Attempt attempt = cache.computeIfAbsent(key, k -> new Attempt());
        int current = attempt.count.incrementAndGet();
        if (current >= MAX_ATTEMPTS) {
            attempt.lockUntil = System.currentTimeMillis() + LOCK_TIME_MS;
        }
    }

    public boolean isBlocked(String key) {
        if (key == null) {
            return false;
        }
        Attempt attempt = cache.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.lockUntil > System.currentTimeMillis()) {
            return true;
        }
        // 锁定过期，清理
        if (attempt.lockUntil != 0L) {
            cache.remove(key);
        }
        return false;
    }

    /**
     * 获取当前请求的客户端 IP，作为限流键。
     */
    public String getClientKey() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return null;
        }
        HttpServletRequest request = servletAttrs.getRequest();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
