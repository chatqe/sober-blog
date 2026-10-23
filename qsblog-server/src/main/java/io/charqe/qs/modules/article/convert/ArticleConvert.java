package io.charqe.qs.modules.article.convert;


import io.charqe.qs.modules.article.controller.admin.vo.AdminArticleVO;
import io.charqe.qs.modules.article.controller.web.vo.ArticleVO;
import io.charqe.qs.modules.article.domain.pojo.Article;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MapStruct转换接口：编译期自动生成实现类，无需手动编写
 *
 * @author Joy
 * @since 2026-01-21 19:32
 */
@Mapper
public interface ArticleConvert {

    // 单例实例（如果不使用Spring注入，可直接用这个）
    ArticleConvert INSTANCE = Mappers.getMapper(ArticleConvert.class);

    /**
     * reqVO → entity：新增用户时的转换
     * 说明：字段名一致时无需配置@Mapping，不一致时需指定
     */
    Article convert(ArticleVO vo);

    /**
     * entity → respVO：返回给前端的转换
     */
    ArticleVO convert(Article article);

    /**
     * entity → AdminArticleVO：后台管理列表项转换
     * 仅拷贝基础字段，categoryName/tagName/authorName/authorAvatar
     * 由调用方通过查询关联填充
     */
    // @Mapping(source = "recommendStatus", target = "recommendStatus", qualifiedByName = "booleanToInteger")
    // @Mapping(source = "viewStatus", target = "viewStatus", qualifiedByName = "booleanToInteger")
    // @Mapping(source = "commentStatus", target = "commentStatus", qualifiedByName = "booleanToInteger")
    AdminArticleVO toAdminVO(Article article);

    /**
     * Boolean → Integer 转换（0/1）
     */
    @Named("booleanToInteger")
    default Integer booleanToInteger(Boolean value) {
        return value != null && value ? 1 : 0;
    }

    /**
     * Integer → Boolean 转换
     */
    @Named("integerToBoolean")
    default Boolean integerToBoolean(Integer value) {
        return value != null && value == 1;
    }

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
