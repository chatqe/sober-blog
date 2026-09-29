package io.charqe.qs.modules.moment.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态同步记录VO
 */
@Data
@Schema(description = "动态同步记录VO")
public class MomentSyncRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "动态ID")
    private Long momentId;

    @Schema(description = "同步方向: 1-出向 2-入向")
    private Integer direction;

    @Schema(description = "平台标识")
    private String platform;

    @Schema(description = "三方平台返回的发布ID")
    private String externalId;

    @Schema(description = "同步状态: 0-待同步 1-同步中 2-成功 3-失败")
    private Integer syncStatus;

    @Schema(description = "失败时的报错信息")
    private String errorMessage;

    @Schema(description = "同步完成时间")
    private LocalDateTime syncedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
