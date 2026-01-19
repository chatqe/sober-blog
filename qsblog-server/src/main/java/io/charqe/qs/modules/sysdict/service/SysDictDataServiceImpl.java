package io.charqe.qs.modules.sysdict.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.sysdict.domain.dao.SysDictDataMapper;
import io.charqe.qs.modules.sysdict.domain.pojo.SysDictData;
import io.charqe.qs.modules.sysdict.service.SysDictDataService;
import org.springframework.stereotype.Service;

/**
 * 字典数据表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class SysDictDataServiceImpl extends CoreServiceImpl<SysDictDataMapper, SysDictData> implements SysDictDataService {

}