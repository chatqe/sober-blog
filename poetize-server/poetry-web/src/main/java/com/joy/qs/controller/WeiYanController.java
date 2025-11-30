package com.joy.qs.controller;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.joy.qs.aop.LoginCheck;
import com.joy.qs.aop.SaveCheck;
import com.joy.qs.common.R;
import com.joy.qs.dao.ArticleMapper;
import com.joy.qs.enums.YouthEnum;
import com.joy.qs.model.BaseReqVO;
import com.joy.qs.model.article.pojo.Article;
import com.joy.qs.model.weiyan.pojo.WeiYan;
import com.joy.qs.service.WeiYanService;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.utils.BlogUtil;
import com.joy.qs.utils.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * <p>
 * 微言表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-10-26
 */
@RestController
@RequestMapping("/weiYan")
public class WeiYanController {

    @Autowired
    private WeiYanService weiYanService;

    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 保存
     */
    @PostMapping("/saveWeiYan")
    @LoginCheck
    @SaveCheck
    public R saveWeiYan(@RequestBody WeiYan weiYanVO) {
        if (!StringUtils.hasText(weiYanVO.getContent())) {
            return R.fail("微言不能为空！");
        }

        String content = StringUtil.removeHtml(weiYanVO.getContent());
        if (!StringUtils.hasText(content)) {
            return R.fail("微言内容不合法！");
        }
        weiYanVO.setContent(content);

        WeiYan weiYan = new WeiYan();
        weiYan.setUserId(BlogUtil.getUserId());
        weiYan.setContent(weiYanVO.getContent());
        weiYan.setIsPublic(weiYanVO.getIsPublic());
        weiYan.setType(CommonConst.WEIYAN_TYPE_FRIEND);
        weiYanService.save(weiYan);
        return R.success();
    }


    /**
     * 保存
     */
    @PostMapping("/saveNews")
    @LoginCheck
    public R saveNews(@RequestBody WeiYan weiYanVO) {
        if (!StringUtils.hasText(weiYanVO.getContent()) || weiYanVO.getSource() == null) {
            return R.fail("信息不全！");
        }

        if (weiYanVO.getCreateTime() == null) {
            weiYanVO.setCreateTime(LocalDateTime.now());
        }

        Integer userId = BlogUtil.getUserId();

        LambdaQueryChainWrapper<Article> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
        Integer count = wrapper.eq(Article::getId, weiYanVO.getSource()).eq(Article::getUserId, userId).count();

        if (count == null || count < 1) {
            return R.fail("来源不存在！");
        }

        WeiYan weiYan = new WeiYan();
        weiYan.setUserId(userId);
        weiYan.setContent(weiYanVO.getContent());
        weiYan.setIsPublic(Boolean.TRUE);
        weiYan.setSource(weiYanVO.getSource());
        weiYan.setCreateTime(weiYanVO.getCreateTime());
        weiYan.setType(CommonConst.WEIYAN_TYPE_NEWS);
        weiYanService.save(weiYan);
        return R.success();
    }

    /**
     * 查询List
     */
    @PostMapping("/listNews")
    public R<BaseReqVO> listNews(@RequestBody BaseReqVO baseReqVO) {
        if (baseReqVO.getSource() == null) {
            return R.fail("来源不能为空！");
        }
        LambdaQueryChainWrapper<WeiYan> lambdaQuery = weiYanService.lambdaQuery();
        lambdaQuery.eq(WeiYan::getType, CommonConst.WEIYAN_TYPE_NEWS);
        lambdaQuery.eq(WeiYan::getSource, baseReqVO.getSource());
        lambdaQuery.eq(WeiYan::getIsPublic, YouthEnum.PUBLIC.getCode());

        lambdaQuery.orderByDesc(WeiYan::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }

    /**
     * 删除
     */
    @GetMapping("/deleteWeiYan")
    @LoginCheck
    public R deleteWeiYan(@RequestParam("id") Integer id) {
        Integer userId = BlogUtil.getUserId();
        weiYanService.lambdaUpdate().eq(WeiYan::getId, id)
                .eq(WeiYan::getUserId, userId)
                .remove();
        return R.success();
    }


    /**
     * 查询List
     */
    @PostMapping("/listWeiYan")
    public R<BaseReqVO> listWeiYan(@RequestBody BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<WeiYan> lambdaQuery = weiYanService.lambdaQuery();
        lambdaQuery.eq(WeiYan::getType, CommonConst.WEIYAN_TYPE_FRIEND);
        if (baseReqVO.getUserId() == null) {
            if (BlogUtil.getUserId() != null) {
                lambdaQuery.eq(WeiYan::getUserId, BlogUtil.getUserId());
            } else {
                lambdaQuery.eq(WeiYan::getIsPublic, YouthEnum.PUBLIC.getCode());
                lambdaQuery.eq(WeiYan::getUserId, BlogUtil.getAdminUser().getId());
            }
        } else {
            if (!baseReqVO.getUserId().equals(BlogUtil.getUserId())) {
                lambdaQuery.eq(WeiYan::getIsPublic, YouthEnum.PUBLIC.getCode());
            }
            lambdaQuery.eq(WeiYan::getUserId, baseReqVO.getUserId());
        }

        lambdaQuery.orderByDesc(WeiYan::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }
}
