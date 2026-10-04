package io.charqe.qs.modules.danmaku.convert;

import io.charqe.qs.modules.danmaku.controller.admin.vo.AdminDanmakuVO;
import io.charqe.qs.modules.danmaku.controller.web.vo.DanmakuVO;
import io.charqe.qs.modules.danmaku.domain.pojo.Danmaku;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * MapStruct转换接口：编译期自动生成实现类
 * componentModel = "spring"：生成的实现类会被Spring管理，可直接注入
 *
 * @author Joy
 * @since 2026-10-03
 */
@Mapper(componentModel = "spring")
public interface DanmakuConvert {

    DanmakuVO toVO(Danmaku danmaku);

    List<DanmakuVO> toVOList(List<Danmaku> danmakus);

    AdminDanmakuVO toAdminVO(Danmaku danmaku);

    List<AdminDanmakuVO> toAdminVOList(List<Danmaku> danmakus);
}
