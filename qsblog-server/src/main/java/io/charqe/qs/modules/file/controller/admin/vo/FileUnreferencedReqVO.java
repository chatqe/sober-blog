package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 未引用清单入参：{@code ref_count=0} 且创建时间早于 {@code days} 天的文件。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileUnreferencedReqVO extends FileListReqVO {

    /** 只统计创建超过 N 天的文件，避免把刚上传待引用的素材列为可清理；0 表示不设下限 */
    @Min(value = 0, message = "天数不能为负")
    @Max(value = 365, message = "天数不超过 365")
    private Integer days = 7;

    @Override
    public void normalize() {
        super.normalize();
        if (days == null || days < 0) {
            days = 7;
        }
        if (days > 365) {
            days = 365;
        }
    }
}
