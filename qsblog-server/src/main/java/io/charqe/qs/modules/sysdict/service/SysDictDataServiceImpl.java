package io.charqe.qs.modules.sysdict.service;

import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.sysdict.controller.admin.dto.SysDictDataReq;
import io.charqe.qs.modules.sysdict.controller.web.vo.SysDictDataVO;
import io.charqe.qs.modules.sysdict.domain.dao.SysDictDataMapper;
import io.charqe.qs.modules.sysdict.domain.pojo.SysDictData;
import io.charqe.qs.modules.sysdict.domain.pojo.SysDictType;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典数据服务实现
 *
 * @author Joy
 * @since 2026-06-22
 */
@Slf4j
@Service
public class SysDictDataServiceImpl extends CoreServiceImpl<SysDictDataMapper, SysDictData> implements SysDictDataService {

    @Resource
    private SysDictTypeService sysDictTypeService;

    @Resource
    private QingSiCache qingSiCache;

    private static final String DICT_CACHE_PREFIX = "sysDict:";

    @Override
    public List<SysDictDataVO> listByTypeCode(String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            return new ArrayList<>();
        }

        // 优先从缓存读取
        String cacheKey = DICT_CACHE_PREFIX + typeCode;
        String cached = qingSiCache.get(cacheKey);
        if (cached != null) {
            return com.alibaba.fastjson2.JSON.parseArray(cached, SysDictDataVO.class);
        }

        // 查询数据库
        List<SysDictDataVO> list = lambdaQuery()
                .eq(SysDictData::getTypeCode, typeCode)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort)
                .list()
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        // 写入缓存
        if (!list.isEmpty()) {
            qingSiCache.put(cacheKey, com.alibaba.fastjson2.JSON.toJSONString(list));
        }

        return list;
    }

    @Override
    public Map<String, List<SysDictDataVO>> batchListByTypeCodes(List<String> typeCodes) {
        if (typeCodes == null || typeCodes.isEmpty()) {
            return java.util.Collections.emptyMap();
        }

        Map<String, List<SysDictDataVO>> result = new java.util.LinkedHashMap<>();

        for (String typeCode : typeCodes) {
            List<SysDictDataVO> dataList = listByTypeCode(typeCode);
            if (!dataList.isEmpty()) {
                result.put(typeCode, dataList);
            }
        }

        return result;
    }

    @Override
    public void save(SysDictDataReq req) {
        validateTypeCode(req.getTypeCode());

        // 检查同一类型下编码唯一性
        long count = lambdaQuery()
                .eq(SysDictData::getTypeCode, req.getTypeCode())
                .eq(SysDictData::getItemCode, req.getItemCode())
                .count();
        if (count > 0) {
            throw new BizException("该字典类型下编码已存在");
        }

        SysDictData entity = new SysDictData();
        entity.setTypeCode(req.getTypeCode());
        entity.setItemCode(req.getItemCode());
        entity.setItemValue(req.getItemValue());
        entity.setItemLabel(req.getItemLabel());
        entity.setSort(req.getSort() != null ? req.getSort() : 0);
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        entity.setIsDefault(req.getIsDefault() != null ? req.getIsDefault() : 0);
        entity.setRemark(req.getRemark());

        save(entity);
        clearDictCache(req.getTypeCode());
    }

    @Override
    public void update(SysDictDataReq req) {
        if (req.getId() == null) {
            throw new BizException("字典数据ID不能为空");
        }

        SysDictData existing = getById(req.getId());
        if (existing == null) {
            throw new BizException("字典数据不存在");
        }

        validateTypeCode(req.getTypeCode());

        // 检查同一类型下编码唯一性（排除自身）
        if (StringUtils.hasText(req.getItemCode()) && !req.getItemCode().equals(existing.getItemCode())) {
            long count = lambdaQuery()
                    .eq(SysDictData::getTypeCode, req.getTypeCode())
                    .eq(SysDictData::getItemCode, req.getItemCode())
                    .ne(SysDictData::getId, req.getId())
                    .count();
            if (count > 0) {
                throw new BizException("该字典类型下编码已存在");
            }
        }

        SysDictData entity = new SysDictData();
        entity.setId(req.getId());
        entity.setTypeCode(req.getTypeCode());
        entity.setItemCode(req.getItemCode());
        entity.setItemValue(req.getItemValue());
        entity.setItemLabel(req.getItemLabel());
        entity.setSort(req.getSort() != null ? req.getSort() : 0);
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        entity.setIsDefault(req.getIsDefault() != null ? req.getIsDefault() : 0);
        entity.setRemark(req.getRemark());

        updateById(entity);
        clearDictCache(req.getTypeCode());
    }

    @Override
    public void batchDelete(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new BizException("请选择要删除的字典数据");
        }

        List<SysDictData> list = listByIds(java.util.Arrays.asList(ids));
        for (SysDictData data : list) {
            clearDictCache(data.getTypeCode());
        }

        removeByIds(java.util.Arrays.asList(ids));
    }

    /**
     * 根据typeCode和itemValue获取字典项label
     */
    @Override
    public String getItemLabel(String typeCode, String itemValue) {
        if (!StringUtils.hasText(typeCode) || !StringUtils.hasText(itemValue)) {
            return itemValue;
        }

        String cacheKey = DICT_CACHE_PREFIX + typeCode + ":" + itemValue;
        String cached = qingSiCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String label = lambdaQuery()
                .eq(SysDictData::getTypeCode, typeCode)
                .eq(SysDictData::getItemValue, itemValue)
                .eq(SysDictData::getStatus, 1)
                .oneOpt()
                .map(SysDictData::getItemLabel)
                .orElse(itemValue);

        qingSiCache.put(cacheKey, label);
        return label;
    }

    private void validateTypeCode(String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            throw new BizException("字典类型编码不能为空");
        }
        // 验证字典类型是否存在
        boolean exists = sysDictTypeService.lambdaQuery()
                .eq(SysDictType::getTypeCode, typeCode)
                .exists();
        if (!exists) {
            throw new BizException("字典类型不存在");
        }
    }

    private void clearDictCache(String typeCode) {
        qingSiCache.del(DICT_CACHE_PREFIX + typeCode);
    }

    private SysDictDataVO toVO(SysDictData entity) {
        SysDictDataVO vo = new SysDictDataVO();
        vo.setItemCode(entity.getItemCode());
        vo.setItemValue(entity.getItemValue());
        vo.setItemLabel(entity.getItemLabel());
        vo.setSort(entity.getSort());
        return vo;
    }
}
