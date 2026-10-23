package io.charqe.qs.modules.message.controller.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "留言回复视图对象")
public class MessageReplyVO {

    @Schema(description = "回复ID", example = "1")
    private Long id;

    @Schema(description = "主留言ID", example = "1")
    private Long messageId;

    @Schema(description = "父回复ID，0=回复主留言", example = "0")
    private Long parentId;

    @Schema(description = "顶级回复ID，0=自身为根", example = "0")
    private Long rootId;

    @Schema(description = "被回复人ID", example = "0")
    private Long replyToId;

    @Schema(description = "被回复人昵称", example = "管理员")
    private String replyToName;

    @Schema(description = "昵称", example = "访客")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "回复内容", example = "这是回复内容")
    private String content;

    @Schema(description = "IP地理位置（展示用）", example = "香港")
    private String ipLocation;

    @Schema(description = "点赞数", example = "2")
    private Integer likeCount;

    @Schema(description = "点踩数", example = "0")
    private Integer dislikeCount;

    @Schema(description = "直接子回复数", example = "1")
    private Integer childCnt;

    @Schema(description = "是否管理员回复", example = "false")
    private Integer isAdmin;

    @Schema(description = "创建时间", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
