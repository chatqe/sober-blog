package io.charqe.qs.modules.message.convert;

import io.charqe.qs.modules.message.controller.admin.vo.AdminMessageVO;
import io.charqe.qs.modules.message.controller.web.vo.MessageVO;
import io.charqe.qs.modules.message.domain.pojo.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * 留言模块 MapStruct 转换接口
 */
@Mapper
public interface MessageConvert {

    MessageConvert INSTANCE = Mappers.getMapper(MessageConvert.class);

    MessageVO toVO(Message message);

    @Mapping(source = "isTop", target = "isTop", qualifiedByName = "integerToBoolean")
    AdminMessageVO toAdminVO(Message message);

    @Named("integerToBoolean")
    default Boolean integerToBoolean(Integer value) {
        return value != null && value == 1;
    }
}
