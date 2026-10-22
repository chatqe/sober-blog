package io.charqe.qs.modules.message.controller.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "留言视图对象")
public class MessageVO {

    @Schema(description = "留言ID", example = "1")
    private Long id;

    @Schema(description = "昵称", example = "访客")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "留言内容", example = "这是一条测试留言")
    private String content;

    @Schema(description = "IP地理位置（展示用）", example = "香港")
    private String ipLocation;

    @Schema(description = "操作系统信息", example = "Windows 10")
    private String os;

    @Schema(description = "浏览器信息", example = "Chrome")
    private String browser;

    @Schema(description = "点赞数", example = "5")
    private Integer likeCount;

    @Schema(description = "点踩数", example = "0")
    private Integer dislikeCount;

    @Schema(description = "回复数", example = "3")
    private Integer replyCount;

    @Schema(description = "是否置顶", example = "false")
    private Integer isTop;

    @Schema(description = "创建时间", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
