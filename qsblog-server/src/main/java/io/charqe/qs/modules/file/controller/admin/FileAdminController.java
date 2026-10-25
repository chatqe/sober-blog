package io.charqe.qs.modules.file.controller.admin;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.FileVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.infra.storage.StoreService;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileDeleteReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileListReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.service.FileInfoService;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.modules.user.domain.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Slf4j
@RestController
@RequestMapping("/file/admin")
public class FileAdminController {

    @Resource
    private FileInfoService fileInfoService;

    @Resource(name = "storeFileService")
    private io.charqe.qs.infra.storage.FileStorageService storeService;

    @Resource
    private AuthSessionService sessions;

    @Value("${qiniu.downloadUrl}")
    private String downloadUrl;

    /**
     * 保存（管理端）
     */
    @PostMapping("/save")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<FileRespVO> save(@Valid @RequestBody FileCreateReqVO vo) {
        User user = sessions.current(SessionScope.ADMIN, false).user();
        return R.success("新增成功", fileInfoService.register(vo, user.getId(), "admin"));
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<String> upload(@RequestParam("file") MultipartFile file, FileVO fileVO) {

        log.info("========================> /resource/upload 接口调用");

        if (file == null || !StringUtils.hasText(fileVO.getType()) || !StringUtils.hasText(fileVO.getRelativePath())) {
            return R.fail("文件和资源类型和资源路径不能为空！");
        }
        fileVO.setFile(file);
        StoreService storageService = storeService.getFileStorage(fileVO.getStoreType());
        FileVO result = storageService.saveFile(fileVO);

        return R.success(result.getVisitPath());
    }

    /**
     * 删除文件：同时兼容 JSON 体 {@code {id}} 与 query 参数 {@code ?id=}。
     */
    @PostMapping("/delete")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R delete(@RequestParam(value = "id", required = false) Long id,
                    @RequestBody(required = false) FileDeleteReqVO body) {
        Long target = id != null ? id : (body == null ? null : body.getId());
        if (target == null) {
            return R.fail("文件ID不能为空");
        }
        fileInfoService.removeById(target);
        return R.success();
    }

    /**
     * 查询文件列表：返回 {@code {list,total,page,pageSize}}，与前端消费口径一致。
     */
    @PostMapping("/list")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Map<String, Object>> list(@RequestBody FileListReqVO req) {
        req.normalize();
        Page<FileInfo> pageReq = new Page<>(Math.max(req.getCurrent(), 1), Math.max(req.getSize(), 1));
        IPage<FileInfo> page = fileInfoService.page(pageReq, filter(req));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", page.getRecords());
        data.put("total", page.getTotal());
        data.put("page", page.getCurrent());
        data.put("pageSize", page.getSize());
        return R.success(data);
    }

    private Wrapper<FileInfo> filter(FileListReqVO req) {
        return Wrappers.<FileInfo>lambdaQuery()
                .eq(StringUtils.hasText(req.getScene()), FileInfo::getScene, req.getScene())
                .like(StringUtils.hasText(req.getKeyword()), FileInfo::getOriginalName, req.getKeyword())
                .orderByDesc(FileInfo::getCreateTime);
    }

}
