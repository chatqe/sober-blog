package io.charqe.qs.modules.moment.service;

import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentPageQueryDTO;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentSaveReqDTO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentDetailVO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentDetailVO.MomentMediaVO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentListVO;
import io.charqe.qs.modules.moment.domain.dao.MomentMapper;
import io.charqe.qs.modules.moment.domain.dao.MomentMediaMapper;
import io.charqe.qs.modules.moment.domain.pojo.Moment;
import io.charqe.qs.modules.moment.domain.pojo.MomentMedia;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 动态 Service 实现
 *
 * @author Joy
 * @since 2026-06-20
 */
@Slf4j
@Service
public class MomentServiceImpl extends CoreServiceImpl<MomentMapper, Moment> implements MomentService {

    @Resource
    private MomentMediaMapper momentMediaMapper;

    @Override
    public PageResult<MomentListVO> pageList(MomentPageQueryDTO query) {
        var wrapper = Lb.<Moment>q()
                .eq(query.getUserId() != null, Moment::getUserId, query.getUserId())
                .eq(query.getStatus() != null, Moment::getStatus, query.getStatus())
                .eq(query.getVisibility() != null, Moment::getVisibility, query.getVisibility())
                .eq(query.getSource() != null, Moment::getSource, query.getSource())
                .orderByDesc(Moment::getCreateTime);

        var mpPage = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.fromMpPage(mpPage, this::toListVO);
    }

    @Override
    public MomentDetailVO getDetail(Long id) {
        Moment moment = getById(id);
        if (moment == null) {
            throw new BizException("动态不存在");
        }
        return toDetailVO(moment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(MomentSaveReqDTO dto) {
        Moment moment = new Moment();
        moment.setUserId(dto.getUserId());
        moment.setContent(dto.getContent());
        moment.setHasText(dto.getHasText());
        moment.setHasImages(dto.getHasImages());
        moment.setHasVideos(dto.getHasVideos());
        moment.setHasAudios(dto.getHasAudios());
        moment.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        moment.setVisibility(dto.getVisibility() != null ? dto.getVisibility() : 2);
        moment.setSource(dto.getSource() != null ? dto.getSource() : 1);
        save(moment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MomentSaveReqDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("ID不能为空");
        }
        Moment exist = getById(dto.getId());
        if (exist == null) {
            throw new BizException("动态不存在");
        }

        Moment moment = new Moment();
        moment.setId(dto.getId());
        if (StringUtils.hasText(dto.getContent())) {
            moment.setContent(dto.getContent());
        }
        if (dto.getHasText() != null) moment.setHasText(dto.getHasText());
        if (dto.getHasImages() != null) moment.setHasImages(dto.getHasImages());
        if (dto.getHasVideos() != null) moment.setHasVideos(dto.getHasVideos());
        if (dto.getHasAudios() != null) moment.setHasAudios(dto.getHasAudios());
        if (dto.getStatus() != null) moment.setStatus(dto.getStatus());
        if (dto.getVisibility() != null) moment.setVisibility(dto.getVisibility());
        if (dto.getAuditReason() != null) {
            moment.setAuditReason(dto.getAuditReason());
            moment.setAuditedAt(LocalDateTime.now());
        }
        updateById(moment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Moment moment = getById(id);
        if (moment == null) {
            throw new BizException("动态不存在");
        }
        // 级联删除媒体关联
        momentMediaMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MomentMedia>()
                .eq(MomentMedia::getMomentId, id));
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            delete(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, Integer status, String auditReason) {
        Moment moment = getById(id);
        if (moment == null) {
            throw new BizException("动态不存在");
        }
        Moment update = new Moment();
        update.setId(id);
        update.setStatus(status);
        update.setAuditReason(auditReason);
        update.setAuditedAt(LocalDateTime.now());
        updateById(update);
    }

    @Override
    public MomentDetailVO toDetailVO(Moment moment) {
        MomentDetailVO vo = new MomentDetailVO();
        vo.setId(moment.getId());
        vo.setUserId(moment.getUserId());
        vo.setContent(moment.getContent());
        vo.setHasText(moment.getHasText());
        vo.setHasImages(moment.getHasImages());
        vo.setHasVideos(moment.getHasVideos());
        vo.setHasAudios(moment.getHasAudios());
        vo.setLikesCount(moment.getLikesCount());
        vo.setCommentsCount(moment.getCommentsCount());
        vo.setViewsCount(moment.getViewsCount());
        vo.setStatus(moment.getStatus());
        vo.setVisibility(moment.getVisibility());
        vo.setSource(moment.getSource());
        vo.setSourceExternalId(moment.getSourceExternalId());
        vo.setSourceUserId(moment.getSourceUserId());
        vo.setDelTime(moment.getDelTime() != null ? moment.getDelTime().toString() : null);
        vo.setDelBy(moment.getDelBy());
        vo.setAuditReason(moment.getAuditReason());
        vo.setAuditedAt(moment.getAuditedAt() != null ? moment.getAuditedAt().toString() : null);
        vo.setCreateTime(moment.getCreateTime() != null ? moment.getCreateTime().toString() : null);
        vo.setUpdateTime(moment.getUpdateTime() != null ? moment.getUpdateTime().toString() : null);

        // 查询媒体列表
        List<MomentMedia> mediaList = momentMediaMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MomentMedia>()
                        .eq(MomentMedia::getMomentId, moment.getId())
                        .orderByAsc(MomentMedia::getSortOrder));
        vo.setMediaList(mediaList.stream().map(this::toMediaVO).collect(Collectors.toList()));
        return vo;
    }

    private MomentListVO toListVO(Moment entity) {
        MomentListVO vo = new MomentListVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        // 内容摘要：截取前50字
        String content = entity.getContent();
        vo.setContentSummary(content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content);
        vo.setHasImages(entity.getHasImages());
        vo.setHasVideos(entity.getHasVideos());
        vo.setHasAudios(entity.getHasAudios());
        vo.setLikesCount(entity.getLikesCount());
        vo.setCommentsCount(entity.getCommentsCount());
        vo.setViewsCount(entity.getViewsCount());
        vo.setStatus(entity.getStatus());
        vo.setVisibility(entity.getVisibility());
        vo.setSource(entity.getSource());
        vo.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        return vo;
    }

    private MomentMediaVO toMediaVO(MomentMedia media) {
        MomentMediaVO vo = new MomentMediaVO();
        vo.setId(media.getId());
        vo.setFileId(media.getFileId());
        vo.setMediaType(media.getMediaType());
        vo.setSortOrder(media.getSortOrder());
        return vo;
    }
}
