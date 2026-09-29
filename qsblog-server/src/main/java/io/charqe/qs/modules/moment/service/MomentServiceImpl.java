package io.charqe.qs.modules.moment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.domain.dao.BehaviorCntMapper;
import io.charqe.qs.modules.behavior.domain.dao.BehaviorStateMapper;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorCnt;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorState;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.service.like.LikeService;
import io.charqe.qs.modules.behavior.service.view.ViewService;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentPageQueryDTO;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentSaveReqDTO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentDetailVO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentListVO;
import io.charqe.qs.modules.moment.controller.web.dto.TimelineQueryDTO;
import io.charqe.qs.modules.moment.controller.web.dto.MomentSyncReqDTO;
import io.charqe.qs.modules.moment.controller.web.vo.MomentLikeUserVO;
import io.charqe.qs.modules.moment.controller.web.vo.MomentMediaVO;
import io.charqe.qs.modules.moment.controller.web.vo.MomentSyncRecordVO;
import io.charqe.qs.modules.moment.controller.web.vo.MomentSyncVO;
import io.charqe.qs.modules.moment.domain.dao.MomentMapper;
import io.charqe.qs.modules.moment.domain.dao.MomentMediaMapper;
import io.charqe.qs.modules.moment.domain.dao.MomentSocialSyncMapper;
import io.charqe.qs.modules.moment.domain.pojo.Moment;
import io.charqe.qs.modules.moment.domain.pojo.MomentMedia;
import io.charqe.qs.modules.moment.domain.pojo.MomentSocialSync;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.user.domain.dao.UserMapper;
import io.charqe.qs.modules.user.domain.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MomentServiceImpl extends CoreServiceImpl<MomentMapper, Moment> implements MomentService {

    private static final int MOMENT_BIZ_TYPE = BizTypeEnum.MOMENT.getCode();

    @Resource
    private MomentMediaMapper momentMediaMapper;
    @Resource
    private MomentSocialSyncMapper momentSocialSyncMapper;
    @Resource
    private BehaviorStateMapper behaviorStateMapper;
    @Resource
    private BehaviorCntMapper behaviorCntMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private LikeService likeService;
    @Resource
    private ViewService viewService;

    // ==================== Admin 方法（保留原有实现）====================

    @Override
    public PageResult<MomentListVO> pageList(MomentPageQueryDTO query) {
        LambdaQueryWrapper<Moment> wrapper = Lb.<Moment>q()
                .eq(query.getUserId() != null, Moment::getUserId, query.getUserId())
                .eq(query.getStatus() != null, Moment::getStatus, query.getStatus())
                .eq(query.getVisibility() != null, Moment::getVisibility, query.getVisibility())
                .eq(query.getSource() != null, Moment::getSource, query.getSource())
                .orderByDesc(Moment::getCreateTime);

        Page<Moment> mpPage = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
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
        momentMediaMapper.delete(new LambdaQueryWrapper<MomentMedia>().eq(MomentMedia::getMomentId, id));
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
        List<MomentMedia> mediaList = momentMediaMapper.selectList(
                new LambdaQueryWrapper<MomentMedia>()
                        .eq(MomentMedia::getMomentId, moment.getId())
                        .orderByAsc(MomentMedia::getSortOrder));
        vo.setMediaList(mediaList.stream().map(this::toMediaVO).collect(Collectors.toList()));
        return vo;
    }

    private MomentListVO toListVO(Moment entity) {
        MomentListVO vo = new MomentListVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
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

    private MomentDetailVO.MomentMediaVO toMediaVO(MomentMedia media) {
        MomentDetailVO.MomentMediaVO vo = new MomentDetailVO.MomentMediaVO();
        vo.setId(media.getId());
        vo.setFileId(media.getFileId());
        vo.setMediaType(media.getMediaType());
        vo.setSortOrder(media.getSortOrder());
        return vo;
    }

    // ==================== Web 端方法 ====================

    @Override
    public PageResult<io.charqe.qs.modules.moment.controller.web.vo.MomentListVO> timeline(TimelineQueryDTO query, Long currentUserId) {
        LambdaQueryWrapper<Moment> wrapper = Lb.<Moment>q()
                .eq(Moment::getStatus, 1)
                .eq(Moment::getVisibility, 2)
                .isNull(Moment::getDelTime)
                .orderByDesc(Moment::getCreateTime);

        Page<Moment> mpPage = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<Moment> records = mpPage.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty();
        }

        Set<Long> userIds = records.stream()
                .map(Moment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = buildUserMap(userIds);

        List<Long> momentIds = records.stream().map(Moment::getId).collect(Collectors.toList());
        Set<Long> likedIds = currentUserId != null ? getLikedIds(currentUserId, momentIds) : Collections.emptySet();

        return PageResult.fromMpPage(mpPage, m -> toWebListVO(m, userMap, likedIds, currentUserId));
    }

    @Override
    public PageResult<io.charqe.qs.modules.moment.controller.web.vo.MomentListVO> myTimeline(TimelineQueryDTO query, Long userId) {
        LambdaQueryWrapper<Moment> wrapper = Lb.<Moment>q()
                .eq(Moment::getUserId, userId)
                .ne(Moment::getStatus, 3)
                .isNull(Moment::getDelTime)
                .orderByDesc(Moment::getCreateTime);

        Page<Moment> mpPage = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        if (mpPage.getRecords().isEmpty()) {
            return PageResult.empty();
        }

        Map<Long, User> userMap = buildUserMap(Collections.singleton(userId));
        List<Long> momentIds = mpPage.getRecords().stream().map(Moment::getId).collect(Collectors.toList());
        Set<Long> likedIds = getLikedIds(userId, momentIds);

        return PageResult.fromMpPage(mpPage, m -> toWebListVO(m, userMap, likedIds, userId));
    }

    @Override
    public PageResult<io.charqe.qs.modules.moment.controller.web.vo.MomentListVO> userTimeline(Long targetUserId, TimelineQueryDTO query, Long currentUserId) {
        if (targetUserId == null) {
            throw new BizException("用户ID不能为空");
        }
        LambdaQueryWrapper<Moment> wrapper = Lb.<Moment>q()
                .eq(Moment::getUserId, targetUserId)
                .eq(Moment::getStatus, 1)
                .in(Moment::getVisibility, 1, 2)
                .isNull(Moment::getDelTime)
                .orderByDesc(Moment::getCreateTime);

        Page<Moment> mpPage = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<Moment> records = mpPage.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty();
        }

        Map<Long, User> userMap = buildUserMap(Collections.singleton(targetUserId));
        List<Long> momentIds = records.stream().map(Moment::getId).collect(Collectors.toList());
        Set<Long> likedIds = currentUserId != null ? getLikedIds(currentUserId, momentIds) : Collections.emptySet();

        return PageResult.fromMpPage(mpPage, m -> toWebListVO(m, userMap, likedIds, currentUserId));
    }

    @Override
    public io.charqe.qs.modules.moment.controller.web.vo.MomentDetailVO getWebDetail(Long id, Long currentUserId) {
        Moment moment = getById(id);
        if (moment == null) {
            throw new BizException("动态不存在");
        }
        if (moment.getStatus() != 1) {
            throw new BizException("动态不可见");
        }
        if (moment.getVisibility() == 0 && (currentUserId == null || !currentUserId.equals(moment.getUserId()))) {
            throw new BizException("该动态为私密，无权查看");
        }

        io.charqe.qs.modules.moment.controller.web.vo.MomentDetailVO vo = toWebDetailVO(moment, currentUserId);

        // 异步记录浏览
        if (moment.getViewsCount() == null || moment.getViewsCount() == 0) {
            // 计数已存在则不重复累加，仅异步记录
        }
        Long effectiveUserId = currentUserId != null ? currentUserId : 0L;
        BehDTO viewDto = new BehDTO();
        viewDto.setBizId(id);
        viewDto.setBizType(MOMENT_BIZ_TYPE);
        viewService.view(viewDto, effectiveUserId);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long userId, io.charqe.qs.modules.moment.controller.web.dto.MomentSaveReqDTO dto) {
        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setContent(dto.getContent());
        moment.setStatus(1);
        moment.setVisibility(dto.getVisibility() != null ? dto.getVisibility() : 2);
        moment.setSource(1);
        computeHasText(moment, dto.getContent());
        computeMediaFlags(moment, dto.getMediaList(), dto.getMediaIds());
        save(moment);

        // 保存媒体关联
        if (!CollectionUtils.isEmpty(dto.getMediaList())) {
            saveMediaWithItems(moment.getId(), dto.getMediaList());
        } else if (!CollectionUtils.isEmpty(dto.getMediaIds())) {
            saveMedia(moment.getId(), dto.getMediaIds());
        }

        return moment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWeb(Long id, Long userId, io.charqe.qs.modules.moment.controller.web.dto.MomentSaveReqDTO dto) {
        Moment moment = getById(id);
        if (moment == null) {
            throw new BizException("动态不存在");
        }
        if (!userId.equals(moment.getUserId())) {
            throw new BizException("无权操作该动态");
        }
        Moment update = new Moment();
        update.setId(id);
        if (StringUtils.hasText(dto.getContent())) {
            update.setContent(dto.getContent());
            update.setHasText(true);
        }
        if (dto.getVisibility() != null) {
            update.setVisibility(dto.getVisibility());
        }
        // content 有显式传入才更新 hasText
        // 前端约定：不传 content = 不修改 content；传 "" = 清空 content
        if (dto.getContent() != null) {
            update.setContent(dto.getContent());
            computeHasText(update, dto.getContent());
        }

        if (dto.getVisibility() != null) {
            update.setVisibility(dto.getVisibility());
        }
        // 只要显式传了 mediaList 或 mediaIds（含空数组），就重算 media flags
        if (dto.getMediaList() != null || dto.getMediaIds() != null) {
            computeMediaFlags(update, dto.getMediaList(), dto.getMediaIds());
        }
        updateById(update);

        // 媒体表操作：mediaList 不为 null 时（含空数组）先清旧媒体
        if (dto.getMediaList() != null) {
            momentMediaMapper.delete(new LambdaQueryWrapper<MomentMedia>().eq(MomentMedia::getMomentId, id));
            if (!dto.getMediaList().isEmpty()) {
                saveMediaWithItems(id, dto.getMediaList());
            }
        } else if (dto.getMediaIds() != null && !dto.getMediaIds().isEmpty()) {
            momentMediaMapper.delete(new LambdaQueryWrapper<MomentMedia>().eq(MomentMedia::getMomentId, id));
            saveMedia(id, dto.getMediaIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWeb(Long id, Long userId) {
        Moment moment = getById(id);
        if (moment == null) {
            throw new BizException("动态不存在");
        }
        if (!userId.equals(moment.getUserId())) {
            throw new BizException("无权删除该动态");
        }
        // 级联删除媒体
        momentMediaMapper.delete(new LambdaQueryWrapper<MomentMedia>().eq(MomentMedia::getMomentId, id));
        removeById(id);
    }

    @Override
    public List<MomentLikeUserVO> listLikes(Long momentId) {
        BehaviorCnt cnt = behaviorCntMapper.selectOne(
                new LambdaQueryWrapper<BehaviorCnt>()
                        .eq(BehaviorCnt::getBizId, momentId)
                        .eq(BehaviorCnt::getBizType, MOMENT_BIZ_TYPE)
                        .eq(BehaviorCnt::getAction, BehActionType.LIKE.getCode()));
        if (cnt == null || cnt.getCount() == null || cnt.getCount() == 0) {
            return Collections.emptyList();
        }

        // 按创建时间倒序取最近点赞用户
        List<BehaviorState> states = behaviorStateMapper.selectList(
                new LambdaQueryWrapper<BehaviorState>()
                        .eq(BehaviorState::getBizId, momentId)
                        .eq(BehaviorState::getBizType, MOMENT_BIZ_TYPE)
                        .eq(BehaviorState::getAction, BehActionType.LIKE.getCode())
                        .eq(BehaviorState::getStatus, BehaviorState.BEH_ACTIVE)
                        .orderByDesc(BehaviorState::getCreateTime)
                        .last("limit 20"));

        if (states.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = states.stream()
                .map(BehaviorState::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = buildUserMap(userIds);

        return states.stream().map(s -> {
            MomentLikeUserVO vo = new MomentLikeUserVO();
            vo.setId(s.getUserId());
            User user = userMap.get(s.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MomentSyncVO sync(MomentSyncReqDTO dto, Long userId) {
        Moment moment = getById(dto.getMomentId());
        if (moment == null) {
            throw new BizException("动态不存在");
        }
        if (!userId.equals(moment.getUserId())) {
            throw new BizException("无权操作该动态");
        }
        if (moment.getStatus() != 1) {
            throw new BizException("只能同步正常状态的动态");
        }

        MomentSyncVO result = new MomentSyncVO();
        result.setMomentId(dto.getMomentId());
        List<String> successPlatforms = new ArrayList<>();
        List<String> failPlatforms = new ArrayList<>();

        for (String platform : dto.getPlatforms()) {
            try {
                MomentSocialSync record = new MomentSocialSync();
                record.setMomentId(dto.getMomentId());
                record.setDirection(1); // 出向
                record.setPlatform(platform);
                record.setSyncStatus(2); // 成功（暂模拟）
                record.setSyncedAt(LocalDateTime.now());
                record.setCreateTime(LocalDateTime.now());
                momentSocialSyncMapper.insert(record);
                successPlatforms.add(platform);
            } catch (Exception e) {
                log.error("同步动态到{}失败: momentId={}", platform, dto.getMomentId(), e);
                failPlatforms.add(platform);
            }
        }

        result.setSuccessPlatforms(successPlatforms);
        result.setFailPlatforms(failPlatforms);
        return result;
    }

    @Override
    public List<MomentSyncRecordVO> listSyncRecords(Long momentId) {
        List<MomentSocialSync> records = momentSocialSyncMapper.selectList(
                new LambdaQueryWrapper<MomentSocialSync>()
                        .eq(MomentSocialSync::getMomentId, momentId)
                        .orderByDesc(MomentSocialSync::getCreateTime));
        return records.stream().map(this::toSyncRecordVO).collect(Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    private io.charqe.qs.modules.moment.controller.web.vo.MomentListVO toWebListVO(Moment m, Map<Long, User> userMap, Set<Long> likedIds, Long currentUserId) {
        io.charqe.qs.modules.moment.controller.web.vo.MomentListVO vo = new io.charqe.qs.modules.moment.controller.web.vo.MomentListVO();
        vo.setId(m.getId());
        vo.setUserId(m.getUserId());
        User author = userMap.get(m.getUserId());
        if (author != null) {
            vo.setAuthorName(author.getUsername());
            vo.setAuthorAvatar(author.getAvatar());
        }
        String content = m.getContent();
        vo.setContentSummary(content != null && content.length() > 100 ? content.substring(0, 100) + "..." : content);
        vo.setHasImages(m.getHasImages());
        vo.setHasVideos(m.getHasVideos());
        vo.setHasAudios(m.getHasAudios());
        vo.setLikesCount(m.getLikesCount());
        vo.setCommentsCount(m.getCommentsCount());
        vo.setViewsCount(m.getViewsCount());
        vo.setVisibility(m.getVisibility());
        vo.setSource(m.getSource());
        vo.setLiked(currentUserId != null && likedIds.contains(m.getId()));
        vo.setCreateTime(m.getCreateTime() != null ? m.getCreateTime().toString() : null);
        return vo;
    }

    private io.charqe.qs.modules.moment.controller.web.vo.MomentDetailVO toWebDetailVO(Moment m, Long currentUserId) {
        io.charqe.qs.modules.moment.controller.web.vo.MomentDetailVO vo = new io.charqe.qs.modules.moment.controller.web.vo.MomentDetailVO();
        vo.setId(m.getId());
        vo.setUserId(m.getUserId());
        vo.setContent(m.getContent());
        vo.setHasText(m.getHasText());
        vo.setHasImages(m.getHasImages());
        vo.setHasVideos(m.getHasVideos());
        vo.setHasAudios(m.getHasAudios());
        vo.setLikesCount(m.getLikesCount());
        vo.setCommentsCount(m.getCommentsCount());
        vo.setViewsCount(m.getViewsCount());
        vo.setVisibility(m.getVisibility());
        vo.setSource(m.getSource());
        vo.setLiked(currentUserId != null && isLiked(currentUserId, m.getId()));
        vo.setCreateTime(m.getCreateTime() != null ? m.getCreateTime().toString() : null);
        vo.setUpdateTime(m.getUpdateTime() != null ? m.getUpdateTime().toString() : null);

        // 加载作者信息
        if (m.getUserId() != null) {
            User author = userMapper.selectById(m.getUserId());
            if (author != null) {
                vo.setAuthorName(author.getUsername());
                vo.setAuthorAvatar(author.getAvatar());
            }
        }

        // 加载媒体列表
        List<MomentMedia> mediaList = momentMediaMapper.selectList(
                new LambdaQueryWrapper<MomentMedia>()
                        .eq(MomentMedia::getMomentId, m.getId())
                        .orderByAsc(MomentMedia::getSortOrder));
        vo.setMediaList(mediaList.stream().map(mm -> {
            MomentMediaVO webVo = new MomentMediaVO();
            webVo.setId(mm.getId());
            webVo.setFileId(mm.getFileId());
            webVo.setMediaType(mm.getMediaType());
            webVo.setSortOrder(mm.getSortOrder());
            return webVo;
        }).collect(Collectors.toList()));

        return vo;
    }

    private boolean isLiked(Long userId, Long momentId) {
        return behaviorStateMapper.selectCount(
                new LambdaQueryWrapper<BehaviorState>()
                        .eq(BehaviorState::getUserId, userId)
                        .eq(BehaviorState::getBizId, momentId)
                        .eq(BehaviorState::getBizType, MOMENT_BIZ_TYPE)
                        .eq(BehaviorState::getAction, BehActionType.LIKE.getCode())
                        .eq(BehaviorState::getStatus, BehaviorState.BEH_ACTIVE)) > 0;
    }

    private Set<Long> getLikedIds(Long userId, List<Long> momentIds) {
        if (userId == null || momentIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<BehaviorState> states = behaviorStateMapper.selectList(
                new LambdaQueryWrapper<BehaviorState>()
                        .in(BehaviorState::getBizId, momentIds)
                        .eq(BehaviorState::getUserId, userId)
                        .eq(BehaviorState::getBizType, MOMENT_BIZ_TYPE)
                        .eq(BehaviorState::getAction, BehActionType.LIKE.getCode())
                        .eq(BehaviorState::getStatus, BehaviorState.BEH_ACTIVE));
        return states.stream().map(BehaviorState::getBizId).collect(Collectors.toSet());
    }

    private Map<Long, User> buildUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private void saveMedia(Long momentId, List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return;
        }
        for (int i = 0; i < mediaIds.size(); i++) {
            MomentMedia mm = new MomentMedia();
            mm.setMomentId(momentId);
            mm.setFileId(mediaIds.get(i));
            mm.setSortOrder(i);
            momentMediaMapper.insert(mm);
        }
    }

    private void saveMediaWithItems(Long momentId, List<io.charqe.qs.modules.moment.controller.web.dto.MomentSaveReqDTO.MediaItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (io.charqe.qs.modules.moment.controller.web.dto.MomentSaveReqDTO.MediaItem item : items) {
            if (item.getFileId() == null || item.getMediaType() == null) {
                continue;
            }
            MomentMedia mm = new MomentMedia();
            mm.setMomentId(momentId);
            mm.setFileId(item.getFileId());
            mm.setMediaType(item.getMediaType());
            mm.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
            momentMediaMapper.insert(mm);
        }
    }

    private void computeHasText(Moment moment, String content) {
        boolean hasText = content != null && !content.trim().isEmpty();
        moment.setHasText(hasText);
    }

    private void computeMediaFlags(Moment moment,
                                   List<io.charqe.qs.modules.moment.controller.web.dto.MomentSaveReqDTO.MediaItem> mediaList,
                                   List<Long> legacyMediaIds) {
        boolean hasImages = false;
        boolean hasVideos = false;
        boolean hasAudios = false;

        if (mediaList != null && !mediaList.isEmpty()) {
            for (io.charqe.qs.modules.moment.controller.web.dto.MomentSaveReqDTO.MediaItem item : mediaList) {
                if (item == null || item.getMediaType() == null) { continue; }
                Integer type = item.getMediaType();
                if (type == 1) hasImages = true;
                else if (type == 2) hasVideos = true;
                else if (type == 3) hasAudios = true;
            }
        } else if (mediaList == null && legacyMediaIds != null && !legacyMediaIds.isEmpty()) {
            // 兼容旧接口：mediaIds 无法区分类型，保守假设有图片
            hasImages = true;
        }
        // mediaList == [] 空数组时三个 flag 均为 false（清空语义）

        moment.setHasImages(hasImages);
        moment.setHasVideos(hasVideos);
        moment.setHasAudios(hasAudios);
    }

    private MomentSyncRecordVO toSyncRecordVO(MomentSocialSync s) {
        MomentSyncRecordVO vo = new MomentSyncRecordVO();
        vo.setId(s.getId());
        vo.setMomentId(s.getMomentId());
        vo.setDirection(s.getDirection());
        vo.setPlatform(s.getPlatform());
        vo.setExternalId(s.getExternalId());
        vo.setSyncStatus(s.getSyncStatus());
        vo.setErrorMessage(s.getErrorMessage());
        vo.setSyncedAt(s.getSyncedAt());
        vo.setCreateTime(s.getCreateTime());
        return vo;
    }

    @Override
    public List<MomentLikeUserVO> listCommentLikes(Long commentId, Integer commentBizType) {
        if (commentId == null || commentBizType == null) {
            return Collections.emptyList();
        }
        List<BehaviorState> states = behaviorStateMapper.selectList(
                new LambdaQueryWrapper<BehaviorState>()
                        .eq(BehaviorState::getBizId, commentId)
                        .eq(BehaviorState::getBizType, commentBizType)
                        .eq(BehaviorState::getAction, BehActionType.LIKE.getCode())
                        .eq(BehaviorState::getStatus, BehaviorState.BEH_ACTIVE)
                        .orderByDesc(BehaviorState::getCreateTime)
                        .last("limit 20"));

        if (states.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = states.stream()
                .map(BehaviorState::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = buildUserMap(userIds);

        return states.stream().map(s -> {
            MomentLikeUserVO vo = new MomentLikeUserVO();
            vo.setId(s.getUserId());
            User user = userMap.get(s.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
