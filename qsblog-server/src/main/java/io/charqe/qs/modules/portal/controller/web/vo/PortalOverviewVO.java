package io.charqe.qs.modules.portal.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 站点概览 VO
 * 首页侧边栏展示：站点信息 + 文章/分类/浏览/点赞总量
 * 统计字段一律 Integer：Long 已被全局约定为 ID 专用类型（JacksonConfig + LongIdSchemaConverter），
 * 声明成 Long 会被下发为字符串，前端无法直接参与计数展示
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
    private Integer articleTotal;

    @Schema(description = "可见分类总数")
    private Integer categoryTotal;

    @Schema(description = "站点累计访问量")
    private Integer historyAllCount;

    @Schema(description = "文章累计浏览量")
    private Integer viewTotal;

    @Schema(description = "文章累计点赞数")
    private Integer likeTotal;
}
