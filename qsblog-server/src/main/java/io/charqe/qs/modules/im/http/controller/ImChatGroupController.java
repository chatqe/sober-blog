package io.charqe.qs.modules.im.http.controller;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.im.websocket.ImConfigConst;
import io.charqe.qs.modules.im.websocket.TioUtil;
import io.charqe.qs.modules.im.websocket.TioWebsocketStarter;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.im.http.entity.ImChatGroup;
import io.charqe.qs.modules.im.http.entity.ImChatGroupUser;
import io.charqe.qs.modules.im.http.entity.ImChatUserGroupMessage;
import io.charqe.qs.modules.im.http.service.ImChatGroupService;
import io.charqe.qs.modules.im.http.service.ImChatGroupUserService;
import io.charqe.qs.modules.im.http.service.ImChatUserGroupMessageService;
import io.charqe.qs.modules.im.http.vo.GroupVO;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.modules.user.enums.UserTypeEnum;
import io.charqe.qs.utils.BlogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.tio.core.Tio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@RestController
@RequestMapping("/imChatGroup")
public class ImChatGroupController {

    @Autowired
    private ImChatGroupService imChatGroupService;

    @Autowired
    private ImChatGroupUserService imChatGroupUserService;

    @Autowired
    private ImChatUserGroupMessageService imChatUserGroupMessageService;

    /**
     * 创建群组
     */
    @PostMapping("/creatGroupCommon")
    @LoginCheck
    @SaveCheck
    public R creatGroup(@RequestBody ImChatGroup imChatGroup) {
        if (!StringUtils.hasText(imChatGroup.getGroupName())) {
            return R.fail(CodeMsg.PARAMETER_ERROR);
        }
        imChatGroup.setGroupType(ImConfigConst.GROUP_COMMON);
        Long userId = BlogUtil.getUserId();
        imChatGroup.setMasterUserId(userId);
        imChatGroupService.save(imChatGroup);

        ImChatGroupUser imChatGroupUser = new ImChatGroupUser();
        imChatGroupUser.setGroupId(imChatGroup.getId());
        imChatGroupUser.setUserId(userId);
        imChatGroupUser.setAdminFlag(ImConfigConst.ADMIN_FLAG_TRUE);
        imChatGroupUser.setUserStatus(ImConfigConst.GROUP_USER_STATUS_PASS);
        imChatGroupUserService.save(imChatGroupUser);

        TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
        if (tioWebsocketStarter != null) {
            Tio.bindGroup(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), String.valueOf(imChatGroup.getId()));
        }

