package com.joy.qs.controller;


import com.joy.qs.aop.LoginCheck;
import com.joy.qs.aop.SaveCheck;
import com.joy.qs.common.R;
import com.joy.qs.vo.BaseReqVO;
import com.joy.qs.vo.CommentVO;
import com.joy.qs.service.CommentService;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.utils.CommonQuery;
import com.joy.qs.utils.cache.PoetryCache;
import com.joy.qs.utils.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 文章评论表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/comment")
public class CommentController {


    @Autowired
    private CommentService commentService;

    @Autowired
    private CommonQuery commonQuery;


    /**
     * 保存评论
     */
    @PostMapping("/saveComment")
    @LoginCheck
    @SaveCheck
    public R saveComment(@Validated @RequestBody CommentVO commentVO) {
        String content = StringUtil.removeHtml(commentVO.getCommentContent());
        if (!StringUtils.hasText(content)) {
            return R.fail("评论内容不合法！");
        }
        commentVO.setCommentContent(content);

        PoetryCache.remove(CommonConst.COMMENT_COUNT_CACHE + commentVO.getSource().toString() + "_" + commentVO.getType());
        return commentService.saveComment(commentVO);
    }


    /**
     * 删除评论
     */
    @GetMapping("/deleteComment")
    @LoginCheck
    public R deleteComment(@RequestParam("id") Integer id) {
        return commentService.deleteComment(id);
    }


    /**
     * 查询评论数量
     */
    @GetMapping("/getCommentCount")
    public R<Integer> getCommentCount(@RequestParam("source") Integer source, @RequestParam("type") String type) {
        return R.success(commonQuery.getCommentCount(source, type));
    }


    /**
     * 查询评论
     */
    @PostMapping("/listComment")
    public R<BaseReqVO> listComment(@RequestBody BaseReqVO baseReqVO) {
        return commentService.listComment(baseReqVO);
    }
}

