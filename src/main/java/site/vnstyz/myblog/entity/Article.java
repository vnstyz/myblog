package site.vnstyz.myblog.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Article {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer status; // 0: 草稿, 1: 发布
    private Long categoryId;
    private String categoryName;
    private Long createdBy;
    private String createdByUsername;
    private LocalDateTime createdTime;
    private LocalDateTime updateTime;
    private LocalDateTime publishedTime;

    /**
     * 渲染后的 HTML 内容，不持久化到数据库。
     */
    private String contentHtml;
}
