package io.charqe.qs.modules.user.convert;

import io.charqe.qs.modules.user.controller.web.dto.LoginUserInfo;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct转换接口：编译期自动生成实现类，无需手动编写
 *
 * @author Joy
 * @since 2026-03-18 19:04
 */

@Mapper
public interface UserConvert {
    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    LoginUserInfo convert(User user);
}
