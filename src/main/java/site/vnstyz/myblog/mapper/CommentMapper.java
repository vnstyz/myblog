package site.vnstyz.myblog.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import site.vnstyz.myblog.entity.Comment;

import java.util.List;

@Mapper
public interface CommentMapper {

    // 插入评论
    int insert(Comment comment);

    // 查询某篇文章的可见评论（按时间正序，先评论的在前）
    List<Comment> findVisibleByArticleId(@Param("articleId") Long articleId);
}
