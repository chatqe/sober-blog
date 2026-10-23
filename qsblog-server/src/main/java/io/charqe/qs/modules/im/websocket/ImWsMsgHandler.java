package io.charqe.qs.modules.im.websocket;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.im.http.entity.ImChatGroupUser;
import io.charqe.qs.modules.im.http.entity.ImChatUserGroupMessage;
import io.charqe.qs.modules.im.http.entity.ImChatUserMessage;
import io.charqe.qs.modules.im.http.service.ImChatGroupUserService;
import io.charqe.qs.modules.im.http.service.ImChatUserMessageService;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.StrUtil;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.SessionScope;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.utils.lock.SetWithLock;
import org.tio.websocket.common.WsRequest;
import org.tio.websocket.common.WsResponse;
import org.tio.websocket.server.handler.IWsMsgHandler;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ImWsMsgHandler implements IWsMsgHandler {

    @Autowired
    private ImChatGroupUserService imChatGroupUserService;

    @Autowired
    private ImChatUserMessageService imChatUserMessageService;

    @Autowired
    private MessageCache messageCache;

    @Autowired
    private CommonQuery commonQuery;

    @Resource
    private AuthSessionService sessions;

    private record Connection(String token, Long userId, String sessionId) { }
    private final Map<ChannelContext, Connection> connections = new ConcurrentHashMap<>();

    public void forget(ChannelContext channel) {
        connections.remove(channel);
    }

    public boolean checkConnection(ChannelContext channel, boolean activity) {
        Connection connection = connections.get(channel);
        if (connection == null) return false;
        try {
            sessions.resolve(connection.token(), SessionScope.WEB, activity);
            return true;
        } catch (RuntimeException e) {
            forget(channel);
            Tio.remove(channel, "前台会话已失效");
            return false;
        }
    }

    @EventListener
    public void onSessionRevoked(AuthSessionService.Revoked event) {
        if (event.scope() == SessionScope.ADMIN) return;
        connections.forEach((channel, connection) -> {
            if (!Objects.equals(connection.userId(), event.userId())) return;
            if (event.sessionId() == null) {
                // 校验版本而非按用户全踢，保留安全事件之后建立的新会话连接。
                checkConnection(channel, false);
            } else if (event.sessionId().equals(connection.sessionId())) {
                forget(channel);
                Tio.remove(channel, "当前前台会话已退出");
            }
        });
    }

    @Scheduled(fixedDelay = 30000)
    public void checkIdleConnections() {
        // 心跳和定时检查不续期，同时发现其他实例触发的撤销。
        connections.keySet().forEach(channel -> checkConnection(channel, false));
    }

    /**
     * 握手时走这个方法，业务可以在这里获取cookie，request等
     * 对httpResponse参数进行补充并返回，如果返回null表示不想和对方建立连接
     * 对于大部分业务，该方法只需要一行代码：return httpResponse;
     */
    @Override
    public HttpResponse handshake(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) {
        String token = httpRequest.getParam(CommonConst.TOKEN_HEADER);

        if (!StringUtils.hasText(token)) {
            return null;
        }

        try {
            AuthSessionService.Resolved identity = sessions.resolve(token, SessionScope.WEB, false);
            connections.put(channelContext, new Connection(identity.token(), identity.user().getId(),
                    identity.session().getSessionId()));
            log.info("握手成功：用户ID：{}", identity.user().getId());
            return httpResponse;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 握手成功后触发该方法
     */
    @Override
    public void onAfterHandshaked(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) {
        if (!checkConnection(channelContext, false)) return;
        Connection connection = connections.get(channelContext);
        if (connection == null) return;
        User user;
        try {
            user = sessions.resolve(connection.token(), SessionScope.WEB, false).user();
        } catch (RuntimeException e) {
            forget(channelContext);
            Tio.remove(channelContext, "握手期间会话已失效");
            return;
        }
        Tio.closeUser(channelContext.tioConfig, user.getId().toString(), null);
        Tio.bindUser(channelContext, user.getId().toString());

        List<ImChatUserMessage> userMessages = imChatUserMessageService.lambdaQuery().eq(ImChatUserMessage::getToId, user.getId())
                .eq(ImChatUserMessage::getMessageStatus, ImConfigConst.USER_MESSAGE_STATUS_FALSE)
                .orderByAsc(ImChatUserMessage::getCreateTime).list();

        if (!CollectionUtils.isEmpty(userMessages)) {
            List<Long> ids = new ArrayList<>();
            userMessages.forEach(userMessage -> {
                ids.add(userMessage.getId());
                ImMessage imMessage = new ImMessage();
                imMessage.setContent(userMessage.getContent());
                imMessage.setFromId(userMessage.getFromId());
                imMessage.setToId(userMessage.getToId());
                imMessage.setMessageType(ImEnum.MESSAGE_TYPE_MSG_SINGLE.getCode());
                User friend = commonQuery.getUser(userMessage.getFromId());
                if (friend != null) {
                    imMessage.setAvatar(friend.getAvatar());
                }
                WsResponse wsResponse = WsResponse.fromText(JSON.toJSONString(imMessage), ImConfigConst.CHARSET);
                Tio.sendToUser(channelContext.tioConfig, userMessage.getToId().toString(), wsResponse);
            });
            imChatUserMessageService.lambdaUpdate().in(ImChatUserMessage::getId, ids)
                    .set(ImChatUserMessage::getMessageStatus, ImConfigConst.USER_MESSAGE_STATUS_TRUE).update();

        }

        LambdaQueryChainWrapper<ImChatGroupUser> lambdaQuery = imChatGroupUserService.lambdaQuery();
        lambdaQuery.select(ImChatGroupUser::getGroupId);
        lambdaQuery.eq(ImChatGroupUser::getUserId, user.getId());
        lambdaQuery.in(ImChatGroupUser::getUserStatus, ImConfigConst.GROUP_USER_STATUS_PASS, ImConfigConst.GROUP_USER_STATUS_SILENCE);
        List<ImChatGroupUser> groupUsers = lambdaQuery.list();
        if (!CollectionUtils.isEmpty(groupUsers)) {
            groupUsers.forEach(groupUser -> Tio.bindGroup(channelContext, groupUser.getGroupId().toString()));
        }
    }

    @Override
    public Object onBytes(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) {
        return null;
    }

    @Override
    public Object onClose(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) {
        forget(channelContext);
        Tio.remove(channelContext, "连接关闭");
        return null;
    }

    @Override
    public Object onText(WsRequest wsRequest, String text, ChannelContext channelContext) {
        if (!checkConnection(channelContext, false)) return null;
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            ImMessage imMessage = JSON.parseObject(text, ImMessage.class);

            String content = StrUtil.removeHtml(imMessage.getContent());
            if (!StringUtils.hasText(content)) {
                return null;
            }
            imMessage.setContent(content);
            Integer type = imMessage.getMessageType();
            if (type == null || (type != ImEnum.MESSAGE_TYPE_MSG_SINGLE.getCode()
                    && type != ImEnum.MESSAGE_TYPE_MSG_GROUP.getCode())) return null;
            if (!checkConnection(channelContext, true)) return null;

            WsResponse wsResponse = WsResponse.fromText(JSON.toJSONString(imMessage), ImConfigConst.CHARSET);
            if (imMessage.getMessageType().intValue() == ImEnum.MESSAGE_TYPE_MSG_SINGLE.getCode()) {
                //单聊
                ImChatUserMessage userMessage = new ImChatUserMessage();
                userMessage.setFromId(imMessage.getFromId());
                userMessage.setToId(imMessage.getToId());
                userMessage.setContent(imMessage.getContent());
                userMessage.setCreateTime(LocalDateTime.now());

                SetWithLock<ChannelContext> setWithLock = Tio.getByUserid(channelContext.tioConfig, imMessage.getToId().toString());
                if (setWithLock != null && setWithLock.size() > 0) {
                    Tio.sendToUser(channelContext.tioConfig, imMessage.getToId().toString(), wsResponse);
                    userMessage.setMessageStatus(ImConfigConst.USER_MESSAGE_STATUS_TRUE);
                } else {
                    userMessage.setMessageStatus(ImConfigConst.USER_MESSAGE_STATUS_FALSE);
                }
                messageCache.putUserMessage(userMessage);
                Tio.sendToUser(channelContext.tioConfig, imMessage.getFromId().toString(), wsResponse);
            } else if (imMessage.getMessageType().intValue() == ImEnum.MESSAGE_TYPE_MSG_GROUP.getCode()) {
                //群聊
                ImChatUserGroupMessage groupMessage = new ImChatUserGroupMessage();
                groupMessage.setContent(imMessage.getContent());
                groupMessage.setFromId(imMessage.getFromId());
                groupMessage.setGroupId(imMessage.getGroupId());
                groupMessage.setCreateTime(LocalDateTime.now());
                messageCache.putGroupMessage(groupMessage);

                SetWithLock<ChannelContext> setWithLock = Tio.getByGroup(channelContext.tioConfig, imMessage.getGroupId().toString());
                if (setWithLock != null && setWithLock.size() > 0) {
                    Tio.sendToGroup(channelContext.tioConfig, imMessage.getGroupId().toString(), wsResponse);
                }
            }
        } catch (Exception e) {
            log.error("解析消息失败：{}", e.getMessage());
        }
        //返回值是要发送给客户端的内容，一般都是返回null
        return null;
    }
}


