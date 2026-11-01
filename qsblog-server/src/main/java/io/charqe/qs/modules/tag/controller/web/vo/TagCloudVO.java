package io.charqe.qs.modules.tag.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 标签云VO
 *
 * <p>供首页标签云展示与点击跳转使用：仅携带展示所需的 id 与 name，
 * 相比原 /tag/cloud 只返回名称，新增 id 使前端点击可直接按标签主键跳转 /tags?t=id。
 * id 为 Long，经全局 Jackson 配置序列化为字符串（全链路字符串 ID）。
 *
 * @author Joy
 * @since 2026-11-01
 */
@Data
@Schema(description = "标签云VO")
public class TagCloudVO {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;
}
