package io.charqe.qs.modules.article.controller.admin.vo;

import io.charqe.qs.common.model.PageQuery;
import io.charqe.qs.common.model.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台文章分页请求dto
 *
 * @author Joy
 * @since 2026-09-21 21:41
 */
@Data
@Schema(description = "后台文章分页响应结果")
public class AdminArticlePageRespVO extends PageResult<AdminArticleVO> {

}
