package io.charqe.qs.modules.article.domain.dao;

import io.charqe.qs.modules.article.domain.pojo.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
public interface ArticleMapper extends BaseMapper<Article> {

    @Update("update article set view_count=view_count+1 where id=#{id}")
    int updateViewCount(@Param("id") Integer id);
}


