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

    // ID 一律 Long：全局约定下 Long 会同时被发为字符串与 string schema；
    // 曾经的 String/Integer 声明是为了绕开类型失配的局部补丁，现统一回 Long
    private Long categoryId;

    // 是否推荐[0:否，1:是]
    private Boolean recommendStatus;

    private Long tagId;

    // private String articleSearch;

}

