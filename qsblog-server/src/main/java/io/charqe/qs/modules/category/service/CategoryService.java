package io.charqe.qs.modules.category.service;

import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.modules.category.controller.admin.dto.CategoryBatchSortDTO;
import io.charqe.qs.modules.category.controller.admin.dto.CategorySaveReqDTO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryDetailVO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryListVO;
import io.charqe.qs.modules.category.controller.web.vo.CategoryWebVO;
import io.charqe.qs.modules.category.controller.web.vo.HomeStatsVO;
import io.charqe.qs.modules.sysdict.controller.web.vo.DictVO;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.common.model.mp.CoreService;

import java.util.List;

/**
 * 分类服务类
 *
 * @author Joy
 * @since 2026-06-22
 */
public interface CategoryService extends CoreService<Category> {

    /**
     * 分页查询分类列表（Admin）
     */
    PageResult<CategoryListVO> pageList(String keyword, Integer type, Integer status, int pageNum, int pageSize);

    /**
     * 获取分类详情（Admin）
     */
    CategoryDetailVO getDetail(Long id);

    /**
     * 新增分类
     */
    void save(CategorySaveReqDTO dto);

    /**
     * 更新分类
     */
    void update(CategorySaveReqDTO dto);

    /**
     * 删除分类
     */
    void delete(Long id);

    /**
     * 批量排序
     */
    void batchSort(CategoryBatchSortDTO dto);

    /**
     * 获取公开分类列表（Web）
     */
    List<CategoryWebVO> listNavBar();

    /**
     * 获取首页分类统计（含每类文章计数、文章总数）
     */
    HomeStatsVO getHomeStats();

    /**
     * 获取分类下拉字典列表
     */
    List<DictVO> getDictList();
}
