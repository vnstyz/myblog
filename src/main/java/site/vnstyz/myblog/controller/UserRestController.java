package site.vnstyz.myblog.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.vnstyz.myblog.dto.ChangePasswordRequest;
import site.vnstyz.myblog.exception.PasswordChangeException;
import site.vnstyz.myblog.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/user")
public class UserRestController {

    @Autowired
    private UserService userService;

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordChangeException("两次输入的新密码不一致");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.changePassword(username, request.getOldPassword(), request.getNewPassword());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "密码修改成功");
        return ResponseEntity.ok(result);
    }
}
