package io.charqe.qs.modules.message.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.message.controller.web.vo.MessageReplyVO;
import io.charqe.qs.modules.message.convert.MessageReplyConvert;
import io.charqe.qs.modules.message.domain.dao.MessageReplyMapper;
import io.charqe.qs.modules.message.domain.pojo.MessageReply;
import io.charqe.qs.utils.UaParseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageReplyServiceImpl extends CoreServiceImpl<MessageReplyMapper, MessageReply> implements MessageReplyService {

    @Resource
    private MessageReplyMapper messageReplyMapper;

    @Resource
    private UaParseUtil uaParseUtil;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addReply(Long messageId, Long parentId, String nickname, String avatar,
                         String email, String website, String content, String ip, String ua) {
        MessageReply reply = new MessageReply();
        reply.setMessageId(messageId);
        reply.setParentId(parentId);
        reply.setRootId(parentId != null && parentId > 0 ? parentId : 0L);
        reply.setReplyToId(parentId != null && parentId > 0 ? parentId : 0L);

        if (parentId != null && parentId > 0) {
            MessageReply parent = getById(parentId);
            if (parent != null) {
                reply.setReplyToName(parent.getNickname());
            }
        }

        reply.setUserId(null);
        reply.setNickname(StringUtils.hasText(nickname) ? truncate(nickname, 64) : "访客");
        reply.setAvatar(avatar);
        reply.setEmail(StringUtils.hasText(email) ? truncate(email, 128) : "");
        reply.setEmailHash(sha256(reply.getEmail()));
        reply.setWebsite(StringUtils.hasText(website) ? truncate(website, 256) : null);
        reply.setContent(StringUtils.hasText(content) ? truncate(content, 2000) : "");
        reply.setContentHash(sha256(reply.getContent()));
        reply.setIp(ip);
        reply.setUa(ua != null ? truncate(ua, 1024) : null);

        UaParseUtil.UaInfo uaInfo = uaParseUtil.parse(ua);
        reply.setBrowser(uaInfo.browser());
        reply.setOs(uaInfo.os());
        reply.setDeviceType(uaInfo.deviceType());

        reply.setStatus(1);
        reply.setLikeCount(0);
        reply.setDislikeCount(0);
        reply.setChildCnt(0);
        reply.setIsAdmin(0);
        save(reply);

        if (parentId != null && parentId > 0) {
            messageReplyMapper.incrementChildCount(parentId);
        }
        messageReplyMapper.incrementReplyCount(messageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeReply(Long replyId, Long userId) {
        MessageReply reply = getById(replyId);
        if (reply == null || reply.getIsDel() == 1) {
            throw new IllegalArgumentException("回复不存在");
        }
        messageReplyMapper.incrementLikeCount(replyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void undislikeReply(Long replyId, Long userId) {
        MessageReply reply = getById(replyId);
        if (reply == null || reply.getIsDel() == 1) {
            throw new IllegalArgumentException("回复不存在");
        }
        messageReplyMapper.decrementLikeCount(replyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminReply(Long messageId, Long parentId, String nickname,
                           String content, String ip, String ua) {
        addReply(messageId, parentId, nickname, null, "", "", content, ip, ua);

        MessageReply lastReply = lambdaQuery()
                .eq(MessageReply::getMessageId, messageId)
                .orderByDesc(MessageReply::getId)
                .last("LIMIT 1")
                .one();
        if (lastReply != null) {
            lastReply.setIsAdmin(1);
            updateById(lastReply);
        }
    }

    public List<MessageReplyVO> listRepliesByMessage(Long messageId) {
        List<MessageReply> replies = lambdaQuery()
                .eq(MessageReply::getMessageId, messageId)
                .eq(MessageReply::getStatus, 1)
                .eq(MessageReply::getIsDel, 0)
                .orderByAsc(MessageReply::getCreateTime)
                .list();

        if (CollectionUtils.isEmpty(replies)) {
            return Collections.emptyList();
        }
        return replies.stream().map(MessageReplyConvert.INSTANCE::toVO).collect(Collectors.toList());
    }

    private String sha256(String input) {
        if (!StringUtils.hasText(input)) return null;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("SHA256 计算失败", e);
            return null;
        }
    }

    private String truncate(String str, int maxLen) {
        if (!StringUtils.hasText(str)) return str;
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
