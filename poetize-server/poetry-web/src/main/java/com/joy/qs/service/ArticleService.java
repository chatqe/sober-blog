package com.joy.qs.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joy.qs.common.R;
import com.joy.qs.vo.ArticleVO;
import com.joy.qs.vo.BaseReqVO;
import com.joy.qs.dto.ArticleDTO;
import com.joy.qs.entity.Article;
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

    R<Page> listArticle(BaseReqVO baseReqVO);

    R<ArticleVO> getArticleById(Integer id, String password);

    R<Page> listAdminArticle(BaseReqVO baseReqVO, Boolean isBoss);

    R<ArticleVO> getArticleByIdForUser(Integer id);

    R<Map<Integer, List<ArticleVO>>> listSortArticle();

    String addArticleLikeCount(ArticleDTO dto);

    boolean checkHasLike(String articleId);
}
