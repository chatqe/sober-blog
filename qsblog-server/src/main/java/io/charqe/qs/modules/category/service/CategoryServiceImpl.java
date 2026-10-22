package io.charqe.qs.modules.category.service;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.StatusEnum;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.category.constant.CateConstant;
import io.charqe.qs.modules.category.controller.admin.dto.CategoryBatchSortDTO;
import io.charqe.qs.modules.category.controller.admin.dto.CategorySaveReqDTO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryDetailVO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryListVO;
import io.charqe.qs.modules.category.controller.web.vo.CategoryWebVO;
import io.charqe.qs.modules.category.controller.web.vo.HomeStatsVO;
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
import java.util.Map;
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

    @Resource
    private io.charqe.qs.modules.article.domain.dao.ArticleMapper articleMapper;


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
    public HomeStatsVO getHomeStats() {
        // 1. 查基础分类数据（isShow=1 且 status=1，按 sort 升序）
        List<Category> categories = lambdaQuery()
                .eq(Category::getIsShow, 1)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .list();
        if (categories.isEmpty()) {
            return new HomeStatsVO();
        }

        // 2. 批量统计每分类文章数（1次SQL GROUP BY）
        Map<Long, Long> categoryCountMap = articleMapper.countByCategoryId();

        // 3. 内存组装
        long articleTotal = categoryCountMap.values().stream().mapToLong(Long::longValue).sum();

        List<HomeStatsVO.CategoryStatsVO> statsList = categories.stream()
                .map(cat -> {
                    HomeStatsVO.CategoryStatsVO vo = new HomeStatsVO.CategoryStatsVO();
                    vo.setId(cat.getId());
                    vo.setName(cat.getName());
                    vo.setDesc(cat.getCateDesc());
                    vo.setType(cat.getType());
                    vo.setSort(cat.getSort());
                    vo.setIcon(cat.getIcon());
                    vo.setCountOfSort(categoryCountMap.getOrDefault(cat.getId(), 0L));
                    return vo;
                })
                .collect(Collectors.toList());

        HomeStatsVO result = new HomeStatsVO();
        result.setArticleTotal(articleTotal);
        result.setCategoryTotal((long) statsList.size());
        result.setCategories(statsList);
        return result;
    }

    private void clearCache() {
        qingSiCache.del(CommonConst.CATEGORY_INFO);
        qingSiCache.del(CommonConst.CATEGORY_DICT);
        qingSiCache.del(CateConstant.CATEGORY_NAVBAR);
        qingSiCache.del("category:public:type_0");
        qingSiCache.del("category:public:type_1");
    }

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
