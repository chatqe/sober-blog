package io.charqe.qs.modules.moment.controller.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 发布/更新动态请求DTO（Web端）
 */
@Data
@Schema(description = "发布/更新动态请求")
public class MomentSaveReqDTO {

    @Schema(description = "动态ID（更新时传入，发布时不传）")
    private Long id;

    @Schema(description = "纯文本内容", example = "今天天气真好")
    private String content;

    /**
     * @deprecated 使用 {@link #mediaList} 替代，保留用于兼容旧前端
     */
    @Deprecated
    @Schema(description = "媒体文件ID列表（已废弃，请使用 mediaList）", example = "[1,2,3]")
    private List<Long> mediaIds;

    @Schema(description = "可见性: 0-私密 1-好友 2-公开，默认2", example = "2")
    private Integer visibility;

    @Schema(description = "媒体文件列表（含类型与排序）")
    private List<MediaItem> mediaList;

    /**
     * 单个媒体文件项
     */
    @Data
    @Schema(description = "单个媒体文件项")
    public static class MediaItem {

        @Schema(description = "文件ID", example = "1")
        private Long fileId;

        @Schema(description = "媒体类型: 1-图片 2-视频 3-音频", example = "1")
        private Integer mediaType;

        @Schema(description = "排序权重", example = "0")
        private Integer sortOrder;
    }
}
