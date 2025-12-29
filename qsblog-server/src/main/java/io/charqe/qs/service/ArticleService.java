package io.charqe.qs.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.R;
import io.charqe.qs.model.article.dto.ArticlePageDTO;
import io.charqe.qs.model.article.vo.ArticlePageVO;
import io.charqe.qs.model.article.vo.ArticleVO;
import io.charqe.qs.model.BaseReqVO;
import io.charqe.qs.model.article.dto.ArticleDTO;
import io.charqe.qs.model.article.pojo.Article;
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
