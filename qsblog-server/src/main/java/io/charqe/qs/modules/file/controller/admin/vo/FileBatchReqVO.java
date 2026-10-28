package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量清理入参：默认 dryRun 只预演，引用中的文件一律跳过。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileBatchReqVO {

    @NotEmpty(message = "文件ID不能为空")
    @Size(max = 100, message = "单次批量操作不超过 100 个文件")
    private List<Long> ids;

    /** 默认 true：只返回逐条预演结果，不改动数据 */
    private Boolean dryRun = true;

    /** false 移入回收站；true 永久删除（引用为 0 才执行） */
    private Boolean force = false;
}
