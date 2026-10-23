package io.charqe.qs.infra.storage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.processing.OperationManager;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.BatchStatus;
import com.qiniu.storage.model.BucketInfo;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.qiniu.util.UrlSafeBase64;
import io.charqe.qs.common.FileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 七牛云工具类（Kodo）
 *
 * @author Joy
 * @since 2026-01-02 19:45
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "qiniu.enable", havingValue = "true")
public class QiniuUtil implements StoreService {

    /* ====================== 配置 ====================== */

    @Value("${qiniu.accessKey}")
    private String accessKey;

    @Value("${qiniu.secretKey}")
    private String secretKey;

    @Value("${qiniu.bucket}")
    private String bucket;

    @Value("${qiniu.downloadUrl}")
    private String downloadUrl;

    @Value("${qiniu.pipeline}")
    private String pipeline;

    /** 上传凭证有效期（秒） */
    private static final long EXPIRE_SECONDS = 60L;

    /** 单次上传大小限制（字节），20MB */
    private static final long F_SIZE_LIMIT = 20971520L;

    /** 七牛批量操作单次上限 */
    private static final int BATCH_LIMIT = 1000;

    private volatile Configuration configuration;
    private volatile Auth auth;

    /* ====================== 基础组件 ====================== */

    private Configuration getConfiguration() {
        if (configuration == null) {
            synchronized (this) {
                if (configuration == null) {
                    configuration = new Configuration(Region.region0());
                }
            }
        }
        return configuration;
    }

    private Auth getAuth() {
        if (auth == null) {
            synchronized (this) {
                if (auth == null) {
                    auth = Auth.create(accessKey, secretKey);
                }
            }
        }
        return auth;
    }

    private BucketManager getBucketManager() {
        return new BucketManager(getAuth(), getConfiguration());
    }

    /**
     * 归一化 key：去掉 downloadUrl 前缀，保留裸 key。
     * 用于 deleteFile / getFileInfo 等接收"完整 URL 或裸 key"混用的场景。
     */
    private String normalizeKey(String raw) {
        if (raw == null) {
            return null;
        }
        if (StrUtil.isNotBlank(downloadUrl) && raw.startsWith(downloadUrl)) {
            return raw.substring(downloadUrl.length());
        }
        return raw;
    }

