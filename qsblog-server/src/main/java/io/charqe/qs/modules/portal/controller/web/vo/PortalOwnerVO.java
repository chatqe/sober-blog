package io.charqe.qs.modules.portal.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 站长信息 VO
 * 动态页头部等前台场景需要匿名可见的作者信息
 *
 * @author Joy
 * @since 2026-10-24
 */
@Data
@Schema(description = "站长信息")
public class PortalOwnerVO {

    @Schema(description = "站长用户ID")
    private Long userId;

    @Schema(description = "站长用户名")
    private String username;

    @Schema(description = "站长头像URL")
    private String avatar;

    @Schema(description = "站长简介")
    private String introduction;
}
