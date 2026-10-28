package io.charqe.qs.modules.file.domain.dao;

import io.charqe.qs.modules.file.domain.pojo.FileBizRef;
import io.charqe.qs.common.model.mp.CoreMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 文件业务关系表 Mapper 接口
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
public interface FileBizRelationMapper extends CoreMapper<FileBizRef> {

    /** 解绑直接物理删除：依赖 (file_id,biz_type,biz_id,biz_field) 唯一键拒绝重复绑定。 */
    @Delete("DELETE FROM file_biz_ref WHERE id = #{id}")
    int purgeById(@Param("id") Long id);

}