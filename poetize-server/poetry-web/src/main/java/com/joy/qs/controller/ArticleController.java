package com.joy.qs.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joy.qs.aop.LoginCheck;
import com.joy.qs.common.R;
import com.joy.qs.vo.ArticleVO;
import com.joy.qs.vo.BaseReqVO;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.dto.ArticleDTO;
import com.joy.qs.service.ArticleService;
import com.joy.qs.utils.PoetryUtil;
import com.joy.qs.utils.cache.PoetryCache;
import com.joy.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Resource
    private QingSiCache qingSiCache;


    /**
     * 保存文章
     */
    @LoginCheck(1)
    @PostMapping("/saveArticle")
    public R saveArticle(@Validated @RequestBody ArticleVO articleVO) {
        PoetryCache.remove(CommonConst.USER_ARTICLE_LIST + PoetryUtil.getUserId().toString());
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
        PoetryCache.remove(CommonConst.USER_ARTICLE_LIST + PoetryUtil.getUserId().toString());
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
        qingSiCache.delete(CommonConst.ARTICLE_LIST);
        qingSiCache.delete(CommonConst.SORT_ARTICLE_LIST);
        return articleService.updateArticle(articleVO);
    }


    /**
     * 查询文章List
     */
    @PostMapping("/listArticle")
    public R<Page> listArticle(@RequestBody BaseReqVO baseReqVO) {
        return articleService.listArticle(baseReqVO);
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

        return R.success(articleService.addArticleLikeCount(dto)) ;
    }

    /**
     * 点赞添加
     */
    @GetMapping("/checkHasLike")
    public R checkHasLike(@RequestParam String articleId) {
        log.info("articleId:{}", articleId);

        return R.success(articleService.checkHasLike(articleId)) ;
    }

}

