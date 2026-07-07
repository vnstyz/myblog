package site.vnstyz.myblog.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String avatar;
    private Integer status;
    private String role;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
