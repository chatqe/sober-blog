package io.charqe.qs.modules.file.service;

import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.infra.storage.QiniuUtil;
import io.charqe.qs.modules.file.controller.web.vo.FileConfigVO;
import io.charqe.qs.modules.file.controller.web.vo.FileUploadTokenReqVO;
import io.charqe.qs.modules.file.controller.web.vo.FileUploadTokenRespVO;
import io.charqe.qs.modules.file.enums.FileSceneEnum;
import io.charqe.qs.modules.file.support.FileKeyGenerator;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 上传策略实现：Key 与凭证只在服务端产生，前端不能自造路径。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {

    /** 允许直传的 MIME 白名单：拒绝 html/svg 等可执行内容落到 CDN。 */
    private static final Pattern MIME_WHITELIST = Pattern.compile(
            "^(image/(png|jpeg|gif|webp|avif|bmp)"
                    + "|video/(mp4|webm|quicktime)"
                    + "|audio/(mpeg|wav|ogg|mp4)"
                    + "|application/pdf|text/plain)$");

    /** 上传成功后回传对象元数据，便于前端核对实际落库结果。 */
    private static final String RETURN_BODY =
            "{\"key\":\"$(key)\",\"hash\":\"$(etag)\",\"fsize\":$(fsize),\"mimeType\":\"$(mimeType)\"}";

    private static final String HASH_ALGO = "sha256";

    @Resource
    private FileKeyGenerator keyGenerator;

    @Resource
    private ObjectProvider<QiniuUtil> qiniuProvider;

    @Value("${store.type:local}")
    private String storeType;

    @Value("${qiniu.enable:false}")
    private boolean qiniuEnabled;

    @Value("${qiniu.uploadUrl:}")
    private String qiniuUploadUrl;

    @Value("${qiniu.downloadUrl:}")
    private String qiniuDownloadUrl;

    @Value("${local.uploadUrl:}")
    private String localUploadUrl;

    @Value("${local.downloadUrl:}")
    private String localDownloadUrl;

    /** 单次上传上限（字节），默认 20MB。 */
    @Value("${upload.fsize-limit:20971520}")
    private long fsizeLimit;

    /** 上传凭证有效期（秒）。 */
    @Value("${upload.token-ttl:3600}")
    private long tokenTtl;

    @Override
    public FileUploadTokenRespVO token(FileUploadTokenReqVO req, User user) {
        FileSceneEnum scene = FileSceneEnum.of(req.getScene());
        if (scene == null || scene == FileSceneEnum.UNKNOWN) {
            throw new BizException("不支持的业务场景：" + req.getScene());
        }
        if (!isManager(user) && !scene.isClientUploadable()) {
            throw new BizException("该场景不允许当前账号上传");
        }
        if (req.getFileSize() > fsizeLimit) {
            throw new BizException("文件大小超过 " + (fsizeLimit / 1024 / 1024) + "MB 限制");
        }
        String mime = req.getMimeType().trim().toLowerCase(Locale.ROOT);
        if (!MIME_WHITELIST.matcher(mime).matches()) {
            throw new BizException("不支持的文件类型：" + mime);
        }

        String key = keyGenerator.build(scene.getCode(), user.getId(), req.getOriginalName(), mime);
        boolean qiniuStore = qiniuStore();
        String token = null;
        if (qiniuStore) {
            QiniuUtil qiniu = qiniuProvider.getIfAvailable();
            if (qiniu == null) {
                throw new BizException("对象存储未启用，请联系管理员");
            }
            token = qiniu.getToken(key, fsizeLimit, mime, true, tokenTtl, RETURN_BODY);
        }

        return FileUploadTokenRespVO.builder()
                .storeType(qiniuStore ? "qiniu" : "local")
                .token(token)
                .key(key)
                .uploadUrl(qiniuStore ? qiniuUploadUrl : localUploadUrl)
                .downloadUrl(qiniuStore ? prefix(qiniuDownloadUrl) : prefix(localDownloadUrl))
                .fsizeLimit(fsizeLimit)
                .mimeLimit(mime)
                .insertOnly(1)
                .hashAlgo(HASH_ALGO)
                .expiresAt(System.currentTimeMillis() / 1000 + tokenTtl)
                .build();
    }

    @Override
    public FileConfigVO config() {
        boolean qiniuStore = qiniuStore();
        return FileConfigVO.builder()
                .storeType(qiniuStore ? "qiniu" : "local")
                .qiniuEnabled(qiniuEnabled)
                .uploadUrl(qiniuStore ? qiniuUploadUrl : localUploadUrl)
                .downloadUrl(qiniuStore ? prefix(qiniuDownloadUrl) : prefix(localDownloadUrl))
                .fsizeLimit(fsizeLimit)
                .clientScenes(FileSceneEnum.clientCodes())
                .hashAlgo(HASH_ALGO)
                .tokenTtl(tokenTtl)
                .build();
    }

    private boolean qiniuStore() {
        return "qiniu".equalsIgnoreCase(storeType) && qiniuEnabled;
    }

    private boolean isManager(User user) {
        return user.getUserType() != null && user.getUserType() <= 1;
    }

    /** 访问前缀统一带末尾斜杠，前端拼接时无需再做归一化。 */
    private String prefix(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        return url.endsWith("/") ? url : url + "/";
    }
}
