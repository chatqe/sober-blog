package io.charqe.qs.modules.im.http.controller;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.im.websocket.ImConfigConst;
import io.charqe.qs.modules.im.http.entity.ImChatUserMessage;
import io.charqe.qs.modules.im.http.service.ImChatUserMessageService;
import io.charqe.qs.modules.im.http.vo.UserMessageVO;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.CommonQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@RestController
@RequestMapping("/imChatUserMessage")
public class ImChatUserMessageController {

    @Autowired
    private ImChatUserMessageService imChatUserMessageService;

    @Autowired
    private CommonQuery commonQuery;

    /**
     * 获取系统消息（只获取前十条）
     */
    @GetMapping("/listSystemMessage")
    @LoginCheck
    public R<Page> listSystemMessage(@RequestParam(value = "current", defaultValue = "1") Long current,
                                     @RequestParam(value = "size", defaultValue = "10") Long size) {
        Page<ImChatUserMessage> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);

        LambdaQueryChainWrapper<ImChatUserMessage> lambdaQuery = imChatUserMessageService.lambdaQuery();
        lambdaQuery.select(ImChatUserMessage::getId, ImChatUserMessage::getContent, ImChatUserMessage::getCreateTime);

        lambdaQuery.eq(ImChatUserMessage::getFromId, ImConfigConst.DEFAULT_SYSTEM_MESSAGE_ID);
        lambdaQuery.eq(ImChatUserMessage::getToId, ImConfigConst.DEFAULT_SYSTEM_MESSAGE_ID);
        lambdaQuery.orderByDesc(ImChatUserMessage::getCreateTime);
        Page<ImChatUserMessage> result = lambdaQuery.page(page);
        List<ImChatUserMessage> records = result.getRecords();
        Collections.reverse(records);
        if (CollectionUtils.isEmpty(records)) {
            return R.success(result);
        } else {
            List<UserMessageVO> collect = records.stream().map(message -> {
                UserMessageVO userMessageVO = new UserMessageVO();
                userMessageVO.setContent(message.getContent());
                userMessageVO.setId(message.getId());
                userMessageVO.setCreateTime(message.getCreateTime());
                return userMessageVO;
            }).collect(Collectors.toList());
            Page<UserMessageVO> resultVO = new Page<>();
            resultVO.setRecords(collect);
            resultVO.setTotal(result.getTotal());
            resultVO.setCurrent(result.getCurrent());
            resultVO.setSize(result.getSize());
            return R.success(resultVO);
        }
    }


    /**
     * 管理员添加系统消息
     */
    @GetMapping("/saveSystemMessage")
    @LoginCheck(0)
    public R saveSystemMessage(@RequestParam("content") String content) {
        ImChatUserMessage userMessage = new ImChatUserMessage();
        userMessage.setContent(content);
        userMessage.setFromId(ImConfigConst.DEFAULT_SYSTEM_MESSAGE_ID);
        userMessage.setToId(ImConfigConst.DEFAULT_SYSTEM_MESSAGE_ID);
        userMessage.setMessageStatus(ImConfigConst.USER_MESSAGE_STATUS_TRUE);
        imChatUserMessageService.save(userMessage);
        return R.success();
    }


    /**
     * 删除系统消息
     */
    @GetMapping("/deleteSystemMessage")
    @LoginCheck(0)
    public R deleteSystemMessage(@RequestParam("id") Integer id) {
        imChatUserMessageService.removeById(id);
        return R.success();
    }

    /**
     * 获取朋友消息（只获取前四十条）
     */
    @GetMapping("/listFriendMessage")
    @LoginCheck
    public R<Page> listFriendMessage(@RequestParam(value = "current", defaultValue = "1") Long current,
                                     @RequestParam(value = "size", defaultValue = "40") Long size,
                                     @RequestParam(value = "friendId") Integer friendId) {
        Page<ImChatUserMessage> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);

        Integer userId = BlogUtil.getUserId();

        LambdaQueryChainWrapper<ImChatUserMessage> lambdaQuery = imChatUserMessageService.lambdaQuery();
        lambdaQuery.and(wrapper -> wrapper.eq(ImChatUserMessage::getFromId, userId).eq(ImChatUserMessage::getToId, friendId))
                .or(wrapper -> wrapper.eq(ImChatUserMessage::getFromId, friendId).eq(ImChatUserMessage::getToId, userId));
        lambdaQuery.orderByDesc(ImChatUserMessage::getCreateTime);
        Page<ImChatUserMessage> result = lambdaQuery.page(page);
        List<ImChatUserMessage> records = result.getRecords();
        Collections.reverse(records);
        if (CollectionUtils.isEmpty(records)) {
            return R.success(result);
        } else {
            List<UserMessageVO> collect = records.stream().map(message -> {
                UserMessageVO userMessageVO = new UserMessageVO();
                userMessageVO.setContent(message.getContent());
                userMessageVO.setFromId(message.getFromId());
                userMessageVO.setToId(message.getToId());
                userMessageVO.setMessageStatus(message.getMessageStatus());
                userMessageVO.setId(message.getId());
                userMessageVO.setCreateTime(message.getCreateTime());
                User from = commonQuery.getUser(message.getFromId());
                if (from != null) {
                    userMessageVO.setAvatar(from.getAvatar());
                }
                return userMessageVO;
            }).collect(Collectors.toList());
            Page<UserMessageVO> resultVO = new Page<>();
            resultVO.setRecords(collect);
            resultVO.setTotal(result.getTotal());
            resultVO.setCurrent(result.getCurrent());
            resultVO.setSize(result.getSize());
            return R.success(resultVO);
        }
    }
}



