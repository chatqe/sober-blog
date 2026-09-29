package io.charqe.qs.modules.article.controller.admin.dto;

import io.charqe.qs.common.model.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台文章分页请求dto
 *
 * @author Joy
 * @since 2026-09-21 21:41
 */
@Data
@Schema(description = "后台文章分页查询请求参数")
public class AdminArticlePageReqDTO extends PageQuery {

    @Schema(description = "文章标题模糊搜索关键词")
    private String titleKey;

    @Schema(description = "分类ID，用于筛选")
    private Long cateId;

    @Schema(description = "标签ID，用于筛选")
    private Long tagId;

    @Schema(description = "是否推荐，null表示不筛选")
    private Boolean recommend;
}
