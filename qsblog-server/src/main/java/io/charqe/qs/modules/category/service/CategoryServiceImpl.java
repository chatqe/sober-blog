package io.charqe.qs.modules.category.service;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.constants.CacheKeyConst;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.OptionScopeEnum;
import io.charqe.qs.common.enums.StatusEnum;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.OptionVO;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.category.constant.CateConstant;
import io.charqe.qs.modules.category.controller.admin.dto.CategoryBatchSortDTO;
import io.charqe.qs.modules.category.controller.admin.dto.CategorySaveReqDTO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryDetailVO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryListVO;
import io.charqe.qs.modules.category.controller.web.vo.CategoryWebVO;
import io.charqe.qs.modules.category.convert.CategoryConvert;
import io.charqe.qs.modules.category.enums.CategoryTypeEnum;
import io.charqe.qs.modules.sysdict.controller.web.vo.DictVO;
import io.charqe.qs.modules.category.domain.dao.CategoryMapper;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 分类服务实现
 *
 * @author Joy
 * @since 2026-06-22
 */
@Slf4j
@Service
public class CategoryServiceImpl extends CoreServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private QingSiCache qingSiCache;


    @Override
    public PageResult<CategoryListVO> pageList(String keyword, Integer type, Integer status, int pageNum, int pageSize) {
        var wrapper = Lb.<Category>q()
                .like(StringUtils.hasText(keyword), Category::getName, keyword)
                .eq(type != null, Category::getType, type)
                .eq(status != null, Category::getStatus, status)
                .orderByAsc(Category::getSort);

        var mpPage = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), wrapper);
        PageResult<CategoryListVO> result = new PageResult<>();
        List<CategoryListVO> voList = mpPage.getRecords()
                .stream().map(CategoryConvert.INSTANCE::toListVO).toList();
        result.setList(voList);
        result.setTotal(Math.toIntExact(mpPage.getTotal()));
        result.setPages((int) mpPage.getPages());
        result.setPageNum((int) mpPage.getCurrent());
        result.setPageSize((int) mpPage.getSize());
        return result;
    }

    @Override
    public CategoryDetailVO getDetail(Long id) {
        Category category = getById(id);
        if (category == null) {
            throw new BizException("分类不存在");
        }
        return CategoryConvert.INSTANCE.toDetailVO(category);
    }

    @Override
    public void save(CategorySaveReqDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new BizException("分类名称不能为空");
        }

        Category entity = CategoryConvert.INSTANCE.toEntity(dto);
        save(entity);
        clearCache();
    }

    @Override
    public void update(CategorySaveReqDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("分类ID不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new BizException("分类名称不能为空");
        }

        Category existing = getById(dto.getId());
        if (existing == null) {
            throw new BizException("分类不存在");
        }

        Category entity = CategoryConvert.INSTANCE.toEntity(dto);
        updateById(entity);
        clearCache();
    }

    @Override
    public void delete(Long id) {
        Category category = getById(id);
        if (category == null) {
            throw new BizException("分类不存在");
        }

        removeById(id);
        clearCache();
    }

    @Override
    public void batchSort(CategoryBatchSortDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException("排序数据不能为空");
        }

        for (CategoryBatchSortDTO.SortItem item : dto.getItems()) {
            Category category = getById(item.getId());
            if (category != null) {
                category.setSort(item.getSort());
                updateById(category);
            }
        }
        clearCache();
    }

    @Override
    public List<CategoryWebVO> listNavBar() {

        String cached = qingSiCache.get(CateConstant.CATEGORY_NAVBAR);
        if (cached != null) {
            return JSON.parseArray(cached, CategoryWebVO.class);
        }

        List<Category> rawList = list(Lb.<Category>q()
                .eq(Category::getStatus, StatusEnum.ENABLE.getCode())
                .eq(Category::getIsShow, StatusEnum.ENABLE.getCode())
                .eq(Category::getIsDel, StatusEnum.DISABLE.getCode())
                .eq(Category::getType, CategoryTypeEnum.BAR.getCode())
                .orderByAsc(Category::getSort));

        List<CategoryWebVO> list = rawList
                .stream()
                .map(CategoryConvert.INSTANCE::toWebVO)
                .toList();

        if (!list.isEmpty()) {
            qingSiCache.put(CateConstant.CATEGORY_NAVBAR, JSON.toJSONString(list));
        }
        return list;
    }

    @Override
    public List<CategoryWebVO> listWebCategories() {
        String cached = qingSiCache.get(CateConstant.CATEGORY_PUBLIC);
        if (cached != null) {
            return JSON.parseArray(cached, CategoryWebVO.class);
        }

        List<Category> categories = lambdaQuery()
                // .eq(Category::getIsShow, StatusEnum.ENABLE.getCode())
                .eq(Category::getStatus, StatusEnum.ENABLE.getCode())
                .eq(Category::getIsDel, StatusEnum.DISABLE.getCode())
                .orderByAsc(Category::getSort)
                .list();

        List<CategoryWebVO> list = categories.stream()
                .map(CategoryConvert.INSTANCE::toWebVO)
                .toList();

        if (!list.isEmpty()) {
            qingSiCache.put(CateConstant.CATEGORY_PUBLIC, JSON.toJSONString(list));
        }
        return list;
    }

    @Override
    public long countVisible() {
        return lambdaQuery()
                .eq(Category::getIsShow, 1)
                .eq(Category::getStatus, 1)
                .count();
    }

    private void clearCache() {
        qingSiCache.del(CommonConst.CATEGORY_INFO);
        qingSiCache.del(CommonConst.CATEGORY_DICT);
        qingSiCache.del(CateConstant.CATEGORY_NAVBAR);
        qingSiCache.del(CateConstant.CATEGORY_PUBLIC);
        // 首页“分类文章”映射按分类 home_show 过滤，分类变更（尤其切换首页展示）必须一并失效，否则首页命中旧快照
        qingSiCache.del(CommonConst.CATEGORY_ARTICLE_LIST);
        // 选项缓存按口径分片（public / all），必须整族失效，否则会出现一口新、一口旧的不一致数据
        Set<String> optionKeys = qingSiCache.keys(CacheKeyConst.CATEGORY_OPTIONS + "*");
        if (optionKeys != null && !optionKeys.isEmpty()) {
            qingSiCache.del(optionKeys);
        }
    }

    @Override
    public List<OptionVO> listOptions(OptionScopeEnum scope) {
        OptionScopeEnum effective = scope == null ? OptionScopeEnum.PUBLIC : scope;
        String cacheKey = CacheKeyConst.CATEGORY_OPTIONS + effective.getCode();
        String cached = qingSiCache.get(cacheKey);
        if (cached != null) {
            return JSON.parseArray(cached, OptionVO.class);
        }

        boolean publicOnly = effective == OptionScopeEnum.PUBLIC;
        // isDel 由 BaseDO 的 @TableLogic 自动过滤；公开口径额外限定启用且展示
        List<Category> categories = list(Lb.<Category>q()
                .eq(publicOnly, Category::getStatus, StatusEnum.ENABLE.getCode())
                .eq(publicOnly, Category::getIsShow, StatusEnum.ENABLE.getCode())
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));

        List<OptionVO> options = categories.stream().map(c -> {
            OptionVO vo = OptionVO.of(c.getId(), c.getName(), c.getSort());
            if (!publicOnly) {
                vo.setStatus(c.getStatus());
                vo.setIsShow(c.getIsShow());
                vo.setDisabled(!Objects.equals(StatusEnum.ENABLE.getCode(), c.getStatus()));
            }
            return vo;
        }).toList();

        if (!options.isEmpty()) {
            qingSiCache.put(cacheKey, options);
        }
        return options;
    }

    /**
     * @deprecated 请改用 {@link #listOptions(OptionScopeEnum)}
     */
    @Deprecated
    @Override
    public List<DictVO> getDictList() {
        String cached = qingSiCache.get(CommonConst.CATEGORY_DICT);
        if (cached != null) {
            return JSON.parseArray(cached, DictVO.class);
        }
        List<Category> list = lambdaQuery()
                .eq(Category::getStatus, 1)
                .eq(Category::getIsShow, 1)
                .orderByAsc(Category::getSort)
                .list();
        List<DictVO> dictList = list.stream().map(c -> {
            DictVO vo = new DictVO();
            vo.setValue(c.getCateDesc());
            vo.setLabel(c.getName());
            vo.setSort(c.getSort());
            return vo;
        }).toList();
        if (!dictList.isEmpty()) {
            qingSiCache.put(CommonConst.CATEGORY_DICT, JSON.toJSONString(dictList));
        }
        return dictList;
    }
}
