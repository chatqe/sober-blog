package io.charqe.qs.modules.article.domain.dto;

import lombok.Data;

/**
 * 文章统计聚合数据
 * 供跨域读模型（如门户概览）消费，口径为全部未删除文章
 * 统计类数值一律 Integer：Long 已被全局约定为 ID 专用类型（见 JacksonConfig）
 *
 * @author Joy
 * @since 2026-10-23
 */
@Data
public class ArticleStatsDTO {

    /**
     * 文章总数（未删除）
     */
    private Integer articleTotal;

    /**
     * 累计浏览量
     */
    private Integer viewTotal;

    /**
     * 累计点赞数
     */
    private Integer likeTotal;
}
