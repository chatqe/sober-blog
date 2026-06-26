package io.charqe.qs.modules.moment.service;

import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentPageQueryDTO;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentSaveReqDTO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentDetailVO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentListVO;
import io.charqe.qs.modules.moment.domain.pojo.Moment;
import io.charqe.qs.common.model.mp.CoreService;

import java.util.List;

/**
 * 动态 Service
 *
 * @author Joy
 * @since 2026-06-20
 */
public interface MomentService extends CoreService<Moment> {

    /**
     * 分页查询动态列表（Admin）
     */
    PageResult<MomentListVO> pageList(MomentPageQueryDTO query);

    /**
     * 获取动态详情（Admin）
     */
    MomentDetailVO getDetail(Long id);

    /**
     * 新增动态（Admin）
     */
    void save(MomentSaveReqDTO dto);

    /**
     * 更新动态（Admin）
     */
    void update(MomentSaveReqDTO dto);

    /**
     * 删除动态（Admin）
     */
    void delete(Long id);

    /**
     * 批量删除动态（Admin）
     */
    void batchDelete(List<Long> ids);

    /**
     * 审核动态（Admin）
     *
     * @param id 动态ID
     * @param status 审核状态: 1-通过 2-驳回
     * @param auditReason 审核原因
     */
    void audit(Long id, Integer status, String auditReason);

    /**
     * Entity -> DetailVO 转换
     */
    MomentDetailVO toDetailVO(Moment moment);
}
