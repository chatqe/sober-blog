package io.charqe.qs.modules.category.convert;

import io.charqe.qs.modules.category.controller.admin.dto.CategorySaveReqDTO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryDetailVO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryListVO;
import io.charqe.qs.modules.category.controller.web.vo.CategoryWebVO;
import io.charqe.qs.modules.category.domain.pojo.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    Category toEntity(CategorySaveReqDTO dto);

    /**
     * Entity → ListVO
     */
    CategoryListVO toListVO(Category entity);

    /**
     * Entity → DetailVO
     */
    CategoryDetailVO toDetailVO(Category entity);

    /**
     * Entity → WebVO
     */
    CategoryWebVO toWebVO(Category entity);

    /**
     * Entity 批量转 ListVO
     */
    List<CategoryListVO> toListVOList(List<Category> entities);

    /**
     * LocalDateTime → 格式化字符串
     */
    @Named("localDateTimeToString")
    default String localDateTimeToString(LocalDateTime time) {
        return time != null ? time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }
}
