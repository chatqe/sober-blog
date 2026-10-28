package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 补登记入参：把存储中存在但登记表缺失的对象补录为 system 资源。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileOrphanBackfillReqVO {

    /** 裸 key 或 CDN 完整地址，服务端统一归一化为裸 key */
    @NotEmpty(message = "待补登记的 key 不能为空")
    @Size(max = 100, message = "单次补登记不超过 100 个 key")
    private List<String> keys;

    /** 默认 true：只报告将要登记的结果，不写库 */
    private Boolean dryRun = true;
}
