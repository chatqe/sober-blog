package io.charqe.qs.modules.tag.controller.web;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.tag.domain.pojo.Tag;
import io.charqe.qs.modules.tag.service.TagServiceImpl;
import io.charqe.qs.utils.cache.QingSiCache;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 标签控制
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */

@RestController
@RequestMapping("/tag")
public class TagController {


    @Resource
    private TagServiceImpl tagService;

    @Resource
    private QingSiCache qingSiCache;

    /**
     * 保存
     */
    @PostMapping("/save")
    @LoginCheck(0)
    public R save(@RequestBody Tag tag) {
        if (!StringUtils.hasText(tag.getName()) || !StringUtils.hasText(tag.getDesc()) || tag.getCategoryId() == null) {
            return R.fail("标签名称和标签描述和分类Id不能为空！");
        }
        tagService.save(tag);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @LoginCheck(0)
    public R delete(@RequestParam("id") Long id) {
        tagService.removeById(id);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 更新
     */
    @PostMapping("/update")
    @LoginCheck(0)
    public R update(@RequestBody Tag tag) {
        tagService.updateById(tag);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/list")
    public R<List<Tag>> list() {
        return R.success(tagService.list());
    }
}
