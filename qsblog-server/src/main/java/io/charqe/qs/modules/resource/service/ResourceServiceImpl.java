package io.charqe.qs.modules.resource.service;

import io.charqe.qs.modules.resource.domain.dao.ResourceMapper;
import io.charqe.qs.modules.resource.domain.pojo.Resource;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 资源信息 服务实现类
 * </p>
 *
 * @author sara
 * @since 2022-03-06
 */
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {

}


