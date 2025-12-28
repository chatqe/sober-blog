package com.joy.qs.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joy.qs.common.R;
import com.joy.qs.model.article.dto.ArticlePageDTO;
import com.joy.qs.model.article.vo.ArticlePageVO;
import com.joy.qs.model.article.vo.ArticleVO;
import com.joy.qs.model.BaseReqVO;
import com.joy.qs.model.article.dto.ArticleDTO;
import com.joy.qs.model.article.pojo.Article;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章表 服务类
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
public interface ArticleService extends IService<Article> {

    R saveArticle(ArticleVO articleVO);

    R deleteArticle(Integer id);

    R updateArticle(ArticleVO articleVO);

    // R<Page> listArticle(BaseReqVO baseReqVO);
    ArticlePageVO listArticle(ArticlePageDTO dto);

    R<ArticleVO> getArticleById(Integer id, String password);

    R<Page> listAdminArticle(BaseReqVO baseReqVO, Boolean isBoss);

    R<ArticleVO> getArticleByIdForUser(Integer id);

    R<Map<Integer, List<ArticleVO>>> listSortArticle();

    String addArticleLikeCount(ArticleDTO dto);

    boolean checkHasLike(String articleId);
    

}