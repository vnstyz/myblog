package site.vnstyz.myblog.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 客户端 IP 提取工具。
 *
 * <p>优先解析 {@code X-Forwarded-For} 的首个 IP（适配反向代理场景），
 * 回落到 {@code request.getRemoteAddr()}。逻辑与 {@code LoginAttemptService.getClientKey()} 一致。
 */
public final class IpUtils {

    private IpUtils() {
    }

    /**
     * 从当前请求上下文提取客户端 IP。
     *
     * @return 客户端 IP 字符串；无法获取请求上下文时返回 {@code null}
     */
    public static String getClientIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return null;
        }
        return getClientIp(servletAttrs.getRequest());
    }

    /**
     * 从指定请求提取客户端 IP。
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
