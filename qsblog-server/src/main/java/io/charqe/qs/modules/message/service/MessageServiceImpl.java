package io.charqe.qs.modules.message.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.message.controller.admin.dto.AdminMessagePageReqDTO;
import io.charqe.qs.modules.message.controller.admin.vo.AdminMessageVO;
import io.charqe.qs.modules.message.controller.web.dto.MessageQueryDTO;
import io.charqe.qs.modules.message.controller.web.dto.MessageSaveDTO;
import io.charqe.qs.modules.message.controller.web.vo.MessageReplyVO;
import io.charqe.qs.modules.message.controller.web.vo.MessageVO;
import io.charqe.qs.modules.message.convert.MessageConvert;
import io.charqe.qs.modules.message.convert.MessageReplyConvert;
import io.charqe.qs.modules.message.domain.dao.MessageMapper;
import io.charqe.qs.modules.message.domain.dao.MessageReplyMapper;
import io.charqe.qs.modules.message.domain.pojo.Message;
import io.charqe.qs.modules.message.domain.pojo.MessageReply;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.UaParseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageServiceImpl extends CoreServiceImpl<MessageMapper, Message> implements MessageService {

    @Resource
    private MessageReplyService messageReplyService;

    @Resource
    private MessageReplyMapper messageReplyMapper;

    @Resource
    private UaParseUtil uaParseUtil;

    @Resource
    private CommonQuery commonQuery;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(MessageSaveDTO dto, String ip, String ua) {
        Message msg = new Message();
        msg.setUserId(BlogUtil.getUserId());
        msg.setNickname(StringUtils.hasText(dto.getNickname()) ? truncate(dto.getNickname(), 64) : BlogUtil.getRandomName(ip));
        msg.setAvatar(dto.getAvatar());
        msg.setEmail(StringUtils.hasText(dto.getEmail()) ? truncate(dto.getEmail(), 128) : "");
        msg.setEmailHash(sha256(msg.getEmail()));
        msg.setWebsite(StringUtils.hasText(dto.getWebsite()) ? truncate(dto.getWebsite(), 256) : null);
        msg.setContent(StringUtils.hasText(dto.getContent()) ? truncate(dto.getContent(), 2000) : "");
        msg.setContentHash(sha256(msg.getContent()));
        msg.setIp(ip);
        msg.setUa(ua != null ? truncate(ua, 1024) : null);

        UaParseUtil.UaInfo uaInfo = uaParseUtil.parse(ua);
        msg.setBrowser(uaInfo.browser());
        msg.setOs(uaInfo.os());
        msg.setDeviceType(uaInfo.deviceType());

        msg.setStatus(1);
        msg.setLikeCount(0);
        msg.setDislikeCount(0);
        msg.setReplyCount(0);
        msg.setIsTop(0);
        if (StringUtils.hasText(ip)) {
            msg.setIpLocation(commonQuery.getIpRegion(ip));
        }
        save(msg);
    }

    @Override
    public PageResult<MessageVO> listMessages(MessageQueryDTO dto) {
        Page<Message> page = lambdaQuery()
                .eq(Message::getStatus, 1)
                .eq(Message::getIsDel, 0)
                .orderByDesc(Message::getIsTop)
                .orderByDesc(Message::getCreateTime)
                .page(new Page<>(dto.getPageNum(), dto.getPageSize()));

        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty();
        }
        return PageResult.fromMpPage(page, MessageConvert.INSTANCE::toVO);
    }

    @Override
    public void like(Long messageId, Long userId) {
        Message msg = getById(messageId);
        if (msg == null || msg.getIsDel() == 1) {
            throw new IllegalArgumentException("留言不存在");
        }
    }

    @Override
    public void unlike(Long messageId, Long userId) {
        Message msg = getById(messageId);
        if (msg == null || msg.getIsDel() == 1) {
            throw new IllegalArgumentException("留言不存在");
        }
    }

    @Override
    public void dislike(Long messageId, Long userId) {
        Message msg = getById(messageId);
        if (msg == null || msg.getIsDel() == 1) {
            throw new IllegalArgumentException("留言不存在");
        }
    }

    @Override
    public void undislike(Long messageId, Long userId) {
        Message msg = getById(messageId);
        if (msg == null || msg.getIsDel() == 1) {
            throw new IllegalArgumentException("留言不存在");
        }
    }

    @Override
    public boolean checkLike(Long messageId, Long userId) {
        return false;
    }

    @Override
    public boolean checkDislike(Long messageId, Long userId) {
        return false;
    }

    @Override
    public List<MessageReplyVO> getReplies(Long messageId) {
        List<MessageReply> replies = messageReplyService.lambdaQuery()
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

    @Override
    public PageResult<AdminMessageVO> listAdminMessages(AdminMessagePageReqDTO dto) {
        LambdaQueryWrapper<Message> wrapper = Lb.<Message>q()
                .eq(CharSequenceUtil.isNotBlank(dto.getKeyword()), Message::getContent, dto.getKeyword())
                .eq(Objects.nonNull(dto.getIsDel()), Message::getIsDel, dto.getIsDel())
                .eq(Objects.nonNull(dto.getIsTop()), Message::getIsTop, dto.getIsTop())
                .eq(CharSequenceUtil.isNotBlank(dto.getCity()), Message::getCity, dto.getCity());

        if (Objects.nonNull(dto.getStartTime()) && Objects.nonNull(dto.getEndTime())) {
            wrapper.between(Message::getCreateTime, dto.getStartTime(), dto.getEndTime());
        }

        if (Objects.nonNull(dto.getHasReply())) {
            if (Boolean.TRUE.equals(dto.getHasReply())) {
                wrapper.gt(Message::getReplyCount, 0);
            } else {
                wrapper.eq(Message::getReplyCount, 0);
            }
        }

        Page<Message> page = page(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);

        return PageResult.fromMpPage(page, MessageConvert.INSTANCE::toAdminVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditMessage(Long id, Integer status) {
        Message msg = getById(id);
        if (msg == null) {
            throw new IllegalArgumentException("留言不存在");
        }
        msg.setStatus(status);
        msg.setModeratorId(BlogUtil.getUserId());
        msg.setModerateTime(java.time.LocalDateTime.now());
        updateById(msg);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleTop(Long id) {
        Message msg = getById(id);
        if (msg == null) {
            throw new IllegalArgumentException("留言不存在");
        }
        msg.setIsTop(msg.getIsTop() == 0 ? 1 : 0);
        updateById(msg);
    }

    private String sha256(String input) {
        if (!StringUtils.hasText(input)) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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
