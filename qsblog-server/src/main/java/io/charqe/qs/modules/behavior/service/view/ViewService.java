package io.charqe.qs.modules.behavior.service.view;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;

/**
 * 浏览服务接口（无取消操作）
 *
 * @author Joy
 * @since 2026-06-12
 */
public interface ViewService {

    /**
     * 记录浏览行为（异步，幂等去重）
     *
     * @param dto    请求参数（bizId + bizType）
     * @param userId 操作人ID（可为null，表示匿名用户）
     */
    void view(BehDTO dto, Long userId);
}
