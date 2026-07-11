package site.vnstyz.myblog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章创建/更新的入参 DTO。
 * 仅包含允许客户端设置的字段，避免批量赋值（Mass Assignment）漏洞，
 * viewCount / likeCount / createdBy 等由服务端控制。
 */
@Data
public class ArticleRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200")
    private String title;

    @Size(max = 500, message = "摘要长度不能超过 500")
    private String summary;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Min(value = 0, message = "状态值非法")
    @Max(value = 1, message = "状态值非法")
    private Integer status;

    @Positive(message = "分类 ID 非法")
    private Long categoryId;
}
