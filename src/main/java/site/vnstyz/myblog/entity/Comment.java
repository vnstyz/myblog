package site.vnstyz.myblog.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long articleId;
    private String authorName;
    private String content;
    // IP 仅服务端使用（限流/审计），序列化时忽略不暴露给前端
    @JsonIgnore
    private String ip;
    private Integer status; // 0: 隐藏, 1: 可见
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdTime;
}
