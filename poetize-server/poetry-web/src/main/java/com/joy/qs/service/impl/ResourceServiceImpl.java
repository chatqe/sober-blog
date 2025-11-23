package com.joy.qs.service.impl;

import com.joy.qs.dao.ResourceMapper;
import com.joy.qs.model.resource.pojo.Resource;
import com.joy.qs.service.ResourceService;
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
