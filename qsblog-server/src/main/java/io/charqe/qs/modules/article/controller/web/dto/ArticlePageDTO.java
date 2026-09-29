package io.charqe.qs.modules.article.controller.web.dto;

import io.charqe.qs.common.model.PageQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章分页请求对象
 *
 * @author sjq
 * @since 2025-11-22 20:46
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticlePageDTO extends PageQuery {
    private Boolean desc = true;
    private String searchKey;
    private String categoryId;

    // 是否推荐[0:否，1:是]
    private Boolean recommendStatus;


    private Integer tagId;

    // private String articleSearch;

}

