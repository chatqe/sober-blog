package io.charqe.qs.modules.file.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件业务关系表
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("file_biz_ref")
public class FileBizRef extends BaseDO {


    /** 文件ID（file_info.id） */
    private Long fileId;

    /** 业务ID */
    private Long bizId;

    /** 业务类型 */
    private Integer bizType;

    /** 业务字段（同一业务对象的多个挂载点，如 cover/content，默认空串） */
    private String bizField;

    /** 排序序号 */
    private Integer sort;

    /** 1-正常 0-隐藏 */
    private Integer status;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

}