package io.charqe.qs.modules.article.controller.web.vo;

import io.charqe.qs.common.model.PageResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * 文章分页响应
 *
 * @author sjq
 * @since 2025-11-22 21:05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticlePageVO extends PageResult {
    private List list;
    private boolean desc = true;
    private String searchKey;
    private Long categoryId;
    private Long tagId;

    public static  ArticlePageVO build(
            PageResult baseResult, // 父类 PageResult 实例
            boolean desc ,
            String searchKey,
            Long categoryId,
            Long tagId

    ) {
        ArticlePageVO result = new ArticlePageVO();

        // 1. 拷贝父类字段（推荐用 Spring 的 BeanUtils，简洁且不易漏字段）
        BeanUtils.copyProperties(baseResult, result);

        // 2. 单独 set 子类额外属性（工厂方法内统一赋值）
        result.setDesc(desc);
        result.setSearchKey(searchKey);
        result.setCategoryId(categoryId);
        result.setTagId(tagId);
        return result;
    }
}


