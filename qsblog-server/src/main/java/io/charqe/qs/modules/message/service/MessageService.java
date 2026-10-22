package io.charqe.qs.modules.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.message.controller.admin.dto.AdminMessagePageReqDTO;
import io.charqe.qs.modules.message.controller.admin.vo.AdminMessageVO;
import io.charqe.qs.modules.message.controller.web.dto.MessageQueryDTO;
import io.charqe.qs.modules.message.controller.web.dto.MessageSaveDTO;
import io.charqe.qs.modules.message.controller.web.vo.MessageReplyVO;
import io.charqe.qs.modules.message.controller.web.vo.MessageVO;
import io.charqe.qs.modules.message.domain.pojo.Message;

import java.util.List;

public interface MessageService extends CoreService<Message> {

    void saveMessage(MessageSaveDTO dto, String ip, String ua);

    PageResult<MessageVO> listMessages(MessageQueryDTO dto);

    void like(Long messageId, Long userId);

    void unlike(Long messageId, Long userId);

    void dislike(Long messageId, Long userId);

    void undislike(Long messageId, Long userId);

    boolean checkLike(Long messageId, Long userId);

    boolean checkDislike(Long messageId, Long userId);

    List<MessageReplyVO> getReplies(Long messageId);

    PageResult<AdminMessageVO> listAdminMessages(AdminMessagePageReqDTO dto);

    void auditMessage(Long id, Integer status);

    void toggleTop(Long id);
}
