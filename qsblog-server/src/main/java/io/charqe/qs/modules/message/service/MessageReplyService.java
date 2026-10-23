package io.charqe.qs.modules.message.service;

import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.message.controller.admin.vo.AdminMessageVO;
import io.charqe.qs.modules.message.controller.web.vo.MessageReplyVO;
import io.charqe.qs.modules.message.domain.pojo.MessageReply;

import java.util.List;

public interface MessageReplyService extends CoreService<MessageReply> {

    void addReply(Long messageId, Long parentId, String nickname, String avatar,
                  String email, String website, String content, String ip, String ua);

    void likeReply(Long replyId, Long userId);

    void undislikeReply(Long replyId, Long userId);

    void adminReply(Long messageId, Long parentId, String nickname,
                    String content, String ip, String ua);
}
