package site.vnstyz.myblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 匿名评论入参 DTO。
 * 仅包含评论内容，用户名由服务端自动生成（访客+随机字符），避免客户端伪造。
 */
@Data
public class CommentRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论长度不能超过 500 字符")
    private String content;
}
