package io.charqe.qs.modules.moment.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 动态新增/编辑请求
 *
 * @author Joy
 * @since 2026-06-25
 */
@Data
@Schema(description = "动态新增/编辑请求")
public class MomentSaveReqDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @NotNull(message = "发布者ID不能为空")
    @Schema(description = "发布者ID", required = true)
    private Long userId;

    @Schema(description = "纯文本内容")
    private String content;

    @Schema(description = "是否有文本")
    private Boolean hasText;

    @Schema(description = "是否有图片")
    private Boolean hasImages;

    @Schema(description = "是否有视频")
    private Boolean hasVideos;

    @Schema(description = "是否有音频")
    private Boolean hasAudios;

    @Schema(description = "状态: 0-草稿 1-正常 2-审核中 3-违规下架")
    private Integer status;

    @Schema(description = "可见性: 0-私密 1-好友 2-公开")
    private Integer visibility;

    @Schema(description = "来源: 1-本站 2-微信 3-QQ 4-微博 5-抖音")
    private Integer source;

    @Schema(description = "审核/驳回原因")
    private String auditReason;

    @Schema(description = "媒体文件ID列表")
    private List<Long> mediaIds;
}
