package io.charqe.qs.modules.sysdict.domain.dao;

import io.charqe.qs.modules.sysdict.domain.pojo.SysDictType;
import io.charqe.qs.common.model.mp.CoreMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 字典类型表 Mapper 接口
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
public interface SysDictTypeMapper extends CoreMapper<SysDictType> {

    /**
     * 检查指定 type_code 是否存在（逻辑删除后不计入）
     *
     * @param typeCode 字典类型编码
     * @return 存在返回 1，否则返回 0
     */
    int existsByTypeCode(@Param("typeCode") String typeCode);
}