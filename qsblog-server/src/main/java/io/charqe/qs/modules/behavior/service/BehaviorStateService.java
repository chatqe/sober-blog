package io.charqe.qs.modules.behavior.service;

import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.behavior.controller.admin.dto.AdminBehStateReqDTO;
import io.charqe.qs.modules.behavior.controller.admin.vo.AdminBehRespVO;
import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorLog;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorState;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户行为状态表 服务类
 *
 * @author Joy
 * @since 2026-03-15 20:04
 */
public interface BehaviorStateService  extends CoreService<BehaviorState> {
    @Transactional(rollbackFor = Exception.class)
    boolean handleBeh(BehDTO dto,boolean flag);

    boolean checkHasBeh(BehaviorState beh);


    /**
     * 查看用户对某个行为主体有多少行为
     * @param
     * @return
     */
    AdminBehRespVO getBehList(AdminBehStateReqDTO dto);

}
