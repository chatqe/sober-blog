package io.charqe.qs.modules.comment.controller.web;


import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.comment.controller.web.vo.CommentVO;
import io.charqe.qs.modules.comment.service.CommentService;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.StringUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


/**
 * 评论控制
 *
 * @author Upjqs
 * @since 2025-12-31 19:57
 */

@RestController
@RequestMapping("/comment")
@Tag(name = "评论管理", description = "评论")
public class CommentController {


    @Autowired
    private CommentService commentService;

    @Autowired
    private CommonQuery commonQuery;

    @Resource
    private QingSiCache qingSiCache;


    /**
     * 保存评论
     */
    @PostMapping("/save")
    @LoginCheck
    @SaveCheck
    public R saveComment(@Validated @RequestBody CommentVO commentVO) {
        String content = StringUtil.removeHtml(commentVO.getCommentContent());
        if (!StringUtils.hasText(content)) {
            return R.fail("评论内容不合法！");
        }
        commentVO.setCommentContent(content);

        // PoetryCache.remove(CommonConst.COMMENT_COUNT_CACHE + commentVO.getSource().toString() + "_" + commentVO.getType());
        qingSiCache.del(CommonConst.COMMENT_COUNT_CACHE + commentVO.getSource().toString() + "_" + commentVO.getType());
        return commentService.saveComment(commentVO);
    }


    /**
     * 删除评论
     */
    @GetMapping("/delete")
    @LoginCheck
    public R deleteComment(@RequestParam("id") Integer id) {
        return commentService.deleteComment(id);
    }


    /**
     * 查询评论数量
     */
    @GetMapping("/getCount")
    public R<Integer> getCommentCount(@RequestParam("source") Integer source, @RequestParam("type") String type) {
        return R.success(commonQuery.getCommentCount(source, type));
    }


    /**
     * 查询评论
     */
    @PostMapping("/list")
    public R<BaseReqVO> listComment(@RequestBody BaseReqVO baseReqVO) {
        return commentService.listComment(baseReqVO);
    }
}



