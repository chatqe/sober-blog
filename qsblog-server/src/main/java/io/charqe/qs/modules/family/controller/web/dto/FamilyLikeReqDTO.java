package io.charqe.qs.modules.family.controller.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 点赞请求
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "点赞请求")
public class FamilyLikeReqDTO {

    @Schema(description = "家庭卡片ID", required = true, example = "1")
    private Long id;
}
