package io.charqe.qs.modules.moment.service;

import io.charqe.qs.modules.moment.domain.dao.MomentSocialSyncMapper;
import io.charqe.qs.modules.moment.domain.pojo.MomentSocialSync;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 三方同步记录 Service 实现
 *
 * @author Joy
 * @since 2026-06-20
 */
@Service
public class MomentSocialSyncServiceImpl extends CoreServiceImpl<MomentSocialSyncMapper, MomentSocialSync> implements MomentSocialSyncService {

}
