package io.charqe.qs.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import io.charqe.qs.common.R;
import io.charqe.qs.model.BaseReqVO;
import io.charqe.qs.model.user.pojo.User;
import io.charqe.qs.model.user.vo.UserVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户信息表 服务类
 * </p>
 *
 * @author sara
 * @since 2021-08-12
 */
public interface UserService extends IService<User> {

    /**
     * 用户名、邮箱、手机号/密码登录
     *
     * @param account
     * @param password
     * @return
     */
    R<UserVO> login(String account, String password, Boolean isAdmin);

    R exit();

    R<UserVO> regist(UserVO user);

    R<UserVO> updateUserInfo(UserVO user);

    R getCode(Integer flag);

    R getCodeForBind(String place, Integer flag);

    R<UserVO> updateSecretInfo(String place, Integer flag, String code, String password);

    R getCodeByRegister(String place, Integer flag);

    R updateForForgetPassword(String place, Integer flag, String code, String password);

    R<Page> listUser(BaseReqVO baseReqVO);

    R<List<UserVO>> getUserByUsername(String username);

    R<UserVO> token(String userToken);

    R<UserVO> subscribe(Integer labelId, Boolean flag);

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
}


