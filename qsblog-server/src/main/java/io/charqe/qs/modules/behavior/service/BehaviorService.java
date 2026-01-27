package io.charqe.qs.modules.behavior.service;

import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.behavior.controller.web.dto.BehaviorReqDTO;
import io.charqe.qs.modules.behavior.domain.pojo.Behavior;

/**
 * 用户行为表 服务类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
public interface BehaviorService extends CoreService<Behavior> {

    /**
     * 处理用户行为
     *
     * @param dto 行为请求参数
     * @return 行为结果
     */
    Object handleBehavior(BehaviorReqDTO dto);
}
