package io.charqe.qs.modules.family.controller.admin;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.family.controller.web.vo.FamilyVO;
import io.charqe.qs.modules.family.domain.pojo.Family;
import io.charqe.qs.modules.family.service.FamilyService;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.cache.PoetryCache;
import io.charqe.qs.utils.cache.QingSiCache;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 家庭信息 前端控制器
 * </p>
 *
 * @author sara
 * @since 2023-01-03
 */
@RestController
@RequestMapping("/family")
public class FamilyController {

    @Autowired
    private FamilyService familyService;

    @Autowired
    private CommonQuery commonQuery;

    @Resource
    private QingSiCache qingSiCache;

    /**
     * 保存
     */
    @PostMapping("/saveFamily")
    @LoginCheck
    public R saveFamily(@Validated @RequestBody FamilyVO familyVO) {
        Integer userId = BlogUtil.getUserId();
        familyVO.setUserId(userId);
        Family oldFamily = familyService.lambdaQuery().select(Family::getId).eq(Family::getUserId, userId).one();
        Family family = new Family();
        BeanUtils.copyProperties(familyVO, family);
        if (userId.intValue() == BlogUtil.getAdminUser().getId().intValue()) {
            family.setStatus(Boolean.TRUE);
        } else {
            family.setStatus(Boolean.FALSE);
        }

        if (oldFamily != null) {
            family.setId(oldFamily.getId());
            familyService.updateById(family);
        } else {
            family.setId(null);
            familyService.save(family);
        }
        if (userId.intValue() == BlogUtil.getAdminUser().getId().intValue()) {
            // PoetryCache.put(CommonConst.ADMIN_FAMILY, family);
            qingSiCache.put(CommonConst.ADMIN_FAMILY, family);
        }
        // PoetryCache.remove(CommonConst.FAMILY_LIST);
        qingSiCache.del(CommonConst.FAMILY_LIST);
        return R.success();
    }

    /**
     * 删除
     */
    @GetMapping("/deleteFamily")
    @LoginCheck(0)
    public R deleteFamily(@RequestParam("id") Integer id) {
        familyService.removeById(id);
        // PoetryCache.remove(CommonConst.FAMILY_LIST);
        qingSiCache.del(CommonConst.FAMILY_LIST);
        return R.success();
    }

    /**
     * 获取
     */
    @GetMapping("/getFamily")
    @LoginCheck
    public R<FamilyVO> getFamily() {
        Integer userId = BlogUtil.getUserId();
        Family family = familyService.lambdaQuery().eq(Family::getUserId, userId).one();
        if (family == null) {
            return R.success();
        } else {
            FamilyVO familyVO = new FamilyVO();
            BeanUtils.copyProperties(family, familyVO);
            return R.success(familyVO);
        }
    }

    /**
     * 获取
     */
    @GetMapping("/getAdminFamily")
    public R<FamilyVO> getAdminFamily() {
        // Family family = (Family) PoetryCache.get(CommonConst.ADMIN_FAMILY);
        Family family = JSON.parseObject(qingSiCache.get(CommonConst.ADMIN_FAMILY), Family.class) ;
        if (Objects.isNull(family)) {
            // 查数据库
            family= familyService.getOne(Wrappers.<Family>lambdaQuery().eq(Family::getUserId, BlogUtil.getAdminUser().getId()));
            if (Objects.isNull(family)) {
                return R.fail("请初始化表白墙");
            }
            qingSiCache.put(CommonConst.ADMIN_FAMILY, JSON.toJSONString(family));
        }
        FamilyVO familyVO = new FamilyVO();
        BeanUtils.copyProperties(family, familyVO);
        return R.success(familyVO);
    }

    /**
     * 查询随机家庭
     */
    @GetMapping("/listRandomFamily")
    public R<List<FamilyVO>> listRandomFamily(@RequestParam(value = "size", defaultValue = "10") Integer size) {
        List<FamilyVO> familyList = commonQuery.getFamilyList();
        if (familyList.size() > size) {
            Collections.shuffle(familyList);
            familyList = familyList.subList(0, size);
        }
        return R.success(familyList);
    }

    /**
     * 查询
     */
    @PostMapping("/listFamily")
    @LoginCheck(0)
    public R<Page> listFamily(@RequestBody BaseReqVO baseReqVO) {
        familyService.lambdaQuery()
                .eq(baseReqVO.getStatus() != null, Family::getStatus, baseReqVO.getStatus())
                .orderByDesc(Family::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }

    /**
     * 修改状态
     */
    @GetMapping("/changeLoveStatus")
    @LoginCheck(0)
    public R changeLoveStatus(@RequestParam("id") Integer id, @RequestParam("flag") Boolean flag) {
        familyService.lambdaUpdate().eq(Family::getId, id).set(Family::getStatus, flag).update();
        PoetryCache.remove(CommonConst.FAMILY_LIST);
        return R.success();
    }
}


