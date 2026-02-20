package io.charqe.qs.modules.moment.controller.web;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.VisibilityEnum;
import io.charqe.qs.modules.article.domain.dao.ArticleMapper;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.moment.domain.pojo.Moment;
import io.charqe.qs.modules.moment.service.MomentService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.StrUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 动态管理
 *
 * @author Joy
 * @since 2025-12-31 20:23
 */

@RestController
@RequestMapping("/moment")
@Tag(name = "moment", description = "动态管理")
public class MomentController {

    @Resource
    private MomentService momentService;

    @Resource
    private ArticleMapper articleMapper;

    /**
     * 保存
     */
    @PostMapping("/save")
    @LoginCheck
    @SaveCheck
    public R saveMoment(@RequestBody Moment momentVO) {
        if (!StringUtils.hasText(momentVO.getContent())) {
            return R.fail("动态不能为空！");
        }

        String content = StrUtil.removeHtml(momentVO.getContent());
        if (!StringUtils.hasText(content)) {
            return R.fail("动态内容不合法！");
        }
        momentVO.setContent(content);

        Moment moment = new Moment();
        moment.setUserId(BlogUtil.getUserId());
        moment.setContent(momentVO.getContent());
        moment.setIsPublic(momentVO.getIsPublic());
        moment.setType(CommonConst.MOMENT_TYPE_FRIEND);
        momentService.save(moment);
        return R.success();
    }


    /**
     * 保存
     */
    @PostMapping("/saveNews")
    @LoginCheck
    public R saveNews(@RequestBody Moment momentVO) {
        if (!StringUtils.hasText(momentVO.getContent()) || momentVO.getSource() == null) {
            return R.fail("信息不全！");
        }

        if (momentVO.getCreateTime() == null) {
            momentVO.setCreateTime(LocalDateTime.now());
        }

        Long userId = BlogUtil.getUserId();

        LambdaQueryChainWrapper<Article> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
        Integer count = Math.toIntExact(wrapper.eq(Article::getId, momentVO.getSource()).eq(Article::getUserId, userId).count());

        if (count == null || count < 1) {
            return R.fail("来源不存在！");
        }

        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setContent(momentVO.getContent());
        moment.setIsPublic(Boolean.TRUE);
        moment.setSource(momentVO.getSource());
        moment.setCreateTime(momentVO.getCreateTime());
        moment.setType(CommonConst.MOMENT_TYPE_NEWS);
        momentService.save(moment);
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
        LambdaQueryChainWrapper<Moment> lambdaQuery = momentService.lambdaQuery();
        lambdaQuery.eq(Moment::getType, CommonConst.MOMENT_TYPE_NEWS);
        lambdaQuery.eq(Moment::getSource, baseReqVO.getSource());
        lambdaQuery.eq(Moment::getIsPublic, VisibilityEnum.PUBLIC.code());

        lambdaQuery.orderByDesc(Moment::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }

    /**
     * 删除
     */
    @GetMapping("/del")
    @LoginCheck
    public R deleteMoment(@RequestParam("id") Integer id) {
        Long userId = BlogUtil.getUserId();
        momentService.lambdaUpdate().eq(Moment::getId, id)
                .eq(Moment::getUserId, userId)
                .remove();
        return R.success();
    }


    /**
     * 查询List
     */
    @PostMapping("/list")
    public R<BaseReqVO> listMoment(@RequestBody BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<Moment> lambdaQuery = momentService.lambdaQuery();
        lambdaQuery.eq(Moment::getType, CommonConst.MOMENT_TYPE_FRIEND);
        if (baseReqVO.getUserId() == null) {
            if (BlogUtil.getUserId() != null) {
                lambdaQuery.eq(Moment::getUserId, BlogUtil.getUserId());
            } else {
                lambdaQuery.eq(Moment::getIsPublic, VisibilityEnum.PUBLIC.code());
                lambdaQuery.eq(Moment::getUserId, BlogUtil.getAdminUser().getId());
            }
        } else {
            if (!baseReqVO.getUserId().equals(BlogUtil.getUserId())) {
                lambdaQuery.eq(Moment::getIsPublic, VisibilityEnum.PUBLIC.code());
            }
            lambdaQuery.eq(Moment::getUserId, baseReqVO.getUserId());
        }

        lambdaQuery.orderByDesc(Moment::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }
}


