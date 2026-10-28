package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 孤儿扫描结果：{@code scanned} 为本次扫描对象数，{@code orphanCount} 为未登记数。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileOrphanScanRespVO {

    private Boolean dryRun;

    private String storeType;

    private String prefix;

    /** 本次实际扫描（返回）的对象数 */
    private Integer scanned;

    /** 已登记的对象数 */
    private Integer registered;

    /** 未登记的对象数 */
    private Integer orphanCount;

    /** 是否还有未返回的对象（受 limit 限制） */
    private Boolean truncated;

    private List<Item> list = new ArrayList<>();

    @Data
    public static class Item {

        /** bucket 内裸 key，可直接用于补登记 */
        private String key;

        private Long size;

        private String mimeType;

        private Long putTime;
    }
}
