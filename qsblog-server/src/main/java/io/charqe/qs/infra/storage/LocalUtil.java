package io.charqe.qs.infra.storage;

import cn.hutool.core.io.FileUtil;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.FileVO;
import io.charqe.qs.utils.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "local.enable", havingValue = "true")
public class LocalUtil implements StoreService {

    @Value("${local.uploadUrl}")
    private String uploadUrl;

    @Value("${local.downloadUrl}")
    private String downloadUrl;

    @Resource
    private ImageSanitizer imageSanitizer;

    

    @Override
    public void deleteFile(List<String> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }

        for (String filePath : files) {
            File file = new File(filePath.replace(downloadUrl, uploadUrl));
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    log.info("文件删除成功：" + filePath);
                } else {
                    log.error("文件删除失败：" + filePath);
                }
            } else {
                log.error("文件不存在或者不是一个文件：" + filePath);
            }
        }
    }

    @Override
    public FileVO saveFile(FileVO fileVO) {
        if (fileVO == null || fileVO.getFile() == null) {
            throw new BizException("文件不能为空！");
        }
        // 前端按服务端 Key 拆出目录与文件名，目录可带尾部斜杠
        String directory = normalizeDirectory(fileVO.getRelativePath());

        byte[] raw;
        try {
            raw = fileVO.getFile().getBytes();
        } catch (IOException e) {
            throw new BizException("文件读取失败！");
        }
        String originalName = StringUtils.hasText(fileVO.getOriginalName())
                ? fileVO.getOriginalName() : fileVO.getFile().getOriginalFilename();
        // 服务端重绘：剥离 EXIF/GPS、纠正方向并限制像素，失败直接拒绝上传
        SanitizedImage sanitized = imageSanitizer.sanitize(raw, originalName,
                fileVO.getFile().getContentType());
        String name = sanitized.getFilename();
        if (!StrUtil.isValidFileName(name)) {
            throw new BizException("文件路径不合法！");
        }
        String relative = directory.isEmpty() ? name : directory + "/" + name;
        String absolutePath = join(uploadUrl, relative);
        if (FileUtil.exist(absolutePath)) {
            throw new BizException("文件已存在！");
        }
        try {
            FileUtil.writeBytes(sanitized.getBytes(), absolutePath);
            FileVO result = new FileVO();
            result.setAbsolutePath(absolutePath);
            result.setOriginalName(name);
            result.setVisitPath(join(downloadUrl, relative));
            log.info("文件写入成功：path={}，重绘={}", relative, sanitized.isRewritten());
            return result;
        } catch (Exception e) {
            log.error("文件上传失败：", e);
            FileUtil.del(absolutePath);
            throw new BizException("文件上传失败！");
        }
    }

    /** 目录归一：去首尾斜杠后逐段校验，拦下穿越与非法字符。 */
    private String normalizeDirectory(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            throw new BizException("文件路径不合法！");
        }
        String path = rawPath.trim();
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!StringUtils.hasText(path) || path.contains("..")) {
            throw new BizException("文件路径不合法！");
        }
        String[] segments = path.split("/");
        if (segments.length > 5) {
            throw new BizException("文件路径不合法！");
        }
        for (String segment : segments) {
            if (!StrUtil.isValidDirectoryName(segment)) {
                throw new BizException("文件路径不合法！");
            }
        }
        return path;
    }

    /** 拼接磁盘路径或下载地址，只补一个分隔符。 */
    private String join(String prefix, String relative) {
        if (!StringUtils.hasText(prefix)) {
            return relative;
        }
        return prefix.endsWith("/") ? prefix + relative : prefix + "/" + relative;
    }

    @Override
    public String getStoreName() {
        return StoreEnum.LOCAL.getCode();
    }
}


