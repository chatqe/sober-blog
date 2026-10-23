package io.charqe.qs.modules.message.convert;

import io.charqe.qs.modules.message.controller.web.vo.MessageReplyVO;
import io.charqe.qs.modules.message.domain.pojo.MessageReply;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MessageReplyConvert {

    MessageReplyConvert INSTANCE = Mappers.getMapper(MessageReplyConvert.class);

    MessageReplyVO toVO(MessageReply reply);
}
