package io.charqe.qs.modules.moment.controller.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 多平台同步请求DTO
 */
@Data
@Schema(description = "多平台同步请求")
public class MomentSyncReqDTO {

    @NotNull(message = "动态ID不能为空")
    @Schema(description = "动态ID", required = true)
    private Long momentId;

    @Schema(description = "目标平台列表: wechat/qq/weibo/douyin", required = true, example = "[\"wechat\",\"weibo\"]")
    private List<String> platforms;
}
