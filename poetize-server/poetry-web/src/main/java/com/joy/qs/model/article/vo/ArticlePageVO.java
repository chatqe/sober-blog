package com.joy.qs.model.article.vo;

import com.joy.qs.common.PageResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
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
    private String sortId;
    private Integer labelId;

    public static  ArticlePageVO build(
            PageResult baseResult, // 父类 PageResult 实例
            boolean desc ,
            String searchKey,
            String sortId,
            Integer labelId

    ) {
        ArticlePageVO result = new ArticlePageVO();

        // 1. 拷贝父类字段（推荐用 Spring 的 BeanUtils，简洁且不易漏字段）
        BeanUtils.copyProperties(baseResult, result);

        // 2. 单独 set 子类额外属性（工厂方法内统一赋值）
        result.setDesc(desc);
        result.setSearchKey(searchKey);
        result.setSortId(sortId);
        result.setLabelId(labelId);
        return result;
    }
}
