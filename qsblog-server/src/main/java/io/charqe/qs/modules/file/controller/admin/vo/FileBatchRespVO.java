package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量清理结果：逐条返回 {@code {ok, skipped, reason}}，与契约修订 4 一致。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileBatchRespVO {

    private Boolean dryRun;

    private Boolean force;

    private Integer total;

    /** 实际执行（dryRun 时为将会执行）的条数 */
    private Integer deleted;

    private Integer skipped;

    private List<Item> list = new ArrayList<>();

    @Data
    public static class Item {

        private Long id;

        private Boolean ok;

        private Boolean skipped;

        private String reason;

        /** soft-delete 或 purge */
        private String action;
    }
}
