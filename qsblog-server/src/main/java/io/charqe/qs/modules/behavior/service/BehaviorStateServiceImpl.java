package io.charqe.qs.modules.behavior.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.behavior.constant.BehConstant;
import io.charqe.qs.modules.behavior.controller.admin.dto.AdminBehStateReqDTO;
import io.charqe.qs.modules.behavior.controller.admin.vo.AdminBehRespVO;
import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.convert.BehaviorConvert;
import io.charqe.qs.modules.behavior.domain.dao.BehaviorStateMapper;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorCnt;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorState;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 用户行为状态实现类
 *
 * @author Joy
 * @since 2026-03-15 20:05
 */

@Service
@Slf4j
public class BehaviorStateServiceImpl extends CoreServiceImpl<BehaviorStateMapper, BehaviorState> implements BehaviorStateService {

    @Resource
    private BehaviorConvert behaviorConvert;
    @Resource
    private BehaviorCntService behCntService;


    /**
     * 处理用户行为
     *
     * @param dto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleBeh(BehDTO dto, boolean flag) {

        // 是否为赞/踩操作
        boolean mutexFlag = BehActionType.LIKE.code().equals(dto.getAction()) || BehActionType.DISLIKE.code().equals(dto.getAction());

        if (mutexFlag) {
            if (flag) {
                //
                list(Lb.<BehaviorState>q()
                        .eq(Objects.nonNull(dto.getUserId()), BehaviorState::getUserId, dto.getUserId())
                        .eq(Objects.nonNull(dto.getBizId()), BehaviorState::getBizId, dto.getBizId())
                        .eq(Objects.nonNull(dto.getBizType()), BehaviorState::getBizType, dto.getBizType())
                        .eq(BehaviorState::getMutexKey, BehConstant.LIKE_OR_DISLIKE));
            } else {
                // 不存在则保存
            }
        }

        BehaviorState behState = getOne(Lb.<BehaviorState>q()
                .eq(Objects.nonNull(dto.getUserId()), BehaviorState::getUserId, dto.getUserId())
                .eq(Objects.nonNull(dto.getBizId()), BehaviorState::getBizId, dto.getBizId())
                .eq(Objects.nonNull(dto.getBizType()), BehaviorState::getBizType, dto.getAction())
                .eq(Objects.nonNull(dto.getAction()), BehaviorState::getAction, dto.getAction())
        );

        // 如果不存在
        if (Objects.isNull(behState)) {
            BehaviorState state = behaviorConvert.convert(dto);
            if (BehActionType.LIKE.code().equals(dto.getAction())) {
                BehaviorState disLikeBeh = getOne(Lb.<BehaviorState>q()
                        .eq(Objects.nonNull(dto.getUserId()), BehaviorState::getUserId, dto.getUserId())
                        .eq(Objects.nonNull(dto.getBizId()), BehaviorState::getBizId, dto.getBizId())
                        .eq(Objects.nonNull(dto.getBizType()), BehaviorState::getBizType, dto.getBizType())
                        .eq(Objects.nonNull(dto.getAction()), BehaviorState::getAction, BehActionType.DISLIKE)
                );
                if (Objects.nonNull(disLikeBeh)) {
                    // 互斥行为存在 删除
                    deleteById(disLikeBeh);

                }
            } else if (BehActionType.DISLIKE.code().equals(dto.getAction())) {

            }
        } else {
            // 存在

        }


        save(behaviorConvert.convert(dto));
        log.info("保存行为记录成功，userId: {}, bizType: {}, action: {}",
                dto.getUserId(), dto.getBizType(), dto.getAction());


        return false;
    }

    @Override
    public boolean checkHasBeh(BehaviorState beh) {

        LambdaQueryWrapper<BehaviorState> wrapper = Lb.<BehaviorState>q()
                .eq(Objects.nonNull(beh.getUserId()), BehaviorState::getUserId, beh.getUserId())
                .eq(Objects.nonNull(beh.getBizId()), BehaviorState::getBizId, beh.getBizId())
                .eq(Objects.nonNull(beh.getBizType()), BehaviorState::getBizType, beh.getBizType())
                .orderByDesc(BehaviorState::getCreateTime)
                .last("limit 1");


        BehaviorState dbBeh = getOne(wrapper);
        return beh.getAction().equals(dbBeh.getAction());
    }

    @Override
    public AdminBehRespVO getBehList(AdminBehStateReqDTO dto) {
        return null;
    }

    /**
     * 保存行为 及计数对象
     */
    private void saveBehAndCount(BehaviorState behaviorState) {

        save(behaviorState);

        // 查看是否已存在
        BehaviorCnt behCnt = behCntService.getOne(Lb.<BehaviorCnt>q()
                .eq(BehaviorCnt::getBizId, behaviorState.getBizId())
                .eq(BehaviorCnt::getBizType, behaviorState.getBizType())
                .eq(BehaviorCnt::getAction, behaviorState.getAction()));
        if (Objects.isNull(behCnt)) {
            behCnt = new BehaviorCnt();
            behCnt.setBizId(behaviorState.getBizId());
            behCnt.setBizType(behaviorState.getBizType());
            behCnt.setAction(behaviorState.getAction());
            behCnt.setCount(1);
        } else {
            // behCnt.setCount(behCnt.getCount() + BehActionType.codeByName(behaviorState.getAction()));
        }


        behCntService.saveOrUpdate(behCnt);
        log.info("保存行为记录成功，userId: {}, bizType: {}, action: {}",
                behaviorState.getUserId(), behaviorState.getBizType(), behaviorState.getAction());

    }
}
