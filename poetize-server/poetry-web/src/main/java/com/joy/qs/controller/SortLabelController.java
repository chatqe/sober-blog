package com.joy.qs.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.joy.qs.aop.LoginCheck;
import com.joy.qs.common.R;
import com.joy.qs.dao.LabelMapper;
import com.joy.qs.dao.SortMapper;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.model.label.pojo.Label;
import com.joy.qs.model.sort.pojo.Sort;
import com.joy.qs.utils.CommonQuery;
import com.joy.qs.utils.cache.PoetryCache;
import com.joy.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 分类标签 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Slf4j
@RestController
@RequestMapping("/webInfo")
public class SortLabelController {

    @Autowired
    private SortMapper sortMapper;

    @Autowired
    private LabelMapper labelMapper;

    @Autowired
    private CommonQuery commonQuery;

    @Resource
    private QingSiCache qingSiCache;

    /**
     * 获取分类标签信息
     */
    @GetMapping("/getSortInfo")
    public R<List<Sort>> getSortInfo() {
        return R.success(commonQuery.getSortInfo());
    }

    /**
     * 保存
     */
    @PostMapping("/saveSort")
    @LoginCheck(0)
    public R saveSort(@RequestBody Sort sort) {
        if (!StringUtils.hasText(sort.getSortName()) || !StringUtils.hasText(sort.getSortDescription())) {
            return R.fail("分类名称和分类描述不能为空！");
        }

        if (sort.getPriority() == null) {
            return R.fail("分类必须配置优先级！");
        }

        sortMapper.insert(sort);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 删除
     */
    @GetMapping("/deleteSort")
    @LoginCheck(0)
    public R deleteSort(@RequestParam("id") Integer id) {
        sortMapper.deleteById(id);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 更新
     */
    @PostMapping("/updateSort")
    @LoginCheck(0)
    public R updateSort(@RequestBody Sort sort) {
        sortMapper.updateById(sort);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }




    /**
     * 查询List
     */
    @GetMapping("/listSort")
    public R<List<Sort>> listSort() {
        return R.success(new LambdaQueryChainWrapper<>(sortMapper).list());
    }


    /**
     * 保存
     */
    @PostMapping("/saveLabel")
    @LoginCheck(0)
    public R saveLabel(@RequestBody Label label) {
        if (!StringUtils.hasText(label.getLabelName()) || !StringUtils.hasText(label.getLabelDescription()) || label.getSortId() == null) {
            return R.fail("标签名称和标签描述和分类Id不能为空！");
        }
        labelMapper.insert(label);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 删除
     */
    @GetMapping("/deleteLabel")
    @LoginCheck(0)
    public R deleteLabel(@RequestParam("id") Integer id) {
        labelMapper.deleteById(id);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 更新
     */
    @PostMapping("/updateLabel")
    @LoginCheck(0)
    public R updateLabel(@RequestBody Label label) {
        labelMapper.updateById(label);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/listLabel")
    public R<List<Label>> listLabel() {
        return R.success(new LambdaQueryChainWrapper<>(labelMapper).list());
    }


    /**
     * 查询List
     */
    @GetMapping("/listSortAndLabel")
    public R<Map> listSortAndLabel() {
        Map<String, List> map = new HashMap<>();
        map.put("sorts", new LambdaQueryChainWrapper<>(sortMapper).list());
        map.put("labels", new LambdaQueryChainWrapper<>(labelMapper).list());
        return R.success(map);
    }
}
