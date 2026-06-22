package io.charqe.qs.modules.family.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 家庭卡片列表响应VO
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "家庭卡片列表VO")
public class FamilyListVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "男生昵称")
    private String manName;

    @Schema(description = "女生昵称")
    private String womanName;

    @Schema(description = "计时")
    private String timing;

    @Schema(description = "是否启用")
    private Boolean status;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "创建时间")
    private String createTime;
}
