package io.charqe.qs.modules.danmaku.convert;

import io.charqe.qs.modules.danmaku.controller.admin.vo.AdminDanmakuVO;
import io.charqe.qs.modules.danmaku.controller.web.vo.DanmakuVO;
import io.charqe.qs.modules.danmaku.domain.pojo.Danmaku;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * MapStruct转换接口：编译期自动生成实现类
 *
 * @author Joy
 * @since 2026-10-03
 */
@Mapper
public interface DanmakuConvert {

    DanmakuConvert INSTANCE = Mappers.getMapper(DanmakuConvert.class);

    DanmakuVO toVO(Danmaku danmaku);

    List<DanmakuVO> toVOList(List<Danmaku> danmakus);

    AdminDanmakuVO toAdminVO(Danmaku danmaku);

    List<AdminDanmakuVO> toAdminVOList(List<Danmaku> danmakus);
}
