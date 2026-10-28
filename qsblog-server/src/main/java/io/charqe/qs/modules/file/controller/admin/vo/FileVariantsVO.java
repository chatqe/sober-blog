package io.charqe.qs.modules.file.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 派生图访问地址：三档宽度，均为 CDN 变换 URL，不占额外存储。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
@Schema(description = "派生图访问地址")
public class FileVariantsVO {

    @Schema(description = "缩略图（320px）")
    private String thumb;

    @Schema(description = "卡片图（800px）")
    private String card;

    @Schema(description = "详情大图（1600px）")
    private String hero;
}
