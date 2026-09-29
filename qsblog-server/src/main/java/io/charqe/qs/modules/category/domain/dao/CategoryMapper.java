package io.charqe.qs.modules.category.domain.dao;

import io.charqe.qs.modules.category.controller.web.dto.ArticleCategoryNameDTO;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.common.model.mp.CoreMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
public interface CategoryMapper extends CoreMapper<Category> {

    List<ArticleCategoryNameDTO> selectBatchIdsWithNames(@Param("ids") List<Long> ids);
}
