package io.charqe.qs.modules.comment.controller.web.vo;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Schema(name = "CommentVO", description = "评论视图对象，用于评论的前端展示和交互")
public class CommentVO {

    @Schema(name = "id", description = "评论ID", example = "1")
    private Long id;

    @Schema(name = "bizId", description = "业务主键ID，如文章ID、树洞ID等", required = true, example = "1")
    @NotNull(message = "业务主键ID不能为空")
    private Long bizId;

    @Schema(name = "bizType", description = "业务类型：1-文章 2-树洞留言 3-表白墙留言", required = true, example = "1")
    @NotNull(message = "业务类型不能为空")
    private Integer bizType;

    @Schema(name = "parentId", description = "父评论ID，0=根评论", example = "0")
    private Long parentId;

    @Schema(name = "replyToUserId", description = "回复目标用户ID", example = "1001")
    private Long replyToUserId;

    @Schema(name = "authorId", description = "评论人ID", example = "1002")
    private Long authorId;

    @Schema(name = "likeCnt", description = "点赞数", example = "10")
    private Integer likeCnt;

    @Schema(name = "content", description = "评论内容", required = true, example = "这是一条测试评论")
    @NotBlank(message = "评论内容不能为空")
    private String content;

    @Schema(name = "rootId", description = "顶级祖先评论ID，0=自身为根", example = "0")
    private Long rootId;

    @Schema(name = "createTime", description = "创建时间", format = "date-time", example = "2023-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(name = "childComments", description = "子评论分页列表")
    private Page childComments;

    @Schema(name = "parentUsername", description = "父评论用户名", example = "user1")
    private String parentUsername;

    @Schema(name = "username", description = "评论用户名", example = "user2")
    private String username;

    @Schema(name = "avatar", description = "用户头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;
}
