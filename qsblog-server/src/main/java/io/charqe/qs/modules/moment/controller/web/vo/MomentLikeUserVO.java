package io.charqe.qs.modules.moment.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动态点赞用户VO
 */
@Data
@Schema(description = "动态点赞用户VO")
public class MomentLikeUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像URL")
    private String avatar;
}
