package io.charqe.qs.modules.sysdict.service;

import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.sysdict.controller.admin.dto.SysDictTypeReq;
import io.charqe.qs.modules.sysdict.domain.pojo.SysDictType;

/**
 * 字典类型服务类
 *
 * @author Joy
 * @since 2026-06-22
 */
public interface SysDictTypeService extends CoreService<SysDictType> {

    /**
     * 分页查询字典类型
     */
    PageResult<SysDictType> page(String typeName, Integer status, int pageNum, int pageSize);

    /**
     * 新增字典类型
     */
    void save(SysDictTypeReq req);

    /**
     * 更新字典类型
     */
    void update(SysDictTypeReq req);

    /**
     * 批量删除字典类型
     */
    void batchDelete(Long[] ids);

    /**
     * 获取所有启用的字典类型列表
     */
    java.util.List<SysDictType> listEnabled();
}
