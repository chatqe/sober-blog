package io.charqe.qs.modules.behavior.controller.web.dto;

import lombok.Data;

/**
 * 行为请求DTO（前端只需传 bizId + bizType，行为类型由 Controller 端点决定）
 *
 * @author Joy
 * @since 2026-01-27 21:47
 */
@Data
public class BehDTO {

    /**
     * 用户ID（通常从 token 中获取，前端可不传）
     */
    private Long userId;

    /**
     * 业务ID（文章/视频/相册/评论ID）
     */
    private Long bizId;

    /**
     * 业务类型（对应 BizTypeEnum 或 CommentTypeEnum 编码）
     */
    private Integer bizType;

}
