package io.charqe.qs.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 下拉选项VO（主数据选项的统一精简投影）
 *
 * <p>分类、标签这类有主键、可增删、存在级联关系的主数据，其下拉数据源使用本VO，
 * 不复用 {@code DictVO}（字典面向值域封闭的枚举，value 是代码值），也不返回实体全量字段。
 *
 * <p>字段约束见 docs/开发规范-ID序列化与下拉选项.md：
 * id 与 categoryId 序列化为字符串，且 {@code value} 恒等于 {@code String(id)}；
 * status / isShow / disabled 仅后台口径（scope=all）返回。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "下拉选项VO")
public class OptionVO {

    @Schema(description = "选项ID（字符串）", example = "5")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "显示文案", example = "一些虫子")
    private String label;

    @Schema(description = "选择值，恒等于 String(id)", example = "5")
    private String value;

    @Schema(description = "所属分类ID（标签选项返回，分类选项不返回）", example = "1")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long categoryId;

    @Schema(description = "排序权重（接口已按 sort、id 升序返回）", example = "1")
    private Integer sort;

    @Schema(description = "状态：0-停用 1-启用（仅后台口径返回）", example = "1")
    private Integer status;

    @Schema(description = "是否展示：0-隐藏 1-展示（仅后台口径返回）", example = "1")
    private Integer isShow;

    @Schema(description = "是否不可选（仅后台口径返回，由 status=0 派生，前端可直接绑 el-option 的 disabled）", example = "false")
    private Boolean disabled;

    /**
     * 构建基础选项：id / label / value 三者绑定，杜绝把描述文本当成选择值
     *
     * @param id    实体主键
     * @param label 显示文案
     * @param sort  排序权重，可为空
     * @return 仅含基础字段的选项
     */
    public static OptionVO of(Long id, String label, Integer sort) {
        OptionVO vo = new OptionVO();
        vo.setId(id);
        vo.setLabel(label);
        vo.setValue(id == null ? null : String.valueOf(id));
        vo.setSort(sort);
        return vo;
    }
}
