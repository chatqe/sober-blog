package io.charqe.qs.modules.behavior.convert;


import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorLog;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorState;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MapStruct转换接口：编译期自动生成实现类，无需手动编写
 *
 * @author Joy
 * @since 2026-03-08 16:07
 */
@Mapper
public interface BehaviorConvert {
    // 单例实例（如果不使用Spring注入，可直接用这个）
    BehaviorConvert INSTANCE = Mappers.getMapper(BehaviorConvert.class);

    /**
     * reqVO → entity：新增用户时的转换
     * 说明：字段名一致时无需配置@Mapping，不一致时需指定
     */
    BehaviorState convert(BehDTO reqVO);


    /**
     * state → log
     */
    BehaviorLog convert(BehaviorState beh);

    /**
     * log → state
     */
    BehaviorState convert(BehaviorLog beh);
    /**
     * entity → respVO：返回给前端的转换
     */
    // FileRespVO convert(BehaviorStateService beh);

    /**
     * 自定义转换方法：LocalDateTime → 格式化的String
     */
    @Named("localDateTimeToString")
    default String localDateTimeToString(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
