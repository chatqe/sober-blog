package io.charqe.qs.modules.sysdict.service;

import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.sysdict.controller.admin.dto.SysDictDataReq;
import io.charqe.qs.modules.sysdict.controller.web.vo.SysDictDataVO;
import io.charqe.qs.modules.sysdict.domain.pojo.SysDictData;

import java.util.List;

/**
 * 字典数据服务类
 *
 * @author Joy
 * @since 2026-06-22
 */
public interface SysDictDataService extends CoreService<SysDictData> {

    /**
     * 根据字典类型编码查询启用的字典数据列表（按sort排序）
     */
    List<SysDictDataVO> listByTypeCode(String typeCode);

    /**
     * 批量查询多个字典类型的字典数据
     */
    java.util.Map<String, List<SysDictDataVO>> batchListByTypeCodes(List<String> typeCodes);

    /**
     * 新增字典数据
     */
    void save(SysDictDataReq req);

    /**
     * 更新字典数据
     */
    void update(SysDictDataReq req);

    /**
     * 批量删除字典数据
     */
    void batchDelete(Long[] ids);

    /**
     * 根据typeCode和itemValue获取字典项label
     */
    String getItemLabel(String typeCode, String itemValue);
}
