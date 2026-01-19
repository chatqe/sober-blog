package io.charqe.qs.modules.moment.service;

import io.charqe.qs.modules.moment.domain.dao.MomentMapper;
import io.charqe.qs.modules.moment.domain.pojo.Moment;
import io.charqe.qs.modules.moment.service.MomentService;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import org.springframework.stereotype.Service;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Service
public class MomentServiceImpl extends CoreServiceImpl<MomentMapper, Moment> implements MomentService {

}

