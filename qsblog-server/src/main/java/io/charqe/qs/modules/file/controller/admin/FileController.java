package io.charqe.qs.modules.file.controller.admin;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.FileVO;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.StatusEnum;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.infra.storage.StoreService;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.service.FileStorageService;
import io.charqe.qs.utils.BlogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {


    @Resource
    private io.charqe.qs.modules.file.service.FileStorageService fileStorageService;

    @Resource(name = "storeFileService")
    private io.charqe.qs.infra.storage.FileStorageService storeService;

    @Value("${qiniu.downloadUrl}")
    private String downloadUrl;

    /**
     * 保存
     */
    @PostMapping("/save")
    @LoginCheck
    public R save(@RequestBody FileCreateReqVO vo) {
        if (!StringUtils.hasText(vo.getFileType()) || !StringUtils.hasText(vo.getFilePath())) {
            return R.fail("资源类型和资源路径不能为空！");
        }
        return R.success("新增成功", fileStorageService.save(vo));
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @LoginCheck
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
     * 删除文件
     */
    @PostMapping("/delete")
    @LoginCheck
    public R delete(@RequestParam("id") Long id) {
        fileStorageService.removeById(id);
        return R.success();
    }

    /**
     * 查询文件列表
     */
    @PostMapping("/list")
    @LoginCheck
    public R list(@RequestBody BaseReqVO baseReqVO) {
        fileStorageService.page(baseReqVO, null);
        return R.success(baseReqVO);
    }
}



