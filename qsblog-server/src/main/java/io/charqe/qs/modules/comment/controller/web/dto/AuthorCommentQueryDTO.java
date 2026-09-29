package io.charqe.qs.modules.comment.controller.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "作者查询自己文章下评论的请求参数")
public class AuthorCommentQueryDTO {

    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，最大50", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "评论状态过滤：1=正常 2=禁言 3=审核中，不传则查全部", example = "1")
    private Integer status;

    @Schema(description = "评论内容关键词模糊搜索", example = "很棒")
    private String searchKey;
}
