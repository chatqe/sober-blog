package io.charqe.qs.modules.weblinks.convert;

import io.charqe.qs.modules.weblinks.controller.web.vo.WeblinksVO;
import io.charqe.qs.modules.weblinks.domain.pojo.Weblinks;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Weblinks 实体与 VO 之间的转换器
 *
 * @author Joy
 * @since 2026-06-21
 */
@Mapper(componentModel = "spring")
public interface WeblinksConvert {

    WeblinksConvert INSTANCE = Mappers.getMapper(WeblinksConvert.class);

    /**
     * VO -> Entity
     * WeblinksVO.introduction 对应 Weblinks.getDesc
     */
    @Mapping(target = "getDesc", source = "introduction")
    Weblinks toEntity(WeblinksVO vo);

    /**
     * Entity -> VO
     * Weblinks.getDesc 对应 WeblinksVO.introduction
     */
    @Mapping(target = "introduction", source = "getDesc")
    WeblinksVO toVO(Weblinks entity);

    /**
     * 批量转换
     */
    List<WeblinksVO> toVOList(List<Weblinks> entities);
}
