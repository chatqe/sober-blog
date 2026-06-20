package io.charqe.qs.modules.moment.service;

import io.charqe.qs.modules.moment.domain.dao.MomentMapper;
import io.charqe.qs.modules.moment.domain.pojo.Moment;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 动态 Service 实现
 *
 * @author Joy
 * @since 2026-06-20
 */
@Service
public class MomentServiceImpl extends CoreServiceImpl<MomentMapper, Moment> implements MomentService {

}
