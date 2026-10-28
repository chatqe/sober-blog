package io.charqe.qs.modules.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.user.auth.AuthSessionVO;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.modules.user.controller.web.vo.UserVO;
import io.charqe.qs.common.model.mp.CoreService;

import java.util.List;
import java.util.Map;

/**
 * 用户Service接口，继承自CoreService，享受通用CRUD及扩展功能
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
public interface UserService extends CoreService<User> {

    /**
     * 用户名、邮箱、手机号/密码登录
     *
     * @param account
     * @param password
     * @return
     */
    R<AuthSessionVO> login(String account, String password, SessionScope scope);

    R exit();

    R<AuthSessionVO> regist(UserVO user);

    R<UserVO> updateUserInfo(UserVO user);

    R getCode(Integer flag);

    R getCodeForBind(String place, Integer flag);

    R<UserVO> updateSecretInfo(String place, Integer flag, String code, String password);

    R getCodeByRegister(String place, Integer flag);

    R updateForForgetPassword(String place, Integer flag, String code, String password);

    R<PageResult> listUser(BaseReqVO baseReqVO);

    R<List<UserVO>> getUserByUsername(String username);

    R<AuthSessionVO> token(String userToken);

    R<UserVO> subscribe(Long labelId, Boolean flag);

    /**
     * 获取验证码
     *
     * @param flag
     * @return
     */
    Map<String, String> getCaptcha(Boolean flag, String email);

    Boolean checkCaptcha(String uuid, String code);

    /**
     * 获取邮件验证码
     *
     * @param bizType
     * @param receiver
     * @return
     */
    R mailCode(String bizType, String receiver);

    /**
     * 重置密码
     *
     * @param email
     * @param bizType
     * @return
     */
    R resetPwdForFgtPwd(String email, String bizType, String code, String password);

    /**
     * 获取站长（userType=0）账号，供前台公开展示作者信息
     *
     * @return 站长账号，缺失时返回 null
     */
    User getOwner();
}


