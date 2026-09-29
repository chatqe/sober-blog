package io.charqe.qs.modules.family.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 家庭卡片新增/编辑请求
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "家庭卡片请求")
public class FamilySaveReqDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @NotBlank(message = "背景封面不能为空")
    @Schema(description = "背景封面", example = "https://example.com/bg.jpg")
    private String bgCover;

    @NotBlank(message = "男生头像不能为空")
    @Schema(description = "男生头像", example = "https://example.com/man.jpg")
    private String manCover;

    @NotBlank(message = "女生头像不能为空")
    @Schema(description = "女生头像", example = "https://example.com/woman.jpg")
    private String womanCover;

    @NotBlank(message = "男生昵称不能为空")
    @Schema(description = "男生昵称", example = "小明")
    private String manName;

    @NotBlank(message = "女生昵称不能为空")
    @Schema(description = "女生昵称", example = "小红")
    private String womanName;

    @NotBlank(message = "计时不能为空")
    @Schema(description = "计时", example = "在一起 100 天")
    private String timing;

    @Schema(description = "倒计时标题", example = "我们的纪念日")
    private String countdownTitle;

    @Schema(description = "倒计时时间", example = "2025-01-01 00:00:00")
    private String countdownTime;

    @Schema(description = "额外信息(JSON)", example = "{}")
    private String familyInfo;
}
