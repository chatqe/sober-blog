package io.charqe.qs.modules.category.convert;

import io.charqe.qs.modules.category.controller.admin.dto.CategorySaveReqDTO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryDetailVO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryListVO;
import io.charqe.qs.modules.category.controller.web.vo.CategoryWebVO;
import io.charqe.qs.modules.category.domain.pojo.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Category 实体与 DTO/VO 之间的转换器
 *
 * @author Joy
 * @since 2026-09-11
 */
@Mapper(componentModel = "spring")
public interface CategoryConvert {

    CategoryConvert INSTANCE = Mappers.getMapper(CategoryConvert.class);

    /**
     * DTO → Entity：保存/更新时使用
     */
    @Mapping(source = "desc", target = "cateDesc")
    Category toEntity(CategorySaveReqDTO dto);

    /**
     * Entity → ListVO
     */
    @Mapping(source = "cateDesc", target = "desc")
    CategoryListVO toListVO(Category entity);

    /**
     * Entity → DetailVO
     */
    @Mapping(source = "cateDesc", target = "desc")
    CategoryDetailVO toDetailVO(Category entity);

    /**
     * Entity → WebVO
     */
    @Mapping(source = "cateDesc", target = "desc")
    CategoryWebVO toWebVO(Category entity);

    /**
     * Entity 批量转 ListVO
     */
    List<CategoryListVO> toListVOList(List<Category> entities);
}
