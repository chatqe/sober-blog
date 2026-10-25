package io.charqe.qs.modules.file.service;

import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileListReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Collection;
import java.util.Set;

/**
 * 文件存储表 服务类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
public interface FileInfoService extends CoreService<FileInfo> {


    public FileRespVO save(FileCreateReqVO vo);

    /**
     * 登记文件元数据：拥有者由调用方按会话决定，服务端补齐存储桶/场景/分类等字段。
     *
     * @param vo        上传后回填的元数据
     * @param ownerId   拥有者 ID，系统任务可为空
     * @param ownerType user/admin/system，为空时按 ownerId 推断
     */
    FileRespVO register(FileCreateReqVO vo, Long ownerId, String ownerType);

    /** 含已删除记录的单条查询；不存在返回 null。 */
    FileInfo findAny(Long id);

    /** 按存储路径查登记记录（含回收站）；路径为空返回 null。 */
    FileInfo findAnyByStoragePath(String path);

    /** 批量判断存储路径是否已登记（含回收站），用于孤儿扫描与补登记。 */
    Set<String> existingStoragePaths(Collection<String> paths);

    /** 软删除（移入回收站），返回受影响行数；记录已删除或不存在时为 0。 */
    int softDelete(Long id);

    /** 从回收站恢复，返回受影响行数；记录不在回收站时为 0。 */
    int restore(Long id);

    /** 永久删除：先清理存储对象，再物理删除元数据。 */
    void purge(FileInfo file);

    /** 回收站分页。 */
    IPage<FileInfo> pageDeleted(FileListReqVO req);

    /** 被引用次数 +1（绑定引用时调用）。 */
    void increaseRefCount(Long id);

    /** 被引用次数 -1（解绑引用时调用，下限为 0）。 */
    void decreaseRefCount(Long id);
}