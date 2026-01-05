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
    private Integer id;

    @Schema(name = "source", description = "评论来源标识，如文章ID、树洞ID等", required = true, example = "1")
    @NotNull(message = "评论来源标识不能为空")
    private Integer source;

    @Schema(name = "type", description = "评论来源类型，如article、treehole等", required = true, example = "article")
    @NotBlank(message = "评论来源类型不能为空")
    private String type;

    @Schema(name = "parentCommentId", description = "父评论ID，层主评论为0，回复评论为父评论ID", example = "0")
    private Integer parentCommentId;

    @Schema(name = "parentUserId", description = "父评论用户ID，层主评论为null，回复评论为被回复用户ID", example = "1001")
    private Integer parentUserId;

    @Schema(name = "userId", description = "评论用户ID", example = "1002")
    private Integer userId;

    @Schema(name = "likeCount", description = "点赞数", example = "10")
    private Integer likeCount;

    @Schema(name = "commentContent", description = "评论内容", required = true, example = "这是一条测试评论")
    @NotBlank(message = "评论内容不能为空")
    private String commentContent;

    @Schema(name = "commentInfo", description = "评论附加信息", example = "测试信息")
    private String commentInfo;

    @Schema(name = "floorCommentId", description = "评论楼层ID，子评论必须传递", example = "1")
    private Integer floorCommentId;

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


