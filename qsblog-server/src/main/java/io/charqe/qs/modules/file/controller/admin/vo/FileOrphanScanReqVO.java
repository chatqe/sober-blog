package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 孤儿扫描入参：按 key 前缀比对对象存储与登记表。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileOrphanScanReqVO {

    /** 裸 key 前缀，如 {@code avatar/}；空串表示整个 bucket */
    private String prefix;

    /** 单次最多扫描对象数，超出需缩小前缀或分页 */
    @Min(value = 1, message = "扫描条数至少为 1")
    @Max(value = 5000, message = "单次扫描不超过 5000 条")
    private Integer limit = 1000;

    /** 扫描本身只读，保留该标记以便前端统一处理批量动作 */
    private Boolean dryRun = true;

    public void normalize() {
        prefix = prefix == null ? "" : prefix.trim();
        if (limit == null || limit < 1) {
            limit = 1000;
        }
        if (limit > 5000) {
            limit = 5000;
        }
    }
}
