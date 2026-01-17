package io.charqe.qs.modules.article.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.article.controller.web.dto.ArticlePageDTO;
import io.charqe.qs.modules.article.controller.web.vo.ArticlePageVO;
import io.charqe.qs.modules.article.controller.web.vo.ArticleVO;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.article.controller.web.dto.ArticleDTO;
import io.charqe.qs.modules.article.domain.pojo.Article;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
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
