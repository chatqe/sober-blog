package io.charqe.qs.modules.sysdict.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.sysdict.domain.dao.SysDictTypeMapper;
import io.charqe.qs.modules.sysdict.domain.pojo.SysDictType;
import io.charqe.qs.modules.sysdict.service.SysDictTypeService;
import org.springframework.stereotype.Service;

/**
 * 字典类型表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class SysDictTypeServiceImpl extends CoreServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictTypeService {

}