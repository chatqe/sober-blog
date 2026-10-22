package io.charqe.qs.modules.message.controller.admin.dto;

import io.charqe.qs.common.model.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * admin留言分页请求DTO
 *
 * @author Joy
 * @since 2026-10-09 20:37
 */
@Data
@Schema(description = "后台留言分页请求视图对象")
public class AdminMessagePageReqDTO extends PageQuery {

    @Schema(description = "关键字")
    private String keyword;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    @Schema(description = "是否删除")
    private Boolean isDel;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "用户类型 1=登录用户，0=游客")
    private Integer userType;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "是否有回复")
    private Boolean hasReply;
}
