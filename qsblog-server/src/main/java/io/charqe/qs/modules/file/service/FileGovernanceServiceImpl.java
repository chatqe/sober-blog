package io.charqe.qs.modules.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.infra.storage.QiniuUtil;
import io.charqe.qs.infra.storage.StorageObject;
import io.charqe.qs.modules.file.controller.admin.vo.FileBatchReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileBatchRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanBackfillReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanBackfillRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanScanReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanScanRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileUnreferencedReqVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.enums.FileSceneEnum;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件治理实现。
 * <p>
 * 治理动作区分「只读」与「写」：扫描只看对象存储与登记表差异；补登记、批量清理
 * 默认 {@code dryRun=true}，必须显式关闭才会落库/落删。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Service
public class FileGovernanceServiceImpl implements FileGovernanceService {

    /** 单次扫描上限：与入参注解保持一致。 */
    private static final int MAX_SCAN = 5000;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private ObjectProvider<QiniuUtil> qiniuProvider;

    @Value("${store.type:local}")
    private String storeType;

    @Value("${qiniu.enable:false}")
    private boolean qiniuEnabled;

    @Value("${qiniu.downloadUrl:}")
    private String downloadUrl;

    @Override
    public FileOrphanScanRespVO scan(FileOrphanScanReqVO req) {
        FileOrphanScanReqVO query = req == null ? new FileOrphanScanReqVO() : req;
        query.normalize();
        QiniuUtil qiniu = requireQiniu();

        int limit = Math.min(query.getLimit(), MAX_SCAN);
        // 多取 1 条用于判断是否还有未返回对象
        List<StorageObject> objects = qiniu.listObjects(query.getPrefix(), limit + 1);
        boolean truncated = objects.size() > limit;
        if (truncated) {
            objects = new ArrayList<>(objects.subList(0, limit));
        }
        // 七牛控制台会为目录生成以 / 结尾的 0 字节占位对象，既非文件也无需登记
        objects = objects.stream()
                .filter(object -> object.getKey() != null && !object.getKey().endsWith("/"))
                .collect(Collectors.toList());

        Set<String> existing = fileInfoService.existingStoragePaths(
                objects.stream().map(StorageObject::getKey).collect(Collectors.toList()));
        List<FileOrphanScanRespVO.Item> orphans = new ArrayList<>();
        for (StorageObject object : objects) {
            if (existing.contains(object.getKey())) {
                continue;
            }
            FileOrphanScanRespVO.Item item = new FileOrphanScanRespVO.Item();
            item.setKey(object.getKey());
            item.setSize(object.getSize());
            item.setMimeType(object.getMimeType());
            item.setPutTime(object.getPutTime());
            orphans.add(item);
        }

        FileOrphanScanRespVO resp = new FileOrphanScanRespVO();
        resp.setDryRun(!Boolean.FALSE.equals(query.getDryRun()));
        resp.setStoreType("qiniu");
        resp.setPrefix(query.getPrefix());
        resp.setScanned(objects.size());
        resp.setRegistered(objects.size() - orphans.size());
        resp.setOrphanCount(orphans.size());
        resp.setTruncated(truncated);
        resp.setList(orphans);
        return resp;
    }

    @Override
    public FileOrphanBackfillRespVO backfill(FileOrphanBackfillReqVO req) {
        List<String> keys = normalizeKeys(req == null ? null : req.getKeys());
        if (keys.isEmpty()) {
            throw new BizException("待补登记的 key 不能为空");
        }
        QiniuUtil qiniu = requireQiniu();
        boolean dryRun = !Boolean.FALSE.equals(req.getDryRun());

        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (key.endsWith("/")) {
                continue;
            }
            if (fileInfoService.findAnyByStoragePath(key) == null) {
                missing.add(key);
            }
        }
        // 只对未登记的 key 查询元数据，避免无谓的存储请求
        Map<String, Map<String, String>> metadata = missing.isEmpty()
                ? Collections.emptyMap()
                : qiniu.getFileInfo(missing);

        List<FileOrphanBackfillRespVO.Item> items = new ArrayList<>();
        int registered = 0;
        int skipped = 0;
        for (String key : keys) {
            if (key.endsWith("/")) {
                items.add(skip(key, null, "目录占位对象"));
                skipped++;
                continue;
            }
            if (fileInfoService.findAnyByStoragePath(key) != null) {
                items.add(skip(key, null, "已登记"));
                skipped++;
                continue;
            }
            Map<String, String> meta = metadata.get(key);
            if (meta == null) {
                items.add(skip(key, null, "存储中不存在"));
                skipped++;
                continue;
            }
            String scene = sceneOf(key);
            if (dryRun) {
                items.add(item(key, true, false, "dryRun", null, scene));
                continue;
            }
            try {
                FileRespVO saved = fileInfoService.register(registerVo(key, meta, scene), null, "system");
                items.add(item(key, true, false, null, saved.getId(), saved.getScene()));
                registered++;
            } catch (RuntimeException e) {
                items.add(skip(key, scene, "登记失败：" + e.getMessage()));
                skipped++;
            }
        }

