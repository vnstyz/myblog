package site.vnstyz.myblog.config;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * 监听认证成功/失败事件，配合 {@link LoginAttemptService} 实现登录失败限制。
 */
@Component
public class AuthenticationAttemptListener {

    private final LoginAttemptService loginAttemptService;

    public AuthenticationAttemptListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        loginAttemptService.loginFailed(loginAttemptService.getClientKey());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttemptService.loginSucceeded(loginAttemptService.getClientKey());
    }
}
