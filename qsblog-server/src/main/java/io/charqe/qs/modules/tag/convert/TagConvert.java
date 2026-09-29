package io.charqe.qs.modules.tag.convert;

import io.charqe.qs.modules.tag.controller.admin.dto.TagSaveReqDTO;
import io.charqe.qs.modules.tag.controller.admin.vo.TagDetailVO;
import io.charqe.qs.modules.tag.controller.admin.vo.TagListVO;
import io.charqe.qs.modules.tag.controller.web.vo.TagWebVO;
import io.charqe.qs.modules.tag.domain.pojo.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Tag 实体与 DTO/VO 之间的转换器
 *
 * @author Joy
 * @since 2026-09-12
 */
@Mapper(componentModel = "spring")
public interface TagConvert {

    TagConvert INSTANCE = Mappers.getMapper(TagConvert.class);

    /**
     * DTO → Entity：保存/更新时使用
     */
    @Mapping(source = "desc", target = "tagDesc")
    Tag toEntity(TagSaveReqDTO dto);

    /**
     * Entity → ListVO
     */
    @Mapping(source = "tagDesc", target = "desc")
    TagListVO toListVO(Tag entity);

    /**
     * Entity → DetailVO
     */
    @Mapping(source = "tagDesc", target = "desc")
    TagDetailVO toDetailVO(Tag entity);

    /**
     * Entity → WebVO
     */
    @Mapping(source = "tagDesc", target = "desc")
    TagWebVO toWebVO(Tag entity);

    /**
     * Entity 批量转 ListVO
     */
    List<TagListVO> toListVOList(List<Tag> entities);
}
