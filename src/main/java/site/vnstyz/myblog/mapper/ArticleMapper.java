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

    // 浏览量绝对值写回（Redis 落库用）
    int setViewCount(@Param("id") Long id, @Param("count") Long count);

    // 仅查询单篇文章浏览量（Redis 缺失回落用）
    Long findViewCountById(@Param("id") Long id);

    // 轻量查询：文章 id 与浏览量（预热/落库遍历用）
    List<Map<String, Object>> selectIdAndViews();

    // 更新点赞数
    int updateLikeCount(@Param("id") Long id, @Param("count") Integer count);

    // 获取统计数据
    Map<String, Object> getStats();

    // 获取热门文章（按浏览量排序）
    List<Article> findHotArticles(@Param("limit") int limit);
}