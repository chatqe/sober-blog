package io.charqe.qs.modules.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.file.controller.admin.vo.FileRefBindReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRefListReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRefUnbindReqVO;
import io.charqe.qs.modules.file.domain.pojo.FileBizRef;

/**
 * 文件业务关系表 服务类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
public interface FileBizRefService extends CoreService<FileBizRef> {

    /**
     * 绑定文件引用：同一 {@code fileId + bizType + bizId + bizField} 重复绑定幂等，
     * 首次绑定成功后文件 {@code ref_count} 加一。
     *
     * @param userId  当前操作人
     * @param manager 是否管理员（管理员可操作任意文件）
     */
    FileBizRef bind(FileRefBindReqVO req, Long userId, boolean manager);

    /**
     * 解绑文件引用：记录不存在时返回 false（幂等），成功时文件 {@code ref_count} 减一。
     */
    boolean unbind(FileRefUnbindReqVO req, Long userId, boolean manager);

    /**
     * 分页查询引用关系：按 fileId 查询需具备该文件的操作权限；
     * 按 bizType + bizId 查询仅管理员可用。
     */
    IPage<FileBizRef> pageRefs(FileRefListReqVO req, Long userId, boolean manager);
}
