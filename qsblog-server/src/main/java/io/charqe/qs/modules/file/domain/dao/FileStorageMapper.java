package io.charqe.qs.modules.file.domain.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.common.model.mp.CoreMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 文件存储表 Mapper 接口
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
public interface FileStorageMapper extends CoreMapper<FileInfo> {

    /** 软删除：同时写入回收站时间，便于按时间清理。 */
    @Update("UPDATE file_info SET is_del = 1, deleted_at = NOW(3) WHERE id = #{id} AND is_del = 0")
    int softDeleteById(@Param("id") Long id);

    /** 从回收站恢复：清空回收站时间。 */
    @Update("UPDATE file_info SET is_del = 0, deleted_at = NULL WHERE id = #{id} AND is_del = 1")
    int restoreById(@Param("id") Long id);

    /** 物理删除：绕开逻辑删除，仅用于引用为 0 的永久删除。 */
    @Delete("DELETE FROM file_info WHERE id = #{id}")
    int purgeById(@Param("id") Long id);

    /** 含已删除记录的查询，用于回收站与永久删除前置校验。 */
    @Select("SELECT * FROM file_info WHERE id = #{id}")
    FileInfo selectAnyById(@Param("id") Long id);

    @Update("UPDATE file_info SET ref_count = ref_count + 1 WHERE id = #{id}")
    int increaseRefCount(@Param("id") Long id);

    @Update("UPDATE file_info SET ref_count = GREATEST(ref_count - 1, 0) WHERE id = #{id}")
    int decreaseRefCount(@Param("id") Long id);

    /** 回收站分页：自定义 SQL 不受逻辑删除拦截器影响。 */
    @Select({"<script>",
            "SELECT * FROM file_info WHERE is_del = 1",
            "<if test=\"scene != null and scene != ''\">AND scene = #{scene}</if>",
            "<if test=\"keyword != null and keyword != ''\">AND original_name LIKE CONCAT('%', #{keyword}, '%')</if>",
            "ORDER BY deleted_at DESC, id DESC",
            "</script>"})
    IPage<FileInfo> selectDeletedPage(IPage<FileInfo> page, @Param("scene") String scene,
                                      @Param("keyword") String keyword);

}