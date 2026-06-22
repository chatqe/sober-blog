package io.charqe.qs.modules.category.service;

import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.category.controller.admin.dto.CategoryBatchSortDTO;
import io.charqe.qs.modules.category.controller.admin.dto.CategorySaveReqDTO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryDetailVO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryListVO;
import io.charqe.qs.modules.category.controller.web.vo.CategoryWebVO;
import io.charqe.qs.modules.category.domain.dao.CategoryMapper;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

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
        List<CategoryListVO> voList = mpPage.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        result.setList(voList);
        result.setTotal(mpPage.getTotal());
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
        return toDetailVO(category);
    }

    @Override
    public void save(CategorySaveReqDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new BizException("分类名称不能为空");
        }

        Category entity = toEntity(dto);
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

        Category entity = toEntity(dto);
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
    public List<CategoryWebVO> listPublic(Integer type) {
        String cacheKey = "category:public:type_" + type;
        String cached = qingSiCache.get(cacheKey);
        if (cached != null) {
            return com.alibaba.fastjson2.JSON.parseArray(cached, CategoryWebVO.class);
        }

        var wrapper = Lb.<Category>q()
                .eq(Category::getStatus, 1)
                .eq(Category::getIsShow, 1)
                .eq(type != null, Category::getType, type)
                .orderByAsc(Category::getSort);

        List<CategoryWebVO> list = lambdaQuery().eq(Category::getStatus, 1)
                .eq(Category::getIsShow, 1)
                .eq(type != null, Category::getType, type)
                .orderByAsc(Category::getSort)
                .list()
                .stream()
                .map(this::toWebVO)
                .collect(Collectors.toList());

        if (!list.isEmpty()) {
            qingSiCache.put(cacheKey, com.alibaba.fastjson2.JSON.toJSONString(list));
        }
        return list;
    }

    private Category toEntity(CategorySaveReqDTO dto) {
        Category entity = new Category();
        entity.setName(dto.getName());
        entity.setDesc(dto.getDesc());
        entity.setType(dto.getType());
        entity.setIsShow(dto.getIsShow());
        entity.setSort(dto.getSort());
        entity.setIcon(dto.getIcon());
        return entity;
    }

    private CategoryListVO toListVO(Category entity) {
        CategoryListVO vo = new CategoryListVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDesc(entity.getDesc());
        vo.setType(entity.getType());
        vo.setIsShow(entity.getIsShow());
        vo.setSort(entity.getSort());
        vo.setIcon(entity.getIcon());
        vo.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        return vo;
    }

    private CategoryDetailVO toDetailVO(Category entity) {
        CategoryDetailVO vo = new CategoryDetailVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDesc(entity.getDesc());
        vo.setType(entity.getType());
        vo.setIsShow(entity.getIsShow());
        vo.setSort(entity.getSort());
        vo.setIcon(entity.getIcon());
        vo.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        vo.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().toString() : null);
        return vo;
    }

    private CategoryWebVO toWebVO(Category entity) {
        CategoryWebVO vo = new CategoryWebVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDesc(entity.getDesc());
        vo.setType(entity.getType());
        vo.setSort(entity.getSort());
        vo.setIcon(entity.getIcon());
        return vo;
    }

    private void clearCache() {
        qingSiCache.del(CommonConst.SORT_INFO);
    }
}
