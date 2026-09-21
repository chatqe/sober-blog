package io.charqe.qs.modules.article.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.article.controller.admin.dto.AdminArticlePageReqDTO;
import io.charqe.qs.modules.article.controller.admin.vo.AdminArticlePageRespVO;
import io.charqe.qs.modules.article.controller.web.vo.ArticleVO;
import io.charqe.qs.modules.article.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@RestController
@RequestMapping("/admin/article")
@Tag(name = "adminArticle", description = "管理后台-文章管理")
public class AdminArticleController {

    @Resource
    private ArticleService articleService;


    /**
     *
     * 后台管理文章分页查询列表
     *
     *
     *
     * @return 文章分页列表
     */
    @Operation(summary = "后台管理文章分页查询列表", description = "管理后台查询文章列表，支持分页和排序")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功，返回文章分页列表"),
            @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/pageList")
    @LoginCheck(1)
    public R<AdminArticlePageRespVO> pageList(@RequestBody AdminArticlePageReqDTO dto) {
        return R.success(articleService.pageList(dto));
    }

    /**
     * 用户查询文章列表
     * <p>
     * 用于用户在管理后台查询自己的文章列表，支持分页和排序
     *
     * @param baseReqVO 分页查询参数
     * @return 文章分页列表
     */
    @Operation(summary = "用户查询文章列表", description = "用于用户在管理后台查询自己的文章列表，支持分页和排序")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功，返回文章分页列表"),
            @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/user/list")
    @LoginCheck(1)
    public R<Page> listUserArticle(
            @Parameter(name = "baseReqVO", description = "分页查询参数", required = true, example = "{\"pageNum\":1,\"pageSize\":10}")
            @RequestBody BaseReqVO baseReqVO) {
        return articleService.listAdminArticle(baseReqVO, false);
    }

    /**
     * 管理员查询所有文章列表
     * <p>
     * 用于管理员在管理后台查询所有用户的文章列表，支持分页和排序
     *
     * @param baseReqVO 分页查询参数
     * @return 文章分页列表
     */
    @Operation(summary = "管理员查询所有文章列表", description = "用于管理员在管理后台查询所有用户的文章列表，支持分页和排序")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功，返回文章分页列表"),
            @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/admin/list")
    @LoginCheck(0)
    public R<Page> listBossArticle(
            @Parameter(name = "baseReqVO", description = "分页查询参数", required = true, example = "{\"pageNum\":1,\"pageSize\":10}")
            @RequestBody BaseReqVO baseReqVO) {
        return articleService.listAdminArticle(baseReqVO, true);
    }

    /**
     * 修改文章状态
     * <p>
     * 用于修改文章的可见状态、评论状态和推荐状态
     *
     * @param articleId       文章ID
     * @param viewStatus      可见状态，true-公开，false-私有
     * @param commentStatus   评论状态，true-开启，false-关闭
     * @param recommendStatus 推荐状态，true-推荐，false-不推荐
     * @return 修改结果
     */
    @Operation(summary = "修改文章状态", description = "用于修改文章的可见状态、评论状态和推荐状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如文章ID为空"),
            @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
            @ApiResponse(responseCode = "403", description = "无权限修改该文章"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/changeArticleStatus")
    @LoginCheck(1)
    public R changeArticleStatus(
            @Parameter(name = "articleId", description = "文章ID", required = true, example = "1")
            @RequestParam("articleId") Long articleId,
            @Parameter(name = "viewStatus", description = "可见状态，true-公开，false-私有", required = false, example = "true")
            @RequestParam(value = "viewStatus", required = false) Boolean viewStatus,
            @Parameter(name = "commentStatus", description = "评论状态，true-开启，false-关闭", required = false, example = "true")
            @RequestParam(value = "commentStatus", required = false) Boolean commentStatus,
            @Parameter(name = "recommendStatus", description = "推荐状态，true-推荐，false-不推荐", required = false, example = "false")
            @RequestParam(value = "recommendStatus", required = false) Boolean recommendStatus) {

        articleService.setArticleStatus(articleId, viewStatus, commentStatus, recommendStatus);

        return R.success("更新成功");
    }

    /**
     * 查询文章详情（管理后台）
     * <p>
     * 用于在管理后台查询文章详情
     *
     * @param id 文章ID
     * @return 文章详情
     */
    @Operation(summary = "查询文章详情（管理后台）", description = "用于在管理后台查询文章详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功，返回文章详情"),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如文章ID为空"),
            @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
            @ApiResponse(responseCode = "403", description = "无权限查看该文章"),
            @ApiResponse(responseCode = "404", description = "文章不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/getArticleWithPwd")
    @LoginCheck(1)
    public R<ArticleVO> getArticleWithPwd(
            @Parameter(name = "id", description = "文章ID", required = true, example = "1")
            @NotNull
            @RequestParam("id") Long id,
            @Parameter(name = "pwd", description = "文章访问密码", required = false, example = "123456")
            @RequestParam(value = "pwd", required = false) String pwd) {

        return articleService.getArticleWithPwd(id, pwd);
    }
}

