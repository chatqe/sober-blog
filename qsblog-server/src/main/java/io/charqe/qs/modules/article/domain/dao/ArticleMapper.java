package io.charqe.qs.modules.article.domain.dao;

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

    /**
     * 按分类ID分组统计文章数量（已过滤已删除文章）
     *
     * @return Map<categoryId, count>
     */
    @Select("SELECT category_id AS id, COUNT(*) AS cnt FROM article WHERE is_del = 0 AND category_id IS NOT NULL GROUP BY category_id")
    Map<Long, Long> countByCategoryId();

    /**
     * 按标签ID分组统计文章数量（已过滤已删除文章）
     *
     * @return Map<tagId, count>
     */
    @Select("SELECT tag_id AS id, COUNT(*) AS cnt FROM article WHERE is_del = 0 AND tag_id IS NOT NULL GROUP BY tag_id")
    Map<Long, Long> countByTagId();
}


