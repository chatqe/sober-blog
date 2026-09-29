package io.charqe.qs.modules.tag.controller.web.dto;

import lombok.Data;

/**
 * 文章冗余标签名称DTO（只含 id + name）
 *
 * @author Joy
 * @since 2026-09-22
 */
@Data
public class ArticleTagNameDTO {
    private Long id;
    private String name;
}
