package io.charqe.qs.controller;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.R;
import io.charqe.qs.constants.CommonConst;
import io.charqe.qs.enums.CodeMsg;
import io.charqe.qs.enums.YouthEnum;
import io.charqe.qs.im.websocket.TioUtil;
import io.charqe.qs.im.websocket.TioWebsocketStarter;
import io.charqe.qs.model.BaseReqVO;
import io.charqe.qs.model.user.pojo.User;
import io.charqe.qs.service.UserService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.tio.core.Tio;

import javax.annotation.Resource;

/**
 * <p>
 * 后台用户 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/admin")
public class AdminUserController {

    @Autowired
    private UserService userService;


    @Resource
    private QingSiCache qingSiCache;

    /**
     * 查询用户
     */
    @PostMapping("/user/list")
    @LoginCheck(0)
    public R<Page> listUser(@RequestBody BaseReqVO baseReqVO) {
        return userService.listUser(baseReqVO);
    }

    /**
     * 修改用户状态
     * <p>
     * flag = true：解禁
     * flag = false：封禁
     */
    @GetMapping("/user/changeUserStatus")
    @LoginCheck(0)
    public R changeUserStatus(@RequestParam("userId") Integer userId, @RequestParam("flag") Boolean flag) {
        if (userId.intValue() == BlogUtil.getAdminUser().getId().intValue()) {
            return R.fail("站长状态不能修改！");
        }

        LambdaUpdateChainWrapper<User> updateChainWrapper = userService.lambdaUpdate().eq(User::getId, userId);
        if (flag) {
            updateChainWrapper.eq(User::getUserStatus, YouthEnum.STATUS_DISABLE.getCode()).set(User::getUserStatus, YouthEnum.STATUS_ENABLE.getCode()).update();
        } else {
            updateChainWrapper.eq(User::getUserStatus, YouthEnum.STATUS_ENABLE.getCode()).set(User::getUserStatus, YouthEnum.STATUS_DISABLE.getCode()).update();
        }
        logout(userId);
        return R.success();
    }

    /**
     * 修改用户赞赏
     */
    @GetMapping("/user/changeUserAdmire")
    @LoginCheck(0)
    public R changeUserAdmire(@RequestParam("userId") Integer userId, @RequestParam("admire") String admire) {
        userService.lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getAdmire, admire)
                .update();
        // PoetryCache.remove(CommonConst.ADMIRE);
        qingSiCache.del(CommonConst.ADMIRE);
        return R.success();
    }

    /**
     * 修改用户类型
     */
    @GetMapping("/user/changeUserType")
    @LoginCheck(0)
    public R changeUserType(@RequestParam("userId") Integer userId, @RequestParam("userType") Integer userType) {
        if (userId.intValue() == BlogUtil.getAdminUser().getId().intValue()) {
            return R.fail("站长类型不能修改！");
        }

        if (userType != 0 && userType != 1 && userType != 2) {
            return R.fail(CodeMsg.PARAMETER_ERROR);
        }
        userService.lambdaUpdate().eq(User::getId, userId).set(User::getUserType, userType).update();

        logout(userId);
        return R.success();
    }

    private void logout(Integer userId) {
        // if (PoetryCache.get(CommonConst.ADMIN_TOKEN + userId) != null) {
        //     String token = (String) PoetryCache.get(CommonConst.ADMIN_TOKEN + userId);
        //     PoetryCache.remove(CommonConst.ADMIN_TOKEN + userId);
        //     PoetryCache.remove(token);
        // }
        if (qingSiCache.get(CommonConst.ADMIN_TOKEN + userId) != null) {
            String token = qingSiCache.get(CommonConst.ADMIN_TOKEN + userId);
            qingSiCache.del(CommonConst.ADMIN_TOKEN + userId);
            qingSiCache.del(token);
        }

        // if (PoetryCache.get(CommonConst.USER_TOKEN + userId) != null) {
        //     String token = (String) PoetryCache.get(CommonConst.USER_TOKEN + userId);
        //     PoetryCache.remove(CommonConst.USER_TOKEN + userId);
        //     PoetryCache.remove(token);
        // }
        if (qingSiCache.get(CommonConst.USER_TOKEN + userId) != null) {
            String token = qingSiCache.get(CommonConst.USER_TOKEN + userId);
            qingSiCache.del(CommonConst.USER_TOKEN + userId);
            qingSiCache.del(token);
        }
        TioWebsocketStarter tioWebsocketStarter = TioUtil.getTio();
        if (tioWebsocketStarter != null) {
            Tio.removeUser(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), "remove user");
        }

    }
}