    private List<String> normalizeKeys(Collection<String> raw) {
        if (CollUtil.isEmpty(raw)) {
            return Collections.emptyList();
        }
        return raw.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeKey)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    /** 列表分批，用于超过 BATCH_LIMIT 的批量操作 */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }

    /* ====================== 上传凭证 ====================== */

    /**
     * 生成上传凭证。
     *
     * @param key 目标文件名；传 null 表示允许任意 key
     */
    public String getToken(String key) {
        StringMap putPolicy = new StringMap();
        putPolicy.put("fsizeLimit", F_SIZE_LIMIT);
        return getAuth().uploadToken(bucket, key, EXPIRE_SECONDS, putPolicy);
    }

    /* ====================== 上传 ====================== */

    @Override
    public FileVO saveFile(FileVO fileVO) {
        if (fileVO == null || fileVO.getFile() == null) {
            throw new IllegalArgumentException("FileVO 或 file 不能为空");
        }
        // 构造裸 key：相对路径 + 原始文件名
        String key = buildUploadKey(fileVO);
        String upToken = getToken(key);
        UploadManager uploadManager = new UploadManager(getConfiguration());

        try {
            byte[] bytes = fileVO.getFile().getBytes();
            Response response = uploadManager.put(bytes, key, upToken);
            if (response.isOK()) {
                fileVO.setVisitPath(downloadUrl + key);
                log.info("文件上传成功：key={}", key);
            } else {
                log.error("文件上传失败：key={}，响应：{}", key, response.bodyString());
                throw new RuntimeException("文件上传失败：" + response.bodyString());
            }
        } catch (QiniuException e) {
            log.error("文件上传异常：key={}，code={}，error={}",
                    key, e.code(), e.response == null ? "" : e.response.toString());
            throw new RuntimeException("文件上传失败", e);
        } catch (Exception e) {
            log.error("文件读取异常：key={}", key, e);
            throw new RuntimeException("文件上传失败", e);
        }
        return fileVO;
    }

    /** 从 FileVO 构造七牛裸 key */
    private String buildUploadKey(FileVO fileVO) {
        String rel = StrUtil.isNotBlank(fileVO.getRelativePath()) ? fileVO.getRelativePath() : "";
        String orig = StrUtil.isNotBlank(fileVO.getOriginalName())
                ? fileVO.getOriginalName()
                : "upload_" + System.currentTimeMillis();
        return rel.startsWith("/") ? rel + orig : rel + "/" + orig;
    }

    /* ====================== 删除 ====================== */

    /**
     * 批量删除文件（支持传入完整 URL 或裸 key）。
     * 单次超过 1000 个会自动分批。
     *
     * @return 失败明细 Map&lt;key, 错误信息&gt;，全部成功则为空 Map
     */
    @Override
    public void deleteFile(List<String> files) {
        batchDeleteFiles(files);
    }

    /** 保留旧签名，内部委托 deleteFile */
    public void batchDel(List<String> files) {
        batchDeleteFiles(files);
    }

    /**
     * 批量删除，返回失败明细。
     *
     * @return Map&lt;key, 错误信息&gt;
     */
    public Map<String, String> batchDeleteFiles(List<String> files) {
        Map<String, String> failed = new HashMap<>();
        List<String> keys = normalizeKeys(files);
        if (CollUtil.isEmpty(keys)) {
            return failed;
        }

        BucketManager bucketManager = getBucketManager();
        for (List<String> part : partition(keys, BATCH_LIMIT)) {
            try {
                BucketManager.BatchOperations ops = new BucketManager.BatchOperations();
                ops.addDeleteOp(bucket, part.toArray(new String[0]));
                Response response = bucketManager.batch(ops);
                BatchStatus[] statuses = response.jsonToObject(BatchStatus[].class);

                for (int i = 0; i < part.size(); i++) {
                    String key = part.get(i);
                    BatchStatus status = statuses[i];
                    if (status.code == 200) {
                        log.info("文件删除成功：{}", key);
                    } else {
                        String err = status.data == null ? "unknown" : status.data.error;
                        log.error("文件删除失败：{}，原因：{}", key, err);
                        failed.put(key, err);
                    }
                }
            } catch (QiniuException e) {
                log.error("批量删除请求异常：{}",
                        e.response == null ? e.getMessage() : e.response.toString());
                part.forEach(k -> failed.put(k, e.getMessage()));
            }
        }
        return failed;
    }

    /* ====================== 移动 / 复制 ====================== */

    /**
     * 移动（或重命名）单个文件。
     * 源 bucket 与目标 bucket 必须属于同一账号，且同区域。
     *
     * @param srcKey  源裸 key（bucket 内路径）
     * @param destKey 目标裸 key（bucket 内路径）
     * @param force   目标已存在时是否强制覆盖
     */
    public void moveFile(String srcKey, String destKey) {
        requireNonBlank(srcKey, destKey);
        try {
            getBucketManager().move(bucket, srcKey, bucket, destKey);
            log.info("文件移动成功：{} -> {}", srcKey, destKey);
        } catch (QiniuException e) {
            log.error("文件移动失败：{} -> {}，code={}，error={}",
                    srcKey, destKey, e.code(), e.response == null ? "" : e.response.toString());
            throw new RuntimeException("文件移动失败", e);
        }
    }

    /**
     * 复制单个文件（同 bucket 内）。
     *
     * @param srcKey  源裸 key
     * @param destKey 目标裸 key
     */
    public void copyFile(String srcKey, String destKey) {
        requireNonBlank(srcKey, destKey);
        try {
            getBucketManager().copy(bucket, srcKey, bucket, destKey);
            log.info("文件复制成功：{} -> {}", srcKey, destKey);
        } catch (QiniuException e) {
            log.error("文件复制失败：{} -> {}，code={}，error={}",
                    srcKey, destKey, e.code(), e.response == null ? "" : e.response.toString());
            throw new RuntimeException("文件复制失败", e);
        }
    }

    /**
     * 批量移动文件。
     *
     * @param srcKeys  源文件裸 key 列表
     * @param destKeys 目标文件裸 key 列表（与 srcKeys 一一对应）
     * @return Map&lt;源key, 错误信息&gt;，空 Map 表示全部成功
     */
    public Map<String, String> batchMoveFiles(List<String> srcKeys, List<String> destKeys) {
        return batchTransfer(srcKeys, destKeys, true);
    }

    /**
     * 批量复制文件（同 bucket 内）。
     *
     * @param srcKeys  源文件裸 key 列表
     * @param destKeys 目标文件裸 key 列表（与 srcKeys 一一对应）
     * @return Map&lt;源key, 错误信息&gt;，空 Map 表示全部成功
     */
    public Map<String, String> batchCopyFiles(List<String> srcKeys, List<String> destKeys) {
        return batchTransfer(srcKeys, destKeys, false);
    }

    /**
     * 一次性复制整个目录：将 srcPrefix 下所有文件复制到 destPrefix 下，
     * 文件名保持不变。空文件列表直接返回，不抛异常。
     *
     * @param srcPrefix  源目录前缀，例如 "a/"
     * @param destPrefix 目标目录前缀，例如 "b/"
     * @return Map&lt;源key, 错误信息&gt;，空 Map 表示全部成功
     */
    public Map<String, String> batchCopyDir(String srcPrefix, String destPrefix) {
        if (StrUtil.isBlank(srcPrefix) || StrUtil.isBlank(destPrefix)) {
            throw new IllegalArgumentException("srcPrefix / destPrefix 不能为空");
        }
        List<String> srcKeys = list(srcPrefix);
        if (CollUtil.isEmpty(srcKeys)) {
            log.info("目录 {} 下无文件，跳过复制", srcPrefix);
            return Collections.emptyMap();
        }
        // list() 返回的是带域名的完整 URL，需要转为裸 key
        List<String> bareKeys = srcKeys.stream()
                .map(this::normalizeKey)
                .collect(Collectors.toList());
        List<String> destKeys = bareKeys.stream()
                .map(key -> destPrefix + key.substring(srcPrefix.length()))
                .collect(Collectors.toList());
        return batchCopyFiles(bareKeys, destKeys);
    }

    /**
     * 批量移动 / 复制的公共实现。
     *
     * @param isMove true=move，false=copy
     */
    private Map<String, String> batchTransfer(List<String> srcKeys,
                                              List<String> destKeys,
                                              boolean isMove) {
        Map<String, String> failed = new HashMap<>();
        if (CollUtil.isEmpty(srcKeys) || CollUtil.isEmpty(destKeys)) {
            return failed;
        }
        if (srcKeys.size() != destKeys.size()) {
            throw new IllegalArgumentException(
                    "源文件列表和目标文件列表长度不一致：" + srcKeys.size() + " vs " + destKeys.size());
        }

        List<String> normSrc = normalizeKeys(srcKeys);
        // destKeys 由调用方显式传入，不做 normalize（若误传域名会暴露问题）
        List<String> normDest = destKeys;

        BucketManager bucketManager = getBucketManager();
        String opName = isMove ? "移动" : "复制";

        int total = normSrc.size();
        for (int start = 0; start < total; start += BATCH_LIMIT) {
            int end = Math.min(start + BATCH_LIMIT, total);
            List<String> partSrc = normSrc.subList(start, end);
            List<String> partDest = normDest.subList(start, end);

            try {
                BucketManager.BatchOperations ops = new BucketManager.BatchOperations();
                for (int i = 0; i < partSrc.size(); i++) {
                    if (isMove) {
                        ops.addMoveOp(bucket, partSrc.get(i), bucket, partDest.get(i));
                    } else {
                        ops.addCopyOp(bucket, partSrc.get(i), bucket, partDest.get(i));
                    }
                }
                Response response = bucketManager.batch(ops);
                BatchStatus[] statuses = response.jsonToObject(BatchStatus[].class);

                for (int i = 0; i < partSrc.size(); i++) {
                    String src = partSrc.get(i);
                    String dest = partDest.get(i);
                    BatchStatus status = statuses[i];
                    if (status.code == 200) {
                        log.info("文件{}成功：{} -> {}", opName, src, dest);
                    } else {
                        String err = status.data == null ? "unknown" : status.data.error;
                        log.error("文件{}失败：{} -> {}，原因：{}", opName, src, dest, err);
                        failed.put(src, err);
                    }
                }
            } catch (QiniuException e) {
                log.error("批量{}请求异常：{}", opName,
                        e.response == null ? e.getMessage() : e.response.toString());
                partSrc.forEach(k -> failed.put(k, e.getMessage()));
            }
        }
        return failed;
    }

    /* ====================== 存储服务标识 ====================== */

    @Override
    public String getStoreName() {
        return StoreEnum.QINIU.getCode();
    }

    /* ====================== 查询 / 列举 ====================== */

    public void fileList() {
        BucketManager bucketManager = getBucketManager();
        try {
            FileListing fileListing =
                    bucketManager.listFilesV2(bucket, "/qingsi/static/avatar/", null, 100, null);
            log.info(fileListing.toString());
        } catch (QiniuException e) {
            throw new RuntimeException(e);
        }
    }

    public void getBucketInfo() {
        BucketManager bucketManager = getBucketManager();
        try {
            BucketInfo bucketInfo = bucketManager.getBucketInfo(bucket);
            log.info(bucketInfo.toString());
        } catch (QiniuException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 批量获取文件元信息（size / mimeType）。
     *
     * @return Map&lt;key, Map&lt;"size"/"mimeType"&gt;&gt;
     */
    public Map<String, Map<String, String>> getFileInfo(List<String> files) {
        Map<String, Map<String, String>> result = new HashMap<>();
        List<String> keys = normalizeKeys(files);
        if (CollUtil.isEmpty(keys)) {
            return result;
        }

        BucketManager bucketManager = getBucketManager();
        for (List<String> part : partition(keys, BATCH_LIMIT)) {
            try {
                BucketManager.BatchOperations ops = new BucketManager.BatchOperations();
                ops.addStatOps(bucket, part.toArray(new String[0]));
                Response response = bucketManager.batch(ops);
                BatchStatus[] statuses = response.jsonToObject(BatchStatus[].class);

                for (int i = 0; i < part.size(); i++) {
                    String key = part.get(i);
                    BatchStatus status = statuses[i];
                    if (status.code == 200) {
                        Map<String, String> info = new HashMap<>(4);
                        info.put("size", String.valueOf(status.data.fsize));
                        info.put("mimeType", status.data.mimeType);
                        result.put(key, info);
                    } else {
                        log.error("查询文件信息失败：{}，原因：{}", key,
                                status.data == null ? "unknown" : status.data.error);
                    }
                }
            } catch (QiniuException e) {
                log.error("批量查询文件信息异常：{}",
                        e.response == null ? e.getMessage() : e.response.toString());
            }
        }
        return result;
    }

    /**
     * 获取文件大小（字节）
     */
    private long getFileSize(String key) {
        Map<String, Map<String, String>> info = getFileInfo(Collections.singletonList(key));
        Map<String, String> meta = info.get(key);
        if (meta == null) {
            throw new IllegalArgumentException("文件不存在 key=" + key);
        }
        return Long.parseLong(meta.getOrDefault("size", "0"));
    }

    /**
     * 按前缀列举文件，返回带域名的完整 URL。
     */
    public List<String> list(String imgPrefix) {
        BucketManager bucketManager = getBucketManager();
        BucketManager.FileListIterator it =
                bucketManager.createFileListIterator(bucket, imgPrefix, 1000, "");

        List<String> urls = new ArrayList<>();
        while (it.hasNext()) {
            FileInfo[] items = it.next();
            if (items == null) break;
            for (FileInfo item : items) {
                String key = item.key;
                if (!key.equals(imgPrefix)) {
                    urls.add(downloadUrl.concat(key));
                }
            }
        }
        return urls;
    }

    /**
     * 前缀匹配 + 体积 ≥ 250KB + 后缀非 .webp，返回裸 key。
     */
    public List<String> listLargeNonWebp(String imgPrefix) {
        long minSize = 250L * 1024;
        BucketManager bucketManager = getBucketManager();
        BucketManager.FileListIterator it =
                bucketManager.createFileListIterator(bucket, imgPrefix, 1000, "");

        List<String> result = new ArrayList<>();
        while (it.hasNext()) {
            FileInfo[] items = it.next();
            if (items == null) break;
            for (FileInfo item : items) {
                String key = item.key;
                if (!key.equals(imgPrefix)
                        && item.fsize >= minSize
                        && !StrUtil.endWithIgnoreCase(key, ".webp")) {
                    result.add(key);
                }
            }
        }
        return result;
    }

    /**
     * 前缀匹配 + 体积 ≥ 250KB，返回裸 key。
     */
    public List<String> listNoDomain(String imgPrefix) {
        long minSize = 250L * 1024;
        BucketManager bucketManager = getBucketManager();
        BucketManager.FileListIterator it =
                bucketManager.createFileListIterator(bucket, imgPrefix, 1000, "");

        List<String> imgKeys = new ArrayList<>();
        while (it.hasNext()) {
            FileInfo[] items = it.next();
            if (items == null) break;
            for (FileInfo item : items) {
                String key = item.key;
                if (!key.equals(imgPrefix)
                        && item.fsize >= minSize) {
                    imgKeys.add(key);
                }
            }
        }
        return imgKeys;
    }

    /* ====================== 持久化转码（pfop） ====================== */

    /**
     * 提交单个持久化转码任务：jpg/jpeg/png → webp。
     *
     * @param srcKey  源裸 key
     * @param quality 质量 75~85
     * @return persistentId
     */
    public String submitPfop(String srcKey, int quality) {
        if (StrUtil.isBlank(srcKey)) {
            throw new IllegalArgumentException("srcKey 不能为空");
        }
        String low = srcKey.toLowerCase();
        if (!low.endsWith(".jpg") && !low.endsWith(".jpeg") && !low.endsWith(".png")) {
            throw new IllegalArgumentException("仅支持 jpg/jpeg/png 三种格式，当前 key=" + srcKey);
        }

        int lastDot = srcKey.lastIndexOf('.');
        String dstKey = srcKey.substring(0, lastDot) + ".webp";

        String fops = StrUtil.format(
                "imageMogr2/format/webp/quality/{}/strip/interlace/1", quality);
        String saveas = UrlSafeBase64.encodeToString(bucket + ":" + dstKey);
        String fopStr = fops + "|saveas/" + saveas;

        OperationManager op = new OperationManager(getAuth(), getConfiguration());
        try {
            return op.pfop(bucket, srcKey, fopStr, pipeline, true);
        } catch (QiniuException e) {
            log.error("提交 pfop 失败, srcKey={}, code={}, error={}",
                    srcKey, e.code(), e.response == null ? "" : e.response.toString());
            throw new RuntimeException("提交转码任务失败", e);
        }
    }

    /**
     * 批量提交 pfop（串行，避免单个失败导致全部丢失）。
     *
     * @return Map&lt;源key, persistentId&gt;
     */
    public Map<String, String> batchSubmitPfop(List<String> srcKeys, int quality) {
        if (CollUtil.isEmpty(srcKeys)) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : srcKeys) {
            try {
                result.put(key, submitPfop(key, quality));
            } catch (Exception e) {
                log.error("提交 pfop 失败 key={}", key, e);
            }
        }
        return result;
    }

    public Map<String, String> batchSubmitPfop(List<String> srcKeys) {
        return batchSubmitPfop(srcKeys, 85);
    }

    /**
     * 查询 pfop 状态。
     *
     * @return 0 成功 / 1 等待 / 2 进行中 / 3 失败 / -1 查询异常
     */
    public int queryPfopStatus(String persistentId) {
        OperationManager op = new OperationManager(getAuth(), getConfiguration());
        try {
            return op.prefop(persistentId).code;
        } catch (QiniuException e) {
            log.error("查询 pfop 状态失败 pid={}，error={}",
                    persistentId, e.response == null ? "" : e.response.toString());
            return -1;
        }
    }

    /* ====================== 工具方法 ====================== */

    private void requireNonBlank(String... values) {
        for (String v : values) {
            if (StrUtil.isBlank(v)) {
                throw new IllegalArgumentException("参数不能为空");
            }
        }
    }
}
