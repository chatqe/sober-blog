package io.charqe.qs.modules.category.controller.web.dto;

import lombok.Data;

/**
 * 文章冗余分类名称DTO（只含 id + name）
 *
 * @author Joy
 * @since 2026-09-22
 */
@Data
public class ArticleCategoryNameDTO {
    private Long id;
    private String name;
}
