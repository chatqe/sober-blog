package io.charqe.qs.modules.file.convert;


import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileDetailRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct转换接口：编译期自动生成实现类，无需手动编写
 *
 * @author Joy
 * @since 2026-01-21 19:32
 */
@Mapper
public interface FileConvert {

    // 单例实例（如果不使用Spring注入，可直接用这个）
    FileConvert INSTANCE = Mappers.getMapper(FileConvert.class);

    /**
     * reqVO → entity：按同名字段复制，业务元数据由服务层派生
     */
    FileInfo convert(FileCreateReqVO reqVO);

    /**
     * entity → respVO：返回给前端的转换
     */
    FileRespVO convert(FileInfo file);

    /**
     * entity → 详情VO：只复制登记字段，访问地址与派生图由服务层推导
     */
    FileDetailRespVO convertDetail(FileInfo file);

}
