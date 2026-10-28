package io.charqe.qs.modules.file.service;

import io.charqe.qs.modules.file.controller.admin.vo.FileDetailRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileVariantsVO;
import io.charqe.qs.modules.file.convert.FileConvert;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.enums.FileVariantEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Locale;

/**
 * 媒体库读取实现：只推导访问地址与派生地址，不写库、不落派生文件。
 * <p>
 * 七牛场景用 {@code imageView2} 的 URL 变换生成三档变体（零额外存储）；
 * 本地存储没有在线变换能力，降级为原图并由 {@code variantMode=none} 明示。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Service
public class FileMediaServiceImpl implements FileMediaService {

    /** 七牛 imageView2 的默认质量。 */
    private static final String VARIANT_QUALITY = "85";

    /** 派生策略取值。 */
    private static final String MODE_TRANSFORM = "transform";
    private static final String MODE_NONE = "none";

    /** 七牛存储类型。 */
    private static final Integer QINIU = 3;

    /** 本地存储类型。 */
    private static final Integer LOCAL = 1;

    @Resource
    private FileInfoService fileInfoService;

    @Value("${qiniu.enable:false}")
    private boolean qiniuEnabled;

    @Value("${qiniu.downloadUrl:}")
    private String qiniuDownloadUrl;

    @Value("${local.downloadUrl:}")
    private String localDownloadUrl;

    @Override
    public FileDetailRespVO detail(Long id) {
        FileInfo file = fileInfoService.findAny(id);
        // 回收站对象不对外暴露详情，避免详情绕过回收站恢复语义
        if (file == null || Integer.valueOf(1).equals(file.getIsDel())) {
            return null;
        }
        FileDetailRespVO vo = FileConvert.INSTANCE.convertDetail(file);
        vo.setVisitPath(visitPath(file));
        vo.setEtag(StringUtils.hasText(file.getFileHash())
                ? file.getFileHash().toLowerCase(Locale.ROOT) : null);
        if (transformable(file)) {
            vo.setVariantMode(MODE_TRANSFORM);
            vo.setVariants(variants(vo.getVisitPath()));
        } else {
            vo.setVariantMode(MODE_NONE);
            vo.setVariants(null);
        }
        return vo;
    }

    /** 自定义 CDN 域名优先，其次按存储类型取默认下载域名。 */
    private String visitPath(FileInfo file) {
        String prefix = domainPrefix(file);
        String path = StringUtils.hasText(file.getStoragePath()) ? file.getStoragePath().trim() : "";
        return prefix.isEmpty() ? path : prefix + stripLeadingSlash(path);
    }

    private String domainPrefix(FileInfo file) {
        String custom = asPrefix(file.getCdnDomain());
        if (!custom.isEmpty()) {
            return custom;
        }
        if (QINIU.equals(file.getThirdPartyName())) {
            return asPrefix(qiniuDownloadUrl);
        }
        if (LOCAL.equals(file.getThirdPartyName())) {
            return asPrefix(localDownloadUrl);
        }
        return "";
    }

    /** 前缀统一补齐末尾斜杠，避免域名与 key 粘连。 */
    private String asPrefix(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        String value = url.trim();
        return value.endsWith("/") ? value : value + "/";
    }

    private String stripLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    /** 只有七牛上的静态图片可做在线变换；gif 会被转成静态帧，故排除。 */
    private boolean transformable(FileInfo file) {
        if (!qiniuEnabled || !QINIU.equals(file.getThirdPartyName())) {
            return false;
        }
        String mime = file.getMimeType() == null ? "" : file.getMimeType().toLowerCase(Locale.ROOT);
        return mime.startsWith("image/") && !"image/gif".equals(mime);
    }

    private FileVariantsVO variants(String visitPath) {
        FileVariantsVO vo = new FileVariantsVO();
        vo.setThumb(variantUrl(visitPath, FileVariantEnum.THUMB));
        vo.setCard(variantUrl(visitPath, FileVariantEnum.CARD));
        vo.setHero(variantUrl(visitPath, FileVariantEnum.HERO));
        return vo;
    }

    private String variantUrl(String base, FileVariantEnum variant) {
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "imageView2/2/w/" + variant.getWidth() + "/q/" + VARIANT_QUALITY;
    }
}
