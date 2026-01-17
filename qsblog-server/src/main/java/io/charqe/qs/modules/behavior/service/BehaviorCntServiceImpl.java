package io.charqe.qs.modules.behavior.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.charqe.qs.modules.behavior.domain.dao.BehaviorCntMapper;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorCnt;
import io.charqe.qs.modules.behavior.service.BehaviorCntService;
import org.springframework.stereotype.Service;

/**
 * 用户行为统计表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class BehaviorCntServiceImpl extends ServiceImpl<BehaviorCntMapper, BehaviorCnt> implements BehaviorCntService {

}
