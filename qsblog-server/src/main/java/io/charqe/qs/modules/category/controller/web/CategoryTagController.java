package io.charqe.qs.modules.category.controller.web;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.tag.domain.dao.TagMapper;
import io.charqe.qs.modules.category.domain.dao.CategoryMapper;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.tag.domain.pojo.Tag;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryTagController {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private CommonQuery commonQuery;

    @Resource
    private QingSiCache qingSiCache;

    /**
     * 获取分类标签信息
     */
    @GetMapping("/getCategoryInfo")
    public R<List<Category>> getCategoryInfo() {
        return R.success(commonQuery.getCategoryInfo());
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    @LoginCheck(0)
    public R saveCategory(@RequestBody Category category) {
        if (!StringUtils.hasText(category.getCategoryName()) || !StringUtils.hasText(category.getDescription())) {
            return R.fail("分类名称和分类描述不能为空！");
        }

        if (category.getSort() == null) {
            return R.fail("分类必须配置优先级！");
        }

        categoryMapper.insert(category);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 删除
     */
    @GetMapping("/del")
    @LoginCheck(0)
    public R deleteCategory(@RequestParam("id") Integer id) {
        categoryMapper.deleteById(id);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 更新
     */
    @PostMapping("/update")
    @LoginCheck(0)
    public R updateCategory(@RequestBody Category category) {
        categoryMapper.updateById(category);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }



    /**
     * 查询List
     */
    @GetMapping("/list")
    public R<List<Category>> listCategory() {
        return R.success(new LambdaQueryChainWrapper<>(categoryMapper).list());
    }





    /**
     * 查询List
     */
    @GetMapping("/listCategoryAndTag")
    public R<Map> listCategoryAndTag() {
        Map<String, List> map = new HashMap<>();
        map.put("categories", new LambdaQueryChainWrapper<>(categoryMapper).list());
        map.put("tags", new LambdaQueryChainWrapper<>(tagMapper).list());
        return R.success(map);
    }
}
