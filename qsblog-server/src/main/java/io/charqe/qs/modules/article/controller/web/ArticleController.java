package io.charqe.qs.modules.article.controller.web;

import io.charqe.qs.modules.user.auth.SessionScope;


import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.article.controller.web.dto.ArticleDTO;
import io.charqe.qs.modules.article.controller.web.dto.ArticlePageDTO;
import io.charqe.qs.modules.article.controller.web.vo.ArticleVO;
import io.charqe.qs.modules.article.service.ArticleService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Slf4j
@RestController
@RequestMapping("/article")
@Tag(name = "article", description = "文章管理")
public class ArticleController {

    @Resource
    private ArticleService articleService;

    @Resource
    private QingSiCache qingSiCache;


    /**
     * 保存文章
     * <p>
     * 用于创建新文章，包括文章标题、内容、分类、标签等信息
     *
     * @param articleVO 文章信息
     * @return 保存结果
     */
    @Operation(summary = "保存文章", description = "用于创建新文章，包括文章标题、内容、分类、标签等信息。创建成功后会清除相关缓存，确保数据一致性。")
    @ApiResponse(responseCode = "200", description = "保存成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如文章标题或内容为空")
    @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    @PostMapping("/save")
    public R saveArticle(
            @Parameter(name = "articleVO", description = "文章信息对象", required = true, example = "{\"articleTitle\":\"测试文章\",\"articleContent\":\"文章内容\"}")
            @Validated @RequestBody ArticleVO articleVO) {
        qingSiCache.del(CommonConst.USER_ARTICLE_LIST + BlogUtil.getUserId());
        qingSiCache.del(CommonConst.ARTICLE_LIST);
        qingSiCache.del(CommonConst.CATEGORY_ARTICLE_LIST);
        return articleService.saveArticle(articleVO);
    }


