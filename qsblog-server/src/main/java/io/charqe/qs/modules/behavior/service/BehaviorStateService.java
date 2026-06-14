package io.charqe.qs.modules.behavior.service;

import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorState;

/**
 * 用户行为状态表 服务类（纯CRUD，业务逻辑已拆分到各行为Service）
 *
 * @author Joy
 * @since 2026-03-15 20:04
 */
public interface BehaviorStateService extends CoreService<BehaviorState> {

    /**
     * 检查用户是否已有某种行为状态
     *
     * @param beh 查询条件
     * @return true-存在活跃行为
     */
    boolean checkHasBeh(BehaviorState beh);
}
