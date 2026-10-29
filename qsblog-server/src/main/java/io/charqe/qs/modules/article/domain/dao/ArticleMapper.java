package io.charqe.qs.modules.article.domain.dao;

import io.charqe.qs.modules.article.domain.dto.ArticleStatsDTO;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.common.model.mp.CoreMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Mapper
public interface ArticleMapper extends CoreMapper<Article> {

    @Update("update article set view_count=view_count+1 where id=#{id}")
    int updateViewCount(@Param("id") Long id);

    @Update("update article set like_count=#{likeCount} where id=#{id}")
    int updateLikeCount(@Param("id") Long id, @Param("likeCount") Integer likeCount);

    /**
     * 按标签ID分组统计文章数量（已过滤已删除文章）
     *
     * @return Map<tagId, count>
     */
    @Select("SELECT tag_id AS id, COUNT(*) AS cnt FROM article WHERE is_del = 0 AND publish_status = 1 AND tag_id IS NOT NULL GROUP BY tag_id")
    Map<Long, Long> countByTagId();

    /**
     * 统计全部未删除文章的总数、累计浏览量、累计点赞数
     * 口径：is_del = 0，不区分文章可见性
     *
     * @return 文章统计聚合数据
     */
    @Select("SELECT COUNT(*) AS article_total, " +
            "CAST(COALESCE(SUM(view_count), 0) AS SIGNED) AS view_total, " +
            "CAST(COALESCE(SUM(like_count), 0) AS SIGNED) AS like_total " +
            "FROM article WHERE is_del = 0")
    ArticleStatsDTO selectStats();
}


