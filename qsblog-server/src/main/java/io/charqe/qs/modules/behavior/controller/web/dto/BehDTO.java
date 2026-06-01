package io.charqe.qs.modules.behavior.controller.web.dto;

import lombok.Data;

/**
 * 行为DTO
 *
 * @author Joy
 * @since 2026-01-27 21:47
 */
@Data
public class BehDTO {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 业务ID 文章/视频/相册/评论ID
     */
    private Long bizId;
    /**
     * 业务类型  1-文章 2-评论 3-视频 4-相册 5-专辑
     */
    private Integer bizType;
    /**
     * 操作类型 1-点赞 2-点踩 3-订阅 4-取消订阅 5-取消点赞 6-取消点踩
     */
    private Integer action;

}
