package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 补登记结果：逐条返回 {@code {ok, skipped, reason}}，单条失败不影响其余 key。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileOrphanBackfillRespVO {

    private Boolean dryRun;

    private Integer total;

    /** 本次实际写入的条数；dryRun 时恒为 0 */
    private Integer registered;

    private Integer skipped;

    private List<Item> list = new ArrayList<>();

    @Data
    public static class Item {

        private String key;

        /** true 表示已登记（或 dryRun 下将会登记） */
        private Boolean ok;

        private Boolean skipped;

        private String reason;

        /** 登记成功后的 file_info.id；dryRun 或跳过时为空 */
        private Long fileId;

        /** 按 key 首段推导的场景 */
        private String scene;
    }
}
