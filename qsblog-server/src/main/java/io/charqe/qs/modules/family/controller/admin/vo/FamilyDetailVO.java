package io.charqe.qs.modules.family.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 家庭卡片详情响应VO
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "家庭卡片详情VO")
public class FamilyDetailVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "背景封面")
    private String bgCover;

    @Schema(description = "男生头像")
    private String manCover;

    @Schema(description = "女生头像")
    private String womanCover;

    @Schema(description = "男生昵称")
    private String manName;

    @Schema(description = "女生昵称")
    private String womanName;

    @Schema(description = "计时")
    private String timing;

    @Schema(description = "倒计时标题")
    private String countdownTitle;

    @Schema(description = "倒计时时间")
    private String countdownTime;

    @Schema(description = "额外信息")
    private String familyInfo;

    @Schema(description = "是否启用")
    private Boolean status;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;
}
