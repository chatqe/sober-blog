package io.charqe.qs.modules.file.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.file.controller.admin.vo.FileDeleteReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileListReqVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.service.FileInfoService;
import io.charqe.qs.modules.user.auth.SessionScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件回收站：软删除的文件在此检索与恢复，永久删除由 {@code /file/admin/delete?force=true} 执行。
 *
 * @author Joy
 * @since 2026-10-26
 */
@RestController
@RequestMapping("/file/recycle")
public class FileRecycleController {

    @Resource
    private FileInfoService fileInfoService;

    /**
     * 回收站列表：返回 {@code {list,total,page,pageSize}}，按进入回收站时间倒序。
     */
    @PostMapping("/list")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Map<String, Object>> list(@RequestBody FileListReqVO req) {
        req.normalize();
        IPage<FileInfo> page = fileInfoService.pageDeleted(req);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", page.getRecords());
        data.put("total", page.getTotal());
        data.put("page", page.getCurrent());
        data.put("pageSize", page.getSize());
        return R.success(data);
    }

    /**
     * 恢复文件：同时兼容 JSON 体 {@code {id}} 与 query 参数 {@code ?id=}。
     */
    @PostMapping("/restore")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R restore(@RequestParam(value = "id", required = false) Long id,
                     @RequestBody(required = false) FileDeleteReqVO body) {
        Long target = id != null ? id : (body == null ? null : body.getId());
        if (target == null) {
            return R.fail("文件ID不能为空");
        }
        if (fileInfoService.findAny(target) == null) {
            return R.fail(404, "文件不存在");
        }
        fileInfoService.restore(target);
        return R.success();
    }
}
