package io.charqe.qs.modules.file.convert;


import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MapStruct转换接口：编译期自动生成实现类，无需手动编写
 * componentModel = "spring"：生成的实现类会被Spring管理，可直接注入
 *
 * @author Joy
 * @since 2026-01-21 19:32
 */
@Mapper(componentModel = "spring")
public interface FileConvert {

    // 单例实例（如果不使用Spring注入，可直接用这个）
    FileConvert INSTANCE = Mappers.getMapper(FileConvert.class);

    /**
     * reqVO → entity：新增用户时的转换
     * 说明：字段名一致时无需配置@Mapping，不一致时需指定
     */
    FileInfo convert(FileCreateReqVO reqVO);

    /**
     * entity → respVO：返回给前端的转换
     */
    FileRespVO convert(FileInfo file);

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