    /**
     * 删除文章
     * <p>
     * 用于删除指定ID的文章
     *
     * @param id 文章ID
     * @return 删除结果
     */
    @Operation(summary = "删除文章", description = "用于删除指定ID的文章。删除成功后会清除相关缓存，确保数据一致性。")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如文章ID为空")
    @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/del")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R deleteArticle(
            @Parameter(name = "id", description = "文章ID", required = true, example = "1")
            @RequestParam("id") Long id) {
        qingSiCache.del(CommonConst.USER_ARTICLE_LIST + BlogUtil.getUserId());
        qingSiCache.del(CommonConst.ARTICLE_LIST);
        qingSiCache.del(CommonConst.CATEGORY_ARTICLE_LIST);
        return articleService.deleteArticle(id);
    }


    /**
     * 更新文章
     * <p>
     * 用于更新指定文章的信息，包括文章标题、内容、分类、标签等
     *
     * @param articleVO 文章信息
     * @return 更新结果
     */
    @Operation(summary = "更新文章", description = "用于更新指定文章的信息，包括文章标题、内容、分类、标签等。更新成功后会清除相关缓存，确保数据一致性。")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如文章标题或内容为空")
    @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/update")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R updateArticle(
            @Parameter(name = "articleVO", description = "文章信息对象", required = true, example = "{\"id\":1,\"articleTitle\":\"更新后的文章标题\",\"articleContent\":\"更新后的文章内容\"}")
            @Validated @RequestBody ArticleVO articleVO) {
        qingSiCache.del(CommonConst.ARTICLE_LIST);
        qingSiCache.del(CommonConst.CATEGORY_ARTICLE_LIST);
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
     * <p>
     * 用于获取文章列表，支持分页、排序等功能
     *
     * @param dto 分页查询参数
     * @return 文章分页列表
     */
    @Operation(summary = "查询文章分页", description = "用于获取文章列表，支持分页、排序等功能。返回结果包含文章基本信息、分类、标签等。")
    @ApiResponse(responseCode = "200", description = "查询成功，返回文章分页列表")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/listPage")
    public R<PageResult> listPage(
            @Parameter(name = "dto", description = "文章分页查询参数", required = true, example = "{\"pageNum\":1,\"pageSize\":10}")
            @RequestBody ArticlePageDTO dto) {
        return R.success(articleService.listArticle(dto));
    }

    /**
     * 查询分类文章列表
     * <p>
     * 用于按分类获取文章列表，返回按分类ID分组的文章数据
     *
     * @return 按分类分组的文章列表
     */
    @Operation(summary = "查询分类文章列表", description = "用于按分类获取文章列表，返回按分类ID分组的文章数据。适合用于首页或分类页展示文章列表。")
    @ApiResponse(responseCode = "200", description = "查询成功，返回按分类分组的文章列表")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/articleCategoryList")
    public R<Map<Long, List<ArticleVO>>> listCategoryArticle() {
        return articleService.listCategoryArticle();
    }

    /**
     * 查询文章详情
     * <p>
     * 用于获取指定ID的文章详情，支持密码访问加密文章
     *
     * @param id       文章ID
     * @param password 文章密码（加密文章时必填）
     * @return 文章详情
     */
    @Operation(summary = "查询文章详情", description = "用于获取指定ID的文章详情，支持密码访问加密文章。返回文章完整信息，包括标题、内容、分类、标签、点赞数、评论数等。")
    @ApiResponse(responseCode = "200", description = "查询成功，返回文章详情")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如文章ID为空")
    @ApiResponse(responseCode = "403", description = "文章密码错误或无访问权限")
    @ApiResponse(responseCode = "404", description = "文章不存在")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/getArticleById")
    public R<ArticleVO> getArticleById(
            @Parameter(name = "id", description = "文章ID", example = "1")
            @RequestParam(value = "id", required = false) Long id,
            @Parameter(name = "articleId", description = "文章ID（兼容 articleId 命名）", example = "1")
            @RequestParam(value = "articleId", required = false) Long articleId,
            @Parameter(name = "password", description = "文章密码（加密文章时必填）", required = false, example = "123456")
            @RequestParam(value = "password", required = false) String password) {
        // 兼容前端两种参数命名：id / articleId（与 checkHasLike、点赞接口的 articleId 对齐）
        Long targetId = id != null ? id : articleId;
        if (targetId == null) {
            return R.fail("文章ID不能为空");
        }
        return articleService.getArticleWithPwd(targetId, password);
    }

    /**
     * 添加文章点赞数
     * <p>
     * 用于增加文章的点赞数，支持记录用户点赞状态
     *
     * @param dto 点赞请求参数
     * @return 点赞结果
     */
    @Operation(summary = "添加文章点赞数", description = "用于增加文章的点赞数，支持记录用户点赞状态。点赞成功后会更新文章的点赞数，并记录用户的点赞行为。")
    @ApiResponse(responseCode = "200", description = "点赞成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/addArticleLikeCount")
    public R addArticleLikeCount(
            @Parameter(name = "dto", description = "点赞请求参数", required = true, example = "{\"articleId\":\"1\"}")
            @RequestBody ArticleDTO dto) {
        log.info("{}", dto);

        return R.success(articleService.addArticleLikeCount(dto));
    }

    /**
     * 检查是否已点赞
     * <p>
     * 用于检查用户是否已点赞指定文章
     *
     * @param articleId 文章ID
     * @return 检查结果，true表示已点赞，false表示未点赞
     */
    @Operation(summary = "检查是否已点赞", description = "用于检查用户是否已点赞指定文章。返回布尔值，表示用户是否已点赞该文章。")
    @ApiResponse(responseCode = "200", description = "检查成功，返回点赞状态")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如文章ID为空")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/checkHasLike")
    public R checkHasLike(
            @Parameter(name = "articleId", description = "文章ID", required = true, example = "1")
            @RequestParam String articleId) {
        log.info("articleId:{}", articleId);

        return R.success(articleService.checkHasLike(articleId));
    }

}


