package io.charqe.qs.modules.portal.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 站点概览 VO
 * 首页侧边栏展示：站点信息 + 文章/分类/浏览/点赞总量
 *
 * @author Joy
 * @since 2026-10-23
 */
@Data
@Schema(description = "站点概览")
public class PortalOverviewVO {

    @Schema(description = "网站名称")
    private String webName;

    @Schema(description = "网站简介")
    private String webTitle;

    @Schema(description = "站点 Logo")
    private String avatar;

    @Schema(description = "文章总数（未删除）")
    private Long articleTotal;

    @Schema(description = "可见分类总数")
    private Long categoryTotal;

    @Schema(description = "站点累计访问量")
    private Long historyAllCount;

    @Schema(description = "文章累计浏览量")
    private Long viewTotal;

    @Schema(description = "文章累计点赞数")
    private Long likeTotal;
}
