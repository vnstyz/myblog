package site.vnstyz.myblog.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import site.vnstyz.myblog.entity.Article;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleMapper {

    // 查询所有文章
    List<Article> findAll();

    // 根据ID查询文章
    Article findById(@Param("id") Long id);

    // 添加文章
    int insert(Article article);

    // 更新文章
    int update(Article article);

    // 根据ID删除文章
    int deleteById(@Param("id") Long id);

    // 根据状态查询文章
    List<Article> findByStatus(@Param("status") Integer status);

    // 更新浏览数
    int updateViewCount(@Param("id") Long id, @Param("count") Integer count);

    // 更新点赞数
    int updateLikeCount(@Param("id") Long id, @Param("count") Integer count);

    // 获取统计数据
    Map<String, Object> getStats();

    // 获取热门文章（按浏览量排序）
    List<Article> findHotArticles(@Param("limit") int limit);
}