        return R.success();
    }

    /**
     * 创建话题
     */
    @PostMapping("/creatGroupTopic")
    @LoginCheck(0)
    public R creatGroupTopic(@RequestBody ImChatGroup imChatGroup) {
        if (!StringUtils.hasText(imChatGroup.getGroupName())) {
            return R.fail(CodeMsg.PARAMETER_ERROR);
        }
        imChatGroup.setGroupType(ImConfigConst.GROUP_TOPIC);
        Long userId = BlogUtil.getUserId();
        imChatGroup.setMasterUserId(userId);
        imChatGroup.setInType(ImConfigConst.IN_TYPE_FALSE);
        imChatGroupService.save(imChatGroup);

        return R.success();
    }


    /**
     * 更新组
     * <p>
     * 只有群主才能修改组
     */
    @PostMapping("/updateGroup")
    @LoginCheck
    public R updateGroup(@RequestBody ImChatGroup imChatGroup) {
        LambdaUpdateChainWrapper<ImChatGroup> lambdaUpdate = imChatGroupService.lambdaUpdate();
        lambdaUpdate.eq(ImChatGroup::getId, imChatGroup.getId());
        lambdaUpdate.eq(ImChatGroup::getMasterUserId, BlogUtil.getUserId());
        if (StringUtils.hasText(imChatGroup.getGroupName())) {
            lambdaUpdate.set(ImChatGroup::getGroupName, imChatGroup.getGroupName());
        }
        if (StringUtils.hasText(imChatGroup.getAvatar())) {
            lambdaUpdate.set(ImChatGroup::getAvatar, imChatGroup.getAvatar());
        }
        if (StringUtils.hasText(imChatGroup.getIntroduction())) {
            lambdaUpdate.set(ImChatGroup::getIntroduction, imChatGroup.getIntroduction());
        }
        // 群通知
        if (StringUtils.hasText(imChatGroup.getNotice())) {
            lambdaUpdate.set(ImChatGroup::getNotice, imChatGroup.getNotice());
        }
        // 修改进入方式
        if (imChatGroup.getInType() != null) {
            lambdaUpdate.set(ImChatGroup::getInType, imChatGroup.getInType());
        }
        // 转让群
        if (imChatGroup.getMasterUserId() != null) {
            lambdaUpdate.set(ImChatGroup::getMasterUserId, imChatGroup.getMasterUserId());
        }
        boolean isSuccess = lambdaUpdate.update();
        if (isSuccess && StringUtils.hasText(imChatGroup.getNotice())) {
            // todo 发送群公告
        }
        return R.success();
    }

    /**
     * 解散群
     */
    @GetMapping("/deleteGroup")
    @LoginCheck
    public R deleteGroup(@RequestParam("id") Long id) {
        User currentUser = BlogUtil.getCurrentUser();
        boolean isSuccess;
        if (currentUser.getUserType().intValue() == UserTypeEnum.ADMIN.getCode()) {
            isSuccess = imChatGroupService.removeById(id);
        } else {
            LambdaUpdateChainWrapper<ImChatGroup> lambdaUpdate = imChatGroupService.lambdaUpdate();
            lambdaUpdate.eq(ImChatGroup::getId, id);
            lambdaUpdate.eq(ImChatGroup::getMasterUserId, BlogUtil.getUserId());
            isSuccess = lambdaUpdate.remove();
        }
        if (isSuccess) {
            // 删除用户
            LambdaUpdateChainWrapper<ImChatGroupUser> lambdaUpdate = imChatGroupUserService.lambdaUpdate();
            lambdaUpdate.eq(ImChatGroupUser::getGroupId, id).remove();
            // 删除聊天记录
            LambdaUpdateChainWrapper<ImChatUserGroupMessage> messageLambdaUpdateChainWrapper = imChatUserGroupMessageService.lambdaUpdate();
            messageLambdaUpdateChainWrapper.eq(ImChatUserGroupMessage::getGroupId, id).remove();

            TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
            if (tioWebsocketStarter != null) {
                Tio.removeGroup(tioWebsocketStarter.getServerTioConfig(), String.valueOf(id), "remove group");
            }
        }
        return R.success();
    }

    /**
     * 管理员查询所有群
     */
    @PostMapping("/listGroupForAdmin")
    @LoginCheck(0)
    public R<BaseReqVO> listGroupForAdmin(@RequestBody BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<ImChatGroup> lambdaQuery = imChatGroupService.lambdaQuery();
        lambdaQuery.orderByDesc(ImChatGroup::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }

    /**
     * 加入话题
     */
    @GetMapping("/addGroupTopic")
    @LoginCheck
    public R addGroupTopic(@RequestParam("id") Integer id) {
        LambdaQueryChainWrapper<ImChatGroup> lambdaQuery = imChatGroupService.lambdaQuery();
        Integer count = Math.toIntExact(lambdaQuery.eq(ImChatGroup::getId, id)
                .eq(ImChatGroup::getGroupType, ImConfigConst.GROUP_TOPIC).count());
        if (count == 1) {
            TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
            if (tioWebsocketStarter != null) {
                Tio.bindGroup(tioWebsocketStarter.getServerTioConfig(), String.valueOf(BlogUtil.getUserId()), String.valueOf(id));
            }
        }
        return R.success();
    }

    /**
     * 用户查询所有群
     * <p>
     * 只查询审核通过和禁言的群
     */
    @GetMapping("/listGroup")
    @LoginCheck
    public R<List<GroupVO>> listGroup() {
        Long userId = BlogUtil.getUserId();
        LambdaQueryChainWrapper<ImChatGroupUser> lambdaQuery = imChatGroupUserService.lambdaQuery();
        lambdaQuery.eq(ImChatGroupUser::getUserId, userId);
        lambdaQuery.in(ImChatGroupUser::getUserStatus, ImConfigConst.GROUP_USER_STATUS_PASS, ImConfigConst.GROUP_USER_STATUS_SILENCE);
        List<ImChatGroupUser> groupUsers = lambdaQuery.list();

        Map<Long, ImChatGroupUser> groupUserMap = groupUsers.stream().collect(Collectors.toMap(ImChatGroupUser::getGroupId, Function.identity()));
        LambdaQueryChainWrapper<ImChatGroup> wrapper = imChatGroupService.lambdaQuery();
        wrapper.eq(ImChatGroup::getGroupType, ImConfigConst.GROUP_TOPIC);
        if (!CollectionUtils.isEmpty(groupUserMap.keySet())) {
            wrapper.or(w -> w.in(ImChatGroup::getId, groupUserMap.keySet())
                    .eq(ImChatGroup::getGroupType, ImConfigConst.GROUP_COMMON));
        }
        List<ImChatGroup> imChatGroups = wrapper.list();
        List<GroupVO> groupVOS = imChatGroups.stream().map(imChatGroup -> {
            ImChatGroupUser imChatGroupUser = groupUserMap.get(imChatGroup.getId());
            if (imChatGroupUser == null) {
                imChatGroupUser = new ImChatGroupUser();
                imChatGroupUser.setUserStatus(ImConfigConst.GROUP_USER_STATUS_PASS);
                imChatGroupUser.setCreateTime(LocalDateTime.now());
                imChatGroupUser.setUserId(userId);
                imChatGroupUser.setAdminFlag(userId.equals(BlogUtil.getAdminUser().getId()));
            }
            return getGroupVO(imChatGroup, imChatGroupUser);
        }).collect(Collectors.toList());
        return R.success(groupVOS);
    }

    private GroupVO getGroupVO(ImChatGroup imChatGroup, ImChatGroupUser imChatGroupUser) {
        GroupVO groupVO = new GroupVO();
        groupVO.setGroupName(imChatGroup.getGroupName());
        groupVO.setAvatar(imChatGroup.getAvatar());
        groupVO.setIntroduction(imChatGroup.getIntroduction());
        groupVO.setNotice(imChatGroup.getNotice());
        groupVO.setInType(imChatGroup.getInType());
        groupVO.setGroupType(imChatGroup.getGroupType());
        groupVO.setId(imChatGroup.getId());
        groupVO.setCreateTime(imChatGroupUser.getCreateTime());
        groupVO.setUserStatus(imChatGroupUser.getUserStatus());
        groupVO.setAdminFlag(imChatGroupUser.getAdminFlag());
        groupVO.setMasterFlag(imChatGroup.getMasterUserId().intValue() == imChatGroupUser.getUserId().intValue());
        return groupVO;
    }
}



