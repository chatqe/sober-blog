package io.charqe.qs.modules.moment.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动态同步结果VO
 */
@Data
@Schema(description = "动态同步结果VO")
public class MomentSyncVO {

    @Schema(description = "动态ID")
    private Long momentId;

    @Schema(description = "同步成功平台列表")
    private java.util.List<String> successPlatforms;

    @Schema(description = "同步失败平台列表")
    private java.util.List<String> failPlatforms;
}
