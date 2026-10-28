package io.charqe.qs.modules.comment.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "作者评论列表 VO")
public class AuthorCommentListVO {

    @Schema(description = "总条数")
    private Integer total;

    @Schema(description = "总页数")
    private Integer pages;

    @Schema(description = "当前页码")
    private Integer pageNum;

    @Schema(description = "每页条数")
    private Integer pageSize;

    @Schema(description = "评论列表")
    private List<Item> list;

    @Data
    @Schema(description = "单条评论")
    public static class Item {
        @Schema(description = "评论ID")
        private Long id;

        @Schema(description = "所属文章ID")
        private Long bizId;

        @Schema(description = "评论内容")
        private String content;

        @Schema(description = "评论人用户名")
        private String username;

        @Schema(description = "评论人头像")
        private String avatar;

        @Schema(description = "评论状态：1=正常 2=禁言 3=审核中")
        private Integer status;

        @Schema(description = "直接子评论数量")
        private Integer childCnt;

        @Schema(description = "创建时间")
        private LocalDateTime createTime;
    }
}
