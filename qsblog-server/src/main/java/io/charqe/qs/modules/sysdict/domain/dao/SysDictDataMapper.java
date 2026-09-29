package io.charqe.qs.modules.sysdict.domain.dao;

import io.charqe.qs.modules.sysdict.domain.pojo.SysDictData;
import io.charqe.qs.common.model.mp.CoreMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 字典数据表 Mapper 接口
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
public interface SysDictDataMapper extends CoreMapper<SysDictData> {

    /**
     * 根据字典类型编码批量删除关联数据
     *
     * @param typeCode 字典类型编码
     * @return 影响行数
     */
    int deleteByTypeCode(@Param("typeCode") String typeCode);
}