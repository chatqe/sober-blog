package io.charqe.qs.modules.article.controller.web;


import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.article.controller.web.dto.ArticleDTO;
import io.charqe.qs.modules.article.controller.web.dto.ArticlePageDTO;
import io.charqe.qs.modules.article.controller.web.vo.ArticleVO;
import io.charqe.qs.modules.article.service.ArticleService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.cache.PoetryCache;
import io.charqe.qs.utils.cache.QingSiCache;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@Slf4j
@RestController
@RequestMapping("/article")
@Tag(name = "文章管理", description = "文章")
public class ArticleController {

    @Resource
    private ArticleService articleService;

    @Resource
    private QingSiCache qingSiCache;


    /**
     * 保存文章
     */
    @LoginCheck(1)
    @PostMapping("/saveArticle")
    public R saveArticle(@Validated @RequestBody ArticleVO articleVO) {
        PoetryCache.remove(CommonConst.USER_ARTICLE_LIST + BlogUtil.getUserId().toString());
        PoetryCache.remove(CommonConst.ARTICLE_LIST);
        PoetryCache.remove(CommonConst.SORT_ARTICLE_LIST);
        return articleService.saveArticle(articleVO);
    }


    /**
     * 删除文章
     */
    @GetMapping("/deleteArticle")
    @LoginCheck(1)
    public R deleteArticle(@RequestParam("id") Integer id) {
        PoetryCache.remove(CommonConst.USER_ARTICLE_LIST + BlogUtil.getUserId().toString());
        PoetryCache.remove(CommonConst.ARTICLE_LIST);
        PoetryCache.remove(CommonConst.SORT_ARTICLE_LIST);
        return articleService.deleteArticle(id);
    }


    /**
     * 更新文章
     */
    @PostMapping("/updateArticle")
    @LoginCheck(1)
    public R updateArticle(@Validated @RequestBody ArticleVO articleVO) {
        // PoetryCache.remove(CommonConst.ARTICLE_LIST);
        // PoetryCache.remove(CommonConst.SORT_ARTICLE_LIST);
        qingSiCache.del(CommonConst.ARTICLE_LIST);
        qingSiCache.del(CommonConst.SORT_ARTICLE_LIST);
        return articleService.updateArticle(articleVO);
    }


    // /**
    //  * 查询文章List
    //  */
    // @PostMapping("/listArticle")
    // public R<Page> listArticle(@RequestBody BaseReqVO baseReqVO) {
    //     return articleService.listArticle(baseReqVO);
    // }

    /**
     * 查询文章分页
     */
    @PostMapping("/listArticle")
    public R<PageResult> listPage(@RequestBody ArticlePageDTO dto) {
        return R.success(articleService.listArticle(dto));
    }

    /**
     * 查询分类文章List
     */
    @GetMapping("/listSortArticle")
    public R<Map<Integer, List<ArticleVO>>> listSortArticle() {
        return articleService.listSortArticle();
    }

    /**
     * 查询文章
     */
    @GetMapping("/getArticleById")
    public R<ArticleVO> getArticleById(@RequestParam("id") Integer id, @RequestParam(value = "password", required = false) String password) {
        return articleService.getArticleById(id, password);
    }

    /**
     * 点赞添加
     */
    @PostMapping("/addArticleLikeCount")
    public R addArticleLikeCount(@RequestBody ArticleDTO dto) {
        log.info("{}", dto);

        return R.success(articleService.addArticleLikeCount(dto));
    }

    /**
     * 点赞添加
     */
    @GetMapping("/checkHasLike")
    public R checkHasLike(@RequestParam String articleId) {
        log.info("articleId:{}", articleId);

        return R.success(articleService.checkHasLike(articleId));
    }

}


