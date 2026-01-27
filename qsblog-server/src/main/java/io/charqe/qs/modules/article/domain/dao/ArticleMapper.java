package io.charqe.qs.modules.article.domain.dao;

import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.common.model.mp.CoreMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
}


