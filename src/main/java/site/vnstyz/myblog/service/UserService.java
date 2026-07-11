package site.vnstyz.myblog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import site.vnstyz.myblog.entity.User;
import site.vnstyz.myblog.exception.PasswordChangeException;
import site.vnstyz.myblog.mapper.UserMapper;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 修改指定用户的登录密码。
     *
     * @param username     当前登录用户名（由 SecurityContext 获取，防止越权）
     * @param oldPassword  当前密码（明文，仅用于校验，不落库）
     * @param newPassword  新密码（明文，将被 BCrypt 哈希后存储）
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new PasswordChangeException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new PasswordChangeException("当前密码错误");
        }
        String hashed = passwordEncoder.encode(newPassword);
        int updated = userMapper.updatePassword(user.getId(), hashed);
        if (updated != 1) {
            throw new PasswordChangeException("密码更新失败，请稍后重试");
        }
    }
}
