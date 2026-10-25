package io.charqe.qs.modules.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.file.controller.admin.vo.FileRefBindReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRefListReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRefUnbindReqVO;
import io.charqe.qs.modules.file.domain.dao.FileBizRelationMapper;
import io.charqe.qs.modules.file.domain.pojo.FileBizRef;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 文件业务关系表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class FileBizRefServiceImpl extends CoreServiceImpl<FileBizRelationMapper, FileBizRef> implements FileBizRefService {

    @Resource
    private FileInfoService fileInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileBizRef bind(FileRefBindReqVO req, Long userId, boolean manager) {
        validateBizType(req.getBizType());
        FileInfo file = requireFile(req.getFileId(), userId, manager);
        String field = normalizeField(req.getBizField());
        FileBizRef existing = find(file.getId(), req.getBizType(), req.getBizId(), field);
        if (existing != null) {
            return existing;
        }

        FileBizRef ref = new FileBizRef();
        ref.setFileId(file.getId());
        ref.setBizType(req.getBizType());
        ref.setBizId(req.getBizId());
        ref.setBizField(field);
        ref.setSort(req.getSort() == null ? 0 : req.getSort());
        ref.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        ref.setCreateBy(String.valueOf(userId));
        try {
            save(ref);
        } catch (DuplicateKeyException e) {
            // 并发下唯一键兜底：另一请求已绑定成功，按幂等返回既有记录
            FileBizRef duplicated = find(file.getId(), req.getBizType(), req.getBizId(), field);
            if (duplicated == null) {
                throw e;
            }
            return duplicated;
        }
        fileInfoService.increaseRefCount(file.getId());
        return ref;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbind(FileRefUnbindReqVO req, Long userId, boolean manager) {
        validateBizType(req.getBizType());
        FileInfo file = requireFile(req.getFileId(), userId, manager);
        FileBizRef ref = find(file.getId(), req.getBizType(), req.getBizId(), normalizeField(req.getBizField()));
        if (ref == null) {
            return false;
        }
        // 物理删除而非逻辑删除：唯一键不含 is_del，逻辑删除会让同一引用无法再次绑定
        getBaseMapper().purgeById(ref.getId());
        fileInfoService.decreaseRefCount(file.getId());
        return true;
    }

    @Override
    public IPage<FileBizRef> pageRefs(FileRefListReqVO req, Long userId, boolean manager) {
        req.normalize();
        LambdaQueryWrapper<FileBizRef> wrapper = Wrappers.<FileBizRef>lambdaQuery();
        if (req.getFileId() != null) {
            FileInfo file = requireFile(req.getFileId(), userId, manager);
            wrapper.eq(FileBizRef::getFileId, file.getId());
        } else if (req.getBizType() != null && req.getBizId() != null) {
            if (!manager) {
                throw new BizException("仅管理员可按业务维度查询引用");
            }
            validateBizType(req.getBizType());
            wrapper.eq(FileBizRef::getBizType, req.getBizType()).eq(FileBizRef::getBizId, req.getBizId());
        } else {
            throw new BizException("请提供文件ID或业务类型+业务ID");
        }
        if (StringUtils.hasText(req.getBizField())) {
            wrapper.eq(FileBizRef::getBizField, normalizeField(req.getBizField()));
        }
        wrapper.orderByAsc(FileBizRef::getSort).orderByDesc(FileBizRef::getId);
        return page(new Page<>(req.getCurrent(), req.getSize()), wrapper);
    }

    private FileBizRef find(Long fileId, Integer bizType, Long bizId, String bizField) {
        validateBizType(bizType);
        return getOne(Wrappers.<FileBizRef>lambdaQuery()
                .eq(FileBizRef::getFileId, fileId)
                .eq(FileBizRef::getBizType, bizType)
                .eq(FileBizRef::getBizId, bizId)
                .eq(FileBizRef::getBizField, bizField), false);
    }

    /** 引用只能挂到未删除且属于当前操作人的文件上。 */
    private FileInfo requireFile(Long fileId, Long userId, boolean manager) {
        FileInfo file = fileInfoService.getById(fileId);
        if (file == null) {
            throw new BizException("文件不存在或已删除");
        }
        if (!manager && !Objects.equals(file.getOwnerId(), userId)) {
            throw new BizException("无权操作该文件");
        }
        return file;
    }

    private void validateBizType(Integer bizType) {
        if (!BizTypeEnum.isValid(bizType)) {
            throw new BizException("未知的业务类型：" + bizType);
        }
    }

    private String normalizeField(String bizField) {
        return StringUtils.hasText(bizField) ? bizField.trim() : "";
    }
}
