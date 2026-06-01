package io.charqe.qs.modules.behavior.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.behavior.convert.BehaviorConvert;
import io.charqe.qs.modules.behavior.domain.dao.BehaviorLogMapper;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户行为表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
@Slf4j
public class BehaviorLogServiceImpl extends CoreServiceImpl<BehaviorLogMapper, BehaviorLog> implements BehaviorLogService {


    @Resource
    private BehaviorCntService behCntService;

    @Resource
    private BehaviorConvert behaviorConvert;




}
