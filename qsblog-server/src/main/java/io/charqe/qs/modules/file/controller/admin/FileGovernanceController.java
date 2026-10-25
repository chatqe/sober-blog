package io.charqe.qs.modules.file.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.file.controller.admin.vo.FileBatchReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileBatchRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanBackfillReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanBackfillRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanScanReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanScanRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileUnreferencedReqVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.service.FileGovernanceService;
import io.charqe.qs.modules.user.auth.SessionScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件治理入口：孤儿扫描/补登记、未引用清单、批量清理，全部限管理员。
 *
 * @author Joy
 * @since 2026-10-26
 */
@RestController
@RequestMapping("/file")
public class FileGovernanceController {

    @Resource
    private FileGovernanceService fileGovernanceService;

    /**
     * 孤儿扫描：按前缀列举对象存储并与登记表比对，只读。
     */
    @PostMapping("/orphan/scan")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<FileOrphanScanRespVO> scan(@RequestBody(required = false) FileOrphanScanReqVO req) {
        return R.success(fileGovernanceService.scan(req));
    }

    /**
     * 补登记：把存储中存在但未登记的对象补录为 system 资源，默认 dryRun。
     */
    @PostMapping("/orphan/backfill")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<FileOrphanBackfillRespVO> backfill(@Valid @RequestBody FileOrphanBackfillReqVO req) {
        return R.success("补登记完成", fileGovernanceService.backfill(req));
    }

    /**
     * 未引用清单：{@code ref_count=0} 且超过 N 天未引用，返回 {@code {list,total,page,pageSize,days}}。
     */
    @PostMapping("/unreferenced/list")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Map<String, Object>> listUnreferenced(@RequestBody(required = false) FileUnreferencedReqVO req) {
        FileUnreferencedReqVO query = req == null ? new FileUnreferencedReqVO() : req;
        query.normalize();
        IPage<FileInfo> page = fileGovernanceService.pageUnreferenced(query);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", page.getRecords());
        data.put("total", page.getTotal());
        data.put("page", page.getCurrent());
        data.put("pageSize", page.getSize());
        data.put("days", query.getDays());
        return R.success(data);
    }

    /**
     * 批量清理：单次 ≤100，默认 dryRun，逐条返回 {@code {id,ok,skipped,reason,action}}。
     */
    @PostMapping("/batch")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<FileBatchRespVO> batch(@Valid @RequestBody FileBatchReqVO req) {
        return R.success("批量操作完成", fileGovernanceService.batch(req));
    }
}
