package io.charqe.qs.modules.moment.controller.web;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
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
     * 保存朋友圈动态
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

        Moment moment = new Moment();
        moment.setUserId(BlogUtil.getUserId());
        moment.setContent(content);
        moment.setHasText(true);
        moment.setVisibility(VisibilityEnum.PUBLIC.code());
        moment.setSource(1); // 本站原创
        moment.setStatus(1); // 正常
        moment.setDelTime(null);
        moment.setDelBy(BlogUtil.getUserId());
        momentService.save(moment);
        return R.success();
    }

    /**
     * 保存新闻来源动态
     */
    @PostMapping("/saveNews")
    @LoginCheck
    public R saveNews(@RequestBody Moment momentVO) {
        if (!StringUtils.hasText(momentVO.getContent()) || momentVO.getSource() == null) {
            return R.fail("信息不全！");
        }

        Long userId = BlogUtil.getUserId();

        LambdaQueryChainWrapper<Article> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
        Integer count = Math.toIntExact(wrapper.eq(Article::getId, momentVO.getSource())
                .eq(Article::getUserId, userId).count());

        if (count < 1) {
            return R.fail("来源不存在！");
        }

        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setContent(momentVO.getContent());
        moment.setHasText(true);
        moment.setVisibility(VisibilityEnum.PUBLIC.code());
        moment.setSource(1); // 本站原创
        moment.setStatus(1); // 正常
        moment.setDelTime(null);
        moment.setDelBy(userId);
        momentService.save(moment);
        return R.success();
    }

    /**
     * 查询新闻列表
     */
    @PostMapping("/listNews")
    public R<BaseReqVO> listNews(@RequestBody BaseReqVO baseReqVO) {
        if (baseReqVO.getBizId() == null) {
            return R.fail("来源不能为空！");
        }
        LambdaQueryChainWrapper<Moment> lambdaQuery = momentService.lambdaQuery();
        lambdaQuery.eq(Moment::getVisibility, VisibilityEnum.PUBLIC.code());
        lambdaQuery.eq(Moment::getStatus, 1);
        lambdaQuery.eq(Moment::getSource, baseReqVO.getBizId());

        lambdaQuery.orderByDesc(Moment::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }

    /**
     * 删除动态
     */
    @GetMapping("/del")
    @LoginCheck
    public R deleteMoment(@RequestParam("id") Long id) {
        Long userId = BlogUtil.getUserId();
        Moment moment = new Moment();
        moment.setId(id);
        moment.setDelBy(userId);
        moment.setDelTime(LocalDateTime.now());
        moment.setIsDel(1);
        momentService.updateById(moment);
        return R.success();
    }

    /**
     * 查询动态列表
     */
    @PostMapping("/list")
    public R<BaseReqVO> listMoment(@RequestBody BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<Moment> lambdaQuery = momentService.lambdaQuery();
        lambdaQuery.eq(Moment::getStatus, 1);
        lambdaQuery.eq(Moment::getSource, baseReqVO.getUserId());

        if (baseReqVO.getUserId() == null) {
            if (BlogUtil.getUserId() != null) {
                lambdaQuery.eq(Moment::getUserId, BlogUtil.getUserId());
                lambdaQuery.eq(Moment::getVisibility, VisibilityEnum.PUBLIC.code());
            } else {
                lambdaQuery.eq(Moment::getUserId, BlogUtil.getAdminUser().getId());
                lambdaQuery.eq(Moment::getVisibility, VisibilityEnum.PUBLIC.code());
            }
        } else {
            if (!baseReqVO.getUserId().equals(BlogUtil.getUserId())) {
                lambdaQuery.eq(Moment::getVisibility, VisibilityEnum.PUBLIC.code());
            }
            lambdaQuery.eq(Moment::getUserId, baseReqVO.getUserId());
        }

        lambdaQuery.orderByDesc(Moment::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }
}
