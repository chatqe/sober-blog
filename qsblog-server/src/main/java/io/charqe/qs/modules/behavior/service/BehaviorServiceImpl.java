package io.charqe.qs.modules.behavior.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.charqe.qs.modules.behavior.domain.dao.BehaviorMapper;
import io.charqe.qs.modules.behavior.domain.pojo.Behavior;
import io.charqe.qs.modules.behavior.service.BehaviorService;
import org.springframework.stereotype.Service;

/**
 * 用户行为表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class BehaviorServiceImpl extends ServiceImpl<BehaviorMapper, Behavior> implements BehaviorService {

}
