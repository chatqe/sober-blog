package com.joy.qs.model.article.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 文章dto
 *
 * @author sjq
 * @since 2025-03-20 22:34
 */
@Data
public class ArticleDTO {


    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long articleId;

    private Long userId;

    /**
     * 操作类型，1：点赞，0：取消点赞，
     */
    private Integer operation;

    private Long commentId;
}
