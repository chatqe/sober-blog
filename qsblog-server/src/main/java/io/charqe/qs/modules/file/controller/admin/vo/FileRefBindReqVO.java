package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 文件引用绑定入参。
 * <p>
 * 业务对象（文章/动态/评论等）保存时登记其对文件的引用，重复绑定同一
 * {@code fileId + bizType + bizId + bizField} 视为幂等成功。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileRefBindReqVO {

    /** 文件 ID（file_info.id） */
    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    /** 业务类型，取值见 BizTypeEnum */
    @NotNull(message = "业务类型不能为空")
    @Min(value = 1, message = "业务类型不合法")
    private Integer bizType;

    /** 业务对象 ID */
    @NotNull(message = "业务ID不能为空")
    @Min(value = 1, message = "业务ID不合法")
    private Long bizId;

    /** 业务字段：同一对象的多个挂载点（cover/content 等），默认空串 */
    @Pattern(regexp = "^[A-Za-z0-9_-]{0,32}$", message = "业务字段只能包含字母、数字、下划线或短横线，且不超过 32 位")
    private String bizField;

    /** 排序序号，默认 0 */
    private Integer sort;

    /** 1-正常 0-隐藏，默认 1 */
    private Integer status;
}
