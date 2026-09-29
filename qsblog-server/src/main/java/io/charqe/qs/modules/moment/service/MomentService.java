package io.charqe.qs.modules.moment.service;

import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentPageQueryDTO;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentSaveReqDTO;
import io.charqe.qs.modules.moment.controller.web.dto.TimelineQueryDTO;
import io.charqe.qs.modules.moment.controller.web.dto.MomentSyncReqDTO;
import io.charqe.qs.modules.moment.controller.web.vo.*;
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
    PageResult<io.charqe.qs.modules.moment.controller.admin.vo.MomentListVO> pageList(MomentPageQueryDTO query);

    /**
     * 获取动态详情（Admin）
     */
    io.charqe.qs.modules.moment.controller.admin.vo.MomentDetailVO getDetail(Long id);

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
     * Entity -> Admin DetailVO 转换
     */
    io.charqe.qs.modules.moment.controller.admin.vo.MomentDetailVO toDetailVO(Moment moment);

    // ==================== Web 端方法 ====================

    /**
     * 公开动态时间线（分页）
     */
    PageResult<MomentListVO> timeline(TimelineQueryDTO query, Long currentUserId);

    /**
     * 当前用户自己的动态时间线（分页）
     */
    PageResult<MomentListVO> myTimeline(TimelineQueryDTO query, Long userId);

    /**
     * 指定用户的公开动态时间线（分页）
     */
    PageResult<MomentListVO> userTimeline(Long userId, TimelineQueryDTO query, Long currentUserId);

    /**
     * 获取动态详情（Web端，含媒体URL和点赞状态）
     */
    MomentDetailVO getWebDetail(Long id, Long currentUserId);

    /**
     * 发布动态（Web端）
     */
    Long publish(Long userId, io.charqe.qs.modules.moment.controller.web.dto.MomentSaveReqDTO dto);

    /**
     * 更新动态（Web端，仅本人可操作）
     */
    void updateWeb(Long id, Long userId, io.charqe.qs.modules.moment.controller.web.dto.MomentSaveReqDTO dto);

    /**
     * 删除动态（Web端，仅本人可操作）
     */
    void deleteWeb(Long id, Long userId);

    /**
     * 获取点赞该动态的用户列表
     */
    List<MomentLikeUserVO> listLikes(Long momentId);

    /**
     * 获取点赞该评论的用户列表
     */
    List<MomentLikeUserVO> listCommentLikes(Long commentId, Integer commentBizType);

    /**
     * 同步到多平台
     */
    MomentSyncVO sync(MomentSyncReqDTO dto, Long userId);

    /**
     * 获取同步记录列表
     */
    List<MomentSyncRecordVO> listSyncRecords(Long momentId);
}
