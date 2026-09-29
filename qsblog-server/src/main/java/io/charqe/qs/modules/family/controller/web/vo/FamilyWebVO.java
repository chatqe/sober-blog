package io.charqe.qs.modules.family.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 家庭卡片公开列表VO
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "家庭卡片公开列表VO")
public class FamilyWebVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "背景封面")
    private String bgCover;

    @Schema(description = "男生昵称")
    private String manName;

    @Schema(description = "女生昵称")
    private String womanName;

    @Schema(description = "计时")
    private String timing;

    @Schema(description = "点赞数")
    private Integer likeCount;
}
