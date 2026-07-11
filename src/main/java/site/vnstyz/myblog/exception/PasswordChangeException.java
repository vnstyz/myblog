package site.vnstyz.myblog.exception;

/**
 * 修改密码相关的业务异常（如原密码错误、用户不存在等）。
 * 由 GlobalExceptionHandler 统一转换为 400 友好提示。
 */
public class PasswordChangeException extends RuntimeException {

    public PasswordChangeException(String message) {
        super(message);
    }
}
