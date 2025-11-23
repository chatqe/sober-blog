package com.joy.qs.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joy.qs.aop.LoginCheck;
import com.joy.qs.common.R;
import com.joy.qs.enums.YouthEnum;
import com.joy.qs.model.BaseReqVO;
import com.joy.qs.model.FileVO;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.model.resource.pojo.Resource;
import com.joy.qs.service.ResourceService;
import com.joy.qs.utils.PoetryUtil;
import com.joy.qs.utils.storage.FileStorageService;
import com.joy.qs.utils.storage.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 资源信息 前端控制器
 * </p>
 *
 * @author sara
 * @since 2022-03-06
 */
@Slf4j
@RestController
@RequestMapping("/resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private FileStorageService fileStorageService;
    @Value("${qiniu.downloadUrl}")
    private String downloadUrl;

    /**
     * 保存
     */
    @PostMapping("/saveResource")
    @LoginCheck
    public R saveResource(@RequestBody Resource resource) {
        if (!StringUtils.hasText(resource.getType()) || !StringUtils.hasText(resource.getPath())) {
            return R.fail("资源类型和资源路径不能为空！");
        }
        Resource re = new Resource();
        re.setPath(resource.getPath());
        re.setType(resource.getType());
        re.setSize(resource.getSize());
        re.setOriginalName(resource.getOriginalName());
        re.setMimeType(resource.getMimeType());
        re.setStoreType(resource.getStoreType());
        re.setUserId(PoetryUtil.getUserId());
        resourceService.save(re);
        return R.success();
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
        StoreService storeService = fileStorageService.getFileStorage(fileVO.getStoreType());
        FileVO result = storeService.saveFile(fileVO);

        Resource re = new Resource();
        re.setPath(result.getVisitPath());
        re.setType(fileVO.getType());
        re.setSize(Integer.valueOf(Long.toString(file.getSize())));
        re.setMimeType(file.getContentType());
        re.setStoreType(fileVO.getStoreType());
        re.setOriginalName(fileVO.getOriginalName());
        re.setUserId(PoetryUtil.getUserId());
        resourceService.save(re);
        return R.success(result.getVisitPath());
    }

    /**
     * 删除
     */
    @PostMapping("/deleteResource")
    @LoginCheck(0)
    public R deleteResource(@RequestParam("path") String path) {
        Resource resource = resourceService.lambdaQuery().select(Resource::getStoreType).eq(Resource::getPath, path).one();
        if (resource == null) {
            return R.fail("文件不存在：" + path);
        }

        StoreService storeService = fileStorageService.getFileStorageByStoreType(resource.getStoreType());
        String replace = path.replace(downloadUrl, "");
        storeService.deleteFile(Collections.singletonList(replace));
        return R.success();
    }

    /**
     * 查询表情包
     */
    @GetMapping("/getImageList")
    @LoginCheck
    public R<List<String>> getImageList() {
        List<Resource> list = resourceService.lambdaQuery().select(Resource::getPath)
                .eq(Resource::getType, CommonConst.PATH_TYPE_INTERNET_MEME)
                .eq(Resource::getStatus, YouthEnum.STATUS_ENABLE.getCode())
                .eq(Resource::getUserId, PoetryUtil.getAdminUser().getId())
                .orderByDesc(Resource::getCreateTime)
                .list();
        List<String> paths = list.stream().map(Resource::getPath).collect(Collectors.toList());
        return R.success(paths);
    }

    /**
     * 查询资源
     */
    @PostMapping("/listResource")
    @LoginCheck(0)
    public R<Page> listResource(@RequestBody BaseReqVO baseReqVO) {
        resourceService.lambdaQuery()
                .eq(StringUtils.hasText(baseReqVO.getResourceType()), Resource::getType, baseReqVO.getResourceType())
                .orderByDesc(Resource::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }

    /**
     * 修改资源状态
     */
    @GetMapping("/changeResourceStatus")
    @LoginCheck(0)
    public R changeResourceStatus(@RequestParam("id") Integer id, @RequestParam("flag") Boolean flag) {
        resourceService.lambdaUpdate().eq(Resource::getId, id).set(Resource::getStatus, flag).update();
        return R.success();
    }
}

