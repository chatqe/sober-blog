package io.charqe.qs.modules.article.controller.admin;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.article.controller.web.vo.ArticleVO;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.article.service.ArticleService;
import io.charqe.qs.utils.BlogUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 后台文章 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "管理后台-文章管理", description = "后台文章相关")
public class AdminArticleController {

    @Resource
    private ArticleService articleService;

    /**
     * 用户查询文章
     */
    @PostMapping("/article/user/list")
    @LoginCheck(1)
    public R<Page> listUserArticle(@RequestBody BaseReqVO baseReqVO) {
        return articleService.listAdminArticle(baseReqVO, false);
    }

    /**
     * Boss查询文章
     */
    @PostMapping("/article/boss/list")
    @LoginCheck(0)
    public R<Page> listBossArticle(@RequestBody BaseReqVO baseReqVO) {
        return articleService.listAdminArticle(baseReqVO, true);
    }

    /**
     * 修改文章状态
     */
    @GetMapping("/article/changeArticleStatus")
    @LoginCheck(1)
    public R changeArticleStatus(@RequestParam("articleId") Integer articleId,
                                 @RequestParam(value = "viewStatus", required = false) Boolean viewStatus,
                                 @RequestParam(value = "commentStatus", required = false) Boolean commentStatus,
                                 @RequestParam(value = "recommendStatus", required = false) Boolean recommendStatus) {
        LambdaUpdateChainWrapper<Article> updateChainWrapper = articleService.lambdaUpdate()
                .eq(Article::getId, articleId)
                .eq(Article::getUserId, BlogUtil.getUserId());
        if (viewStatus != null) {
            updateChainWrapper.set(Article::getViewStatus, viewStatus);
        }
        if (commentStatus != null) {
            updateChainWrapper.set(Article::getCommentStatus, commentStatus);
        }
        if (recommendStatus != null) {
            updateChainWrapper.set(Article::getRecommendStatus, recommendStatus);
        }
        updateChainWrapper.update();
        return R.success();
    }

    /**
     * 查询文章
     */
    @GetMapping("/article/getArticleById")
    @LoginCheck(1)
    public R<ArticleVO> getArticleByIdForUser(@RequestParam("id") Integer id) {
        return articleService.getArticleByIdForUser(id);
    }
}

