package io.charqe.qs.modules.file.controller.admin.vo;

import io.charqe.qs.common.model.BaseReqVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件引用查询入参。
 * <p>
 * 按 {@code fileId} 查询时，非管理员只能查自己拥有的文件；按业务维度
 * （{@code bizType + bizId}，其中 bizId 继承自 BaseReqVO）查询仅管理员可用，
 * 避免跨用户探测引用关系。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileRefListReqVO extends BaseReqVO {

    private Long fileId;

    private Integer bizType;

    private String bizField;

    public void normalize() {
        if (getCurrent() < 1) {
            setCurrent(1);
        }
        if (getSize() < 1) {
            setSize(20);
        }
        if (getSize() > 200) {
            setSize(200);
        }
    }
}
