package io.charqe.qs.modules.moment.service;

import io.charqe.qs.modules.moment.domain.dao.MomentMediaMapper;
import io.charqe.qs.modules.moment.domain.pojo.MomentMedia;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 动态文件关联 Service 实现
 *
 * @author Joy
 * @since 2026-06-20
 */
@Service
public class MomentMediaServiceImpl extends CoreServiceImpl<MomentMediaMapper, MomentMedia> implements MomentMediaService {

}