        FileOrphanBackfillRespVO resp = new FileOrphanBackfillRespVO();
        resp.setDryRun(dryRun);
        resp.setTotal(keys.size());
        resp.setRegistered(registered);
        resp.setSkipped(skipped);
        resp.setList(items);
        return resp;
    }

    @Override
    public IPage<FileInfo> pageUnreferenced(FileUnreferencedReqVO req) {
        FileUnreferencedReqVO query = req == null ? new FileUnreferencedReqVO() : req;
        query.normalize();

        LambdaQueryWrapper<FileInfo> wrapper = Wrappers.<FileInfo>lambdaQuery()
                .and(w -> w.eq(FileInfo::getRefCount, 0).or().isNull(FileInfo::getRefCount))
                .eq(FileInfo::getStatus, 1)
                .eq(StringUtils.hasText(query.getScene()), FileInfo::getScene, query.getScene())
                .like(StringUtils.hasText(query.getKeyword()), FileInfo::getOriginalName, query.getKeyword())
                .le(query.getDays() > 0, FileInfo::getCreateTime, LocalDateTime.now().minusDays(query.getDays()))
                .orderByAsc(FileInfo::getCreateTime);
        return fileInfoService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    @Override
    public FileBatchRespVO batch(FileBatchReqVO req) {
        if (req == null || req.getIds() == null || req.getIds().isEmpty()) {
            throw new BizException("文件ID不能为空");
        }
        boolean dryRun = !Boolean.FALSE.equals(req.getDryRun());
        boolean force = Boolean.TRUE.equals(req.getForce());
        // 去重且保留调用方顺序，便于前端按返回索引对位
        Set<Long> ids = new LinkedHashSet<>();
        for (Long id : req.getIds()) {
            if (id != null) {
                ids.add(id);
            }
        }

        List<FileBatchRespVO.Item> items = new ArrayList<>();
        int deleted = 0;
        int skipped = 0;
        for (Long id : ids) {
            FileInfo file = fileInfoService.findAny(id);
            if (file == null) {
                items.add(skipItem(id, "文件不存在"));
                skipped++;
                continue;
            }
            int refCount = file.getRefCount() == null ? 0 : file.getRefCount();
            if (refCount > 0) {
                items.add(skipItem(id, "文件仍被 " + refCount + " 处引用"));
                skipped++;
                continue;
            }
            if (!force && Integer.valueOf(1).equals(file.getIsDel())) {
                items.add(skipItem(id, "已在回收站"));
                skipped++;
                continue;
            }
            String action = force ? "purge" : "soft-delete";
            if (!dryRun) {
                if (force) {
                    fileInfoService.purge(file);
                } else {
                    fileInfoService.softDelete(id);
                }
            }
            items.add(item(id, action));
            deleted++;
        }

        FileBatchRespVO resp = new FileBatchRespVO();
        resp.setDryRun(dryRun);
        resp.setForce(force);
        resp.setTotal(ids.size());
        resp.setDeleted(deleted);
        resp.setSkipped(skipped);
        resp.setList(items);
        return resp;
    }

    private QiniuUtil requireQiniu() {
        if (!"qiniu".equalsIgnoreCase(storeType) || !qiniuEnabled) {
            throw new BizException("孤儿治理仅支持七牛云存储");
        }
        QiniuUtil qiniu = qiniuProvider.getIfAvailable();
        if (qiniu == null) {
            throw new BizException("对象存储未启用，请联系管理员");
        }
        return qiniu;
    }

    /** 入参允许完整 CDN 地址，统一归一化为裸 key。 */
    private List<String> normalizeKeys(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        return raw.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeKey)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeKey(String raw) {
        String key = raw.trim();
        if (StringUtils.hasText(downloadUrl) && key.startsWith(downloadUrl)) {
            key = key.substring(downloadUrl.length());
        }
        return key;
    }

    /** 场景取 key 首段，未登记的场景统一落 unknown。 */
    private String sceneOf(String key) {
        int slash = key.indexOf('/');
        String segment = slash > 0 ? key.substring(0, slash) : "";
        FileSceneEnum scene = FileSceneEnum.of(segment);
        return scene == null ? FileSceneEnum.UNKNOWN.getCode() : scene.getCode();
    }

    private FileCreateReqVO registerVo(String key, Map<String, String> meta, String scene) {
        String mimeType = meta.get("mimeType");
        FileCreateReqVO vo = new FileCreateReqVO();
        vo.setStoragePath(key);
        vo.setOriginalName(StringUtils.getFilename(key));
        vo.setFileSize(Long.valueOf(meta.getOrDefault("size", "0")));
        vo.setMimeType(StringUtils.hasText(mimeType) ? mimeType : "application/octet-stream");
        vo.setThirdPartyName(3);
        vo.setScene(scene);
        return vo;
    }

    private FileOrphanBackfillRespVO.Item item(String key, boolean ok, boolean skipped,
                                               String reason, Long fileId, String scene) {
        FileOrphanBackfillRespVO.Item item = new FileOrphanBackfillRespVO.Item();
        item.setKey(key);
        item.setOk(ok);
        item.setSkipped(skipped);
        item.setReason(reason);
        item.setFileId(fileId);
        item.setScene(scene);
        return item;
    }

    private FileOrphanBackfillRespVO.Item skip(String key, String scene, String reason) {
        return item(key, false, true, reason, null, scene);
    }

    private FileBatchRespVO.Item item(Long id, String action) {
        FileBatchRespVO.Item item = new FileBatchRespVO.Item();
        item.setId(id);
        item.setOk(true);
        item.setSkipped(false);
        item.setAction(action);
        return item;
    }

    private FileBatchRespVO.Item skipItem(Long id, String reason) {
        FileBatchRespVO.Item item = new FileBatchRespVO.Item();
        item.setId(id);
        item.setOk(false);
        item.setSkipped(true);
        item.setReason(reason);
        return item;
    }
}
