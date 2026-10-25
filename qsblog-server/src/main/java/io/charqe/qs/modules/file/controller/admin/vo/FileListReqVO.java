package io.charqe.qs.modules.file.controller.admin.vo;

import io.charqe.qs.common.model.BaseReqVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件媒体库分页入参。
 * <p>
 * 同时兼容前端历史参数 {@code pageNum/pageSize} 与 MP Page 的 {@code current/size}。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileListReqVO extends BaseReqVO {

    private Integer pageNum;

    private Integer pageSize;

    /** 业务场景过滤 */
    private String scene;

    /** 原始文件名模糊搜索 */
    private String keyword;

    public void normalize() {
        if (pageNum != null && pageNum > 0) {
            setCurrent(pageNum);
        }
        if (pageSize != null && pageSize > 0) {
            setSize(pageSize);
        }
    }
}
