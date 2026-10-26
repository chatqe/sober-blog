package io.charqe.qs.modules.category.service;

import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.enums.OptionScopeEnum;
import io.charqe.qs.common.model.OptionVO;
import io.charqe.qs.modules.category.controller.admin.dto.CategoryBatchSortDTO;
import io.charqe.qs.modules.category.controller.admin.dto.CategorySaveReqDTO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryDetailVO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryListVO;
import io.charqe.qs.modules.category.controller.web.vo.CategoryWebVO;
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
     * 获取公开分类列表（Web，启用且展示，含导航栏与普通分类）
     */
    List<CategoryWebVO> listWebCategories();

    /**
     * 获取分类下拉选项（Options）
     *
     * @param scope 口径：PUBLIC 仅启用且展示；ALL 含停用/隐藏并返回状态字段（只供后台使用）
     * @return 精简选项列表，按 sort、id 升序
     */
    List<OptionVO> listOptions(OptionScopeEnum scope);

    /**
     * 获取分类下拉字典列表
     *
     * @deprecated value 返回的是分类描述（cateDesc）而非主键，不能用于表单取值，
     *             请改用 {@link #listOptions(OptionScopeEnum)}；保留仅为外部兼容
     */
    @Deprecated
    List<DictVO> getDictList();

    /**
     * 统计可见分类总数（启用且展示）
     *
     * @return 可见分类数量
     */
    long countVisible();
}
