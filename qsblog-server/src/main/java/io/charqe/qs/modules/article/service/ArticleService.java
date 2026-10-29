package io.charqe.qs.modules.article.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.article.controller.admin.dto.AdminArticlePageReqDTO;
import io.charqe.qs.modules.article.controller.admin.vo.AdminArticlePageRespVO;
import io.charqe.qs.modules.article.controller.web.dto.ArticlePageDTO;
import io.charqe.qs.modules.article.controller.web.vo.ArticlePageVO;
import io.charqe.qs.modules.article.controller.web.vo.ArticleVO;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.article.controller.web.dto.ArticleDTO;
import io.charqe.qs.modules.article.domain.dto.ArticleStatsDTO;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.common.model.mp.CoreService;

import java.util.List;
import java.util.Map;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
public interface ArticleService extends CoreService<Article> {

    R saveArticle(ArticleVO articleVO);

    R deleteArticle(Long id);

    R updateArticle(ArticleVO articleVO);

    // R<Page> listArticle(BaseReqVO baseReqVO);
    ArticlePageVO listArticle(ArticlePageDTO dto);

    /**
     * 获取加密文章
     *
     * @param id
     * @return
     */
    R<ArticleVO> getArticleWithPwd(Long id, String password);

    R<PageResult> listAdminArticle(BaseReqVO baseReqVO, Boolean isBoss);

    R<ArticleVO> getArticleById(Long id);

    R<Map<Long, List<ArticleVO>>> listCategoryArticle();

    String addArticleLikeCount(ArticleDTO dto);

    boolean checkHasLike(String articleId);

    /**
     * 更新文章状态
     *
     * @param articleId
     * @param viewStatus
     * @param commentStatus
     * @param recommendStatus
     */
    void setArticleStatus(Long articleId, Boolean viewStatus, Boolean commentStatus, Boolean recommendStatus, Integer publishStatus);

    AdminArticlePageRespVO pageList(AdminArticlePageReqDTO dto);

    /**
     * 获取文章统计（全部未删除文章的总数、累计浏览量、累计点赞数）
     *
     * @return 文章统计聚合数据
     */
    ArticleStatsDTO getStats();
}
