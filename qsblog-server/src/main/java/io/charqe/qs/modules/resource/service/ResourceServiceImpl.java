package io.charqe.qs.modules.resource.service;

import io.charqe.qs.modules.resource.domain.dao.ResourceMapper;
import io.charqe.qs.modules.resource.domain.pojo.Resource;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import org.springframework.stereotype.Service;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Service
public class ResourceServiceImpl extends CoreServiceImpl<ResourceMapper, Resource> implements ResourceService {

}


