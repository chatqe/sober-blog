package io.charqe.qs.modules.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.charqe.qs.modules.file.controller.admin.vo.FileBatchReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileBatchRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanBackfillReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanBackfillRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanScanReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanScanRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileUnreferencedReqVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;

/**
 * 文件治理：孤儿扫描/补登记、未引用清单、批量清理。
 *
 * @author Joy
 * @since 2026-10-26
 */
public interface FileGovernanceService {

    /** 按前缀比对对象存储与登记表，只读。 */
    FileOrphanScanRespVO scan(FileOrphanScanReqVO req);

    /** 把存储中存在但未登记的对象补录为 system 资源，默认 dryRun。 */
    FileOrphanBackfillRespVO backfill(FileOrphanBackfillReqVO req);

    /** 引用为 0 且超过 N 天未使用的文件分页。 */
    IPage<FileInfo> pageUnreferenced(FileUnreferencedReqVO req);

    /** 批量清理：引用中的文件一律跳过，默认 dryRun。 */
    FileBatchRespVO batch(FileBatchReqVO req);
}
