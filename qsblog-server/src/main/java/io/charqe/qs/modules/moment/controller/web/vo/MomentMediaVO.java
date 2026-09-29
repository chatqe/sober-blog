package io.charqe.qs.modules.moment.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动态媒体文件VO
 */
@Data
@Schema(description = "媒体文件VO")
public class MomentMediaVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "媒体类型: 1-图片 2-视频 3-音频")
    private Integer mediaType;

    @Schema(description = "排序权重")
    private Integer sortOrder;

    @Schema(description = "文件URL")
    private String url;
}
