package io.charqe.qs.modules.tag.domain.dao;

import io.charqe.qs.modules.tag.controller.web.dto.ArticleTagNameDTO;
import io.charqe.qs.modules.tag.domain.pojo.Tag;
import io.charqe.qs.common.model.mp.CoreMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 标签表 Mapper 接口
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
public interface TagMapper extends CoreMapper<Tag> {

    List<ArticleTagNameDTO> selectBatchIdsWithNames(@Param("ids") List<Long> ids);
}