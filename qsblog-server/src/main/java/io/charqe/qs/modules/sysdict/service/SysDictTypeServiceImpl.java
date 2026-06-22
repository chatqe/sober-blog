package io.charqe.qs.modules.sysdict.service;

import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.sysdict.controller.admin.dto.SysDictTypeReq;
import io.charqe.qs.modules.sysdict.domain.dao.SysDictTypeMapper;
import io.charqe.qs.modules.sysdict.domain.pojo.SysDictType;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 字典类型服务实现
 *
 * @author Joy
 * @since 2026-06-22
 */
@Slf4j
@Service
public class SysDictTypeServiceImpl extends CoreServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictTypeService {

    @Resource
    private SysDictDataService sysDictDataService;

    @Resource
    private QingSiCache qingSiCache;

    private static final String DICT_CACHE_PREFIX = "sysDict:";

    @Override
    public PageResult<SysDictType> page(String typeName, Integer status, int pageNum, int pageSize) {
        var wrapper = Lb.<SysDictType>q()
                .like(StringUtils.hasText(typeName), SysDictType::getTypeName, typeName)
                .eq(status != null, SysDictType::getStatus, status)
                .orderByAsc(SysDictType::getSort);

        var mpPage = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), wrapper);
        return PageResult.fromMpPage(mpPage);
    }

    @Override
    public void save(SysDictTypeReq req) {
        if (!StringUtils.hasText(req.getTypeCode())) {
            throw new BizException("字典类型编码不能为空");
        }

        // 检查编码唯一性
        long count = lambdaQuery().eq(SysDictType::getTypeCode, req.getTypeCode()).count();
        if (count > 0) {
            throw new BizException("字典类型编码已存在");
        }

        SysDictType entity = new SysDictType();
        entity.setTypeCode(req.getTypeCode());
        entity.setTypeName(req.getTypeName());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        entity.setSort(req.getSort() != null ? req.getSort() : 0);
        entity.setRemark(req.getRemark());

        save(entity);
        clearDictCache();
    }

    @Override
    public void update(SysDictTypeReq req) {
        if (req.getId() == null) {
            throw new BizException("字典类型ID不能为空");
        }

        SysDictType existing = getById(req.getId());
        if (existing == null) {
            throw new BizException("字典类型不存在");
        }

        // 检查编码唯一性（排除自身）
        if (StringUtils.hasText(req.getTypeCode()) && !req.getTypeCode().equals(existing.getTypeCode())) {
            long count = lambdaQuery().eq(SysDictType::getTypeCode, req.getTypeCode()).count();
            if (count > 0) {
                throw new BizException("字典类型编码已存在");
            }
        }

        SysDictType entity = new SysDictType();
        entity.setId(req.getId());
        entity.setTypeCode(req.getTypeCode());
        entity.setTypeName(req.getTypeName());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        entity.setSort(req.getSort() != null ? req.getSort() : 0);
        entity.setRemark(req.getRemark());

        updateById(entity);
        clearDictCache();
    }

    @Override
    public void batchDelete(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new BizException("请选择要删除的字典类型");
        }

        // 先删除关联的字典数据
        for (Long id : ids) {
            SysDictType type = getById(id);
            if (type != null) {
                sysDictDataService.remove(Lb.<io.charqe.qs.modules.sysdict.domain.pojo.SysDictData>q()
                        .eq(io.charqe.qs.modules.sysdict.domain.pojo.SysDictData::getTypeCode, type.getTypeCode()));
                qingSiCache.del(DICT_CACHE_PREFIX + type.getTypeCode());
            }
        }

        removeByIds(java.util.Arrays.asList(ids));
        clearDictCache();
    }

    @Override
    public List<SysDictType> listEnabled() {
        return lambdaQuery()
                .eq(SysDictType::getStatus, 1)
                .orderByAsc(SysDictType::getSort)
                .list();
    }

    private void clearDictCache() {
        qingSiCache.del(CommonConst.SORT_INFO);
    }
}
