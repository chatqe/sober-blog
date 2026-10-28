package io.charqe.qs.modules.user.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.EmailBizType;
import io.charqe.qs.common.enums.VisibilityEnum;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.infra.thirdparty.mail.MailUtil;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorState;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.service.BehaviorLogService;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.modules.im.http.dao.ImChatGroupUserMapper;
import io.charqe.qs.modules.im.http.dao.ImChatUserFriendMapper;
import io.charqe.qs.modules.im.http.entity.ImChatGroupUser;
import io.charqe.qs.modules.im.http.entity.ImChatUserFriend;
import io.charqe.qs.modules.im.websocket.ImConfigConst;
import io.charqe.qs.modules.im.websocket.TioUtil;
import io.charqe.qs.modules.im.websocket.TioWebsocketStarter;
import io.charqe.qs.modules.moment.domain.pojo.Moment;
import io.charqe.qs.modules.moment.service.MomentService;
import io.charqe.qs.modules.user.controller.web.vo.UserVO;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.AuthSessionVO;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.modules.user.auth.UserCredentialService;
import io.charqe.qs.modules.user.domain.dao.UserMapper;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.user.enums.UserTypeEnum;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.utils.*;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.tio.core.Tio;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Service
@Slf4j
public class UserServiceImpl extends CoreServiceImpl<UserMapper, User> implements UserService {


    @Resource
    private MomentService momentService;

    @Resource
    private ImChatGroupUserMapper imChatGroupUserMapper;

    @Resource
    private ImChatUserFriendMapper imChatUserFriendMapper;

    @Resource
    private MailUtil mailUtil;

    @Resource
    private QingSiCache qingSiCache;

    @Resource
    private QingsiUtil qingsiUtil;

    @Resource
    private CaptchaUtils captchaUtils;

    @Resource
    private SvgCaptchaUtils svgCaptchaUtils;

    @Resource
    private BehaviorLogService behaviorLogService;


    @Resource
    private  JWTUtils jwtUtils;

    @Resource
    private AuthSessionService sessions;

    @Resource
    private UserCredentialService credentials;

    @Value("${user.code.format}")
    private String codeFormat;
    @Value("${user.fwd.format}")
    private String fwdFormat;


    @Override
    public R<AuthSessionVO> login(String account, String password, SessionScope scope) {
        UserCredentialService.Verified verified = credentials.authenticate(account, password);
        return R.success(sessions.issue(verified.user(), scope, verified.revision()));
    }

    @Override
    public R exit() {
        sessions.logout(BlogUtil.getToken(), SessionScope.WEB);
        return R.success();
    }


    @Override
    public R<AuthSessionVO> regist(UserVO user) {
        // 正则表达式 用于校验手机号(11位数字)
        String regex = "\\d{11}";
        if (user.getUsername().matches(regex)) {
            return R.fail("用户名不能为11位数字！");
        }

        if (user.getUsername().contains("@")) {
            return R.fail("用户名不能包含@！");
        }

        if (StringUtils.hasText(user.getPhoneNumber()) && StringUtils.hasText(user.getEmail())) {
            return R.fail("手机号与邮箱只能选择其中一个！");
        }

        if (StringUtils.hasText(user.getPhoneNumber())) {
            String codeKey = CommonConst.CODE_MAIL + user.getPhoneNumber() + "_" + EmailBizType.REGISTER.getType();
            Integer codeCache = JSON.parseObject(qingSiCache.get(codeKey), Integer.class);
            if (codeCache == null || codeCache != Integer.parseInt(user.getCode())) {
                return R.fail("验证码错误！");
            }
            qingSiCache.del(codeKey);
        } else if (StringUtils.hasText(user.getEmail())) {
            String codeKey = CommonConst.CODE_MAIL + user.getEmail() + "_" + EmailBizType.REGISTER.getType();
            Integer codeCache = JSON.parseObject(qingSiCache.get(codeKey), Integer.class);
            if (codeCache == null || codeCache != Integer.parseInt(user.getCode())) {
                return R.fail("验证码错误！");
            }
            qingSiCache.del(codeKey);
        } else {
            return R.fail("请输入邮箱或手机号！");
        }


        user.setPassword(new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(user.getPassword())));

        Integer count = Math.toIntExact(lambdaQuery().eq(User::getUsername, user.getUsername()).count());
        if (count != 0) {
            return R.fail("用户名重复！");
        }
        if (StringUtils.hasText(user.getPhoneNumber())) {
            Integer phoneNumberCount = Math.toIntExact(lambdaQuery().eq(User::getPhoneNumber, user.getPhoneNumber()).count());
            if (phoneNumberCount != 0) {
                return R.fail("手机号重复！");
            }
        } else if (StringUtils.hasText(user.getEmail())) {
            Integer emailCount = Math.toIntExact(lambdaQuery().eq(User::getEmail, user.getEmail()).count());
            if (emailCount != 0) {
                return R.fail("邮箱重复！");
            }
        }

        User u = new User();
        u.setUsername(user.getUsername());
        u.setPhoneNumber(user.getPhoneNumber());
        u.setEmail(user.getEmail());
        u.setPassword(encodePassword(user.getPassword()));
        u.setUserType(2);
        u.setUserStatus(1);
        // u.setAvatar(BlogUtil.getRandomAvatar(null));
        u.setAvatar(qingsiUtil.getRandomAvatar());
        save(u);

        User one = lambdaQuery().eq(User::getId, u.getId()).one();

        Moment moment = new Moment();
        moment.setUserId(one.getId());
        moment.setContent("到此一游");
        moment.setHasText(true);
        moment.setVisibility(VisibilityEnum.PUBLIC.getCode());
        moment.setSource(1);
        moment.setStatus(1);
        moment.setDelTime(null);
        moment.setDelBy(one.getId());
        momentService.save(moment);

        ImChatGroupUser imChatGroupUser = new ImChatGroupUser();
        imChatGroupUser.setGroupId(ImConfigConst.DEFAULT_GROUP_ID);
        imChatGroupUser.setUserId(one.getId());
        imChatGroupUser.setUserStatus(ImConfigConst.GROUP_USER_STATUS_PASS);
        imChatGroupUserMapper.insert(imChatGroupUser);

        ImChatUserFriend imChatUser = new ImChatUserFriend();
        imChatUser.setUserId(one.getId());
        imChatUser.setFriendId(BlogUtil.getAdminUser().getId());
        imChatUser.setRemark("站长");
        imChatUser.setFriendStatus(ImConfigConst.FRIEND_STATUS_PASS);
        imChatUserFriendMapper.insert(imChatUser);

        ImChatUserFriend imChatFriend = new ImChatUserFriend();
        imChatFriend.setUserId(BlogUtil.getAdminUser().getId());
        imChatFriend.setFriendId(one.getId());
        imChatFriend.setFriendStatus(ImConfigConst.FRIEND_STATUS_PASS);
        imChatUserFriendMapper.insert(imChatFriend);

        return R.success(sessions.issue(one, SessionScope.WEB, sessions.revision(one.getId())));
    }

    @Override
    public R<UserVO> updateUserInfo(UserVO user) {
        if (StringUtils.hasText(user.getUsername())) {
            String regex = "\\d{11}";
            if (user.getUsername().matches(regex)) {
                return R.fail("用户名不能为11位数字！");
            }

            if (user.getUsername().contains("@")) {
                return R.fail("用户名不能包含@！");
            }

            Integer count = Math.toIntExact(lambdaQuery().eq(User::getUsername, user.getUsername()).ne(User::getId, BlogUtil.getUserId()).count());
            if (count != 0) {
                return R.fail("用户名重复！");
            }
        }
        User u = new User();
        u.setId(BlogUtil.getUserId());
        u.setUsername(user.getUsername());
        u.setAvatar(user.getAvatar());
        u.setGender(user.getGender());
        u.setIntroduction(user.getIntroduction());
        updateById(u);
        User one = lambdaQuery().eq(User::getId, u.getId()).one();
        qingSiCache.del(CommonConst.USER_CACHE + one.getId());

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(one, userVO);
        userVO.setPassword(null);
        userVO.setAccessToken(BlogUtil.getToken());
        return R.success(userVO);
    }

    @Override
    public R getCode(Integer flag) {
        User user = BlogUtil.getCurrentUser();
        // 生成六位随机数(100000~ 900000)
        int i = new Random().nextInt(900000) + 100000;
        if (flag == 1) {
            if (!StringUtils.hasText(user.getPhoneNumber())) {
                return R.fail("请先绑定手机号！");
            }

            log.debug("{}---{}---手机验证码---{}", user.getId(), user.getUsername(), i);
        } else if (flag == 2) {
            if (!StringUtils.hasText(user.getEmail())) {
                return R.fail("请先绑定邮箱！");
            }

            log.debug("{}---{}---邮箱验证码---{}", user.getId(), user.getUsername(), i);

            List<String> mail = new ArrayList<>();
            mail.add(user.getEmail());
            String text = getCodeMail(i);
            WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);

            AtomicInteger count = JSON.parseObject(qingSiCache.get(CommonConst.CODE_MAIL + mail.get(0)), AtomicInteger.class);
            if (count == null || count.get() < CommonConst.CODE_MAIL_COUNT) {
                mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfo == null ? CommonConst.WEB_DEFAULT_TITLE : webInfo.getWebName()) + "的回执！", text);
                if (count == null) {
                    qingSiCache.put(CommonConst.CODE_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                } else {
                    count.incrementAndGet();
                }
            } else {
                return R.fail("验证码发送次数过多，请明天再试！");
            }
        }
        qingSiCache.put(CommonConst.USER_CODE + BlogUtil.getUserId() + "_" + flag, Integer.valueOf(i), 300);
        return R.success();
    }

    @Override
    public R getCodeForBind(String place, Integer flag) {
        int i = new Random().nextInt(900000) + 100000;
        if (flag == 1) {
            log.debug("{}---手机验证码---{}", place, i);
        } else if (flag == 2) {
            log.debug("{}---邮箱验证码---{}", place, i);
            List<String> mail = new ArrayList<>();
            mail.add(place);
            String text = getCodeMail(i);
            WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);

            AtomicInteger count = JSON.parseObject(qingSiCache.get(CommonConst.CODE_MAIL + mail.get(0)), AtomicInteger.class);
            if (count == null || count.get() < CommonConst.CODE_MAIL_COUNT) {
                mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfo == null ? CommonConst.WEB_DEFAULT_TITLE : webInfo.getWebName()) + "的回执！", text);
                if (count == null) {
                    qingSiCache.put(CommonConst.CODE_MAIL + mail.get(0), new AtomicInteger(1), CommonConst.CODE_EXPIRE);
                } else {
                    count.incrementAndGet();
                }
            } else {
                return R.fail("验证码发送次数过多，请明天再试！");
            }
        }
        qingSiCache.put(CommonConst.USER_CODE + BlogUtil.getUserId() + "_" + place + "_" + flag, Integer.valueOf(i), 300);
        return R.success();
    }

    @Override
    public R<UserVO> updateSecretInfo(String place, Integer flag, String code, String password) {
        password = new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(password));

        User user = getById(BlogUtil.getUserId());
        AuthSessionService.checkUser(user);
        if ((flag == 1 || flag == 2) && !verifyPassword(password, user.getPassword())) {
            return R.fail("密码错误！");
        }
        if ((flag == 1 || flag == 2) && !StringUtils.hasText(code)) {
            return R.fail("请输入验证码！");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        if (flag == 1) {
            Integer count = Math.toIntExact(lambdaQuery().eq(User::getPhoneNumber, place).count());
            if (count != 0) {
                return R.fail("手机号重复！");
            }
            Integer codeCache = JSON.parseObject(qingSiCache.get(CommonConst.USER_CODE + BlogUtil.getUserId() + "_" + place + "_" + flag), Integer.class);
            if (codeCache != null && codeCache.intValue() == Integer.parseInt(code)) {

                qingSiCache.del(CommonConst.USER_CODE + BlogUtil.getUserId() + "_" + place + "_" + flag);

                updateUser.setPhoneNumber(place);
            } else {
                return R.fail("验证码错误！");
            }

        } else if (flag == 2) {
            Integer count = Math.toIntExact(lambdaQuery().eq(User::getEmail, place).count());
            if (count != 0) {
                return R.fail("邮箱重复！");
            }
            Integer codeCache = JSON.parseObject(qingSiCache.get(CommonConst.USER_CODE + BlogUtil.getUserId() + "_" + place + "_" + flag), Integer.class);
            if (codeCache != null && codeCache.intValue() == Integer.parseInt(code)) {

                qingSiCache.del(CommonConst.USER_CODE + BlogUtil.getUserId() + "_" + place + "_" + flag);

                updateUser.setEmail(place);
            } else {
                return R.fail("验证码错误！");
            }
        } else if (flag == 3) {
            if (verifyPassword(place, user.getPassword())) {
                updateUser.setPassword(encodePassword(password));
            } else {
                return R.fail("密码错误！");
            }
        }
        updateById(updateUser);
        if (flag == 3) sessions.revokeAll(user.getId());

        User one = lambdaQuery().eq(User::getId, user.getId()).one();
        qingSiCache.del(CommonConst.USER_CACHE + one.getId());

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(one, userVO);
        userVO.setPassword(null);
        return R.success(userVO);
    }

    /**
     * 注册 获取验证码
     *
     * @param place
     * @param flag
     * @return
     */
    @Override
    public R getCodeByRegister(String place, Integer flag) {
        // 随机6位数字 字符串
        String code = RandomUtil.randomNumbers(6);
        if (flag == 1) {
            log.debug("{}---手机验证码---{}", place, code);
        } else if (flag == 2) {
            log.debug("{}---邮箱验证码---{}", place, code);

            List<String> mail = new ArrayList<>();
            mail.add(place);
            String text = getCodeMail(code);
            WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);

            AtomicInteger count = JSON.parseObject(qingSiCache.get(CommonConst.CODE_MAIL + mail.get(0)), AtomicInteger.class);
            if (count == null || count.get() < CommonConst.CODE_MAIL_COUNT) {
                mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfo == null ? CommonConst.WEB_DEFAULT_TITLE : webInfo.getWebName()) + "的回执！", text);
                if (count == null) {
                    qingSiCache.put(CommonConst.CODE_MAIL + mail.get(0), new AtomicInteger(1).toString(), CommonConst.CODE_EXPIRE);
                } else {
                    count.incrementAndGet();
                }
            } else {
                return R.fail("验证码发送次数过多，请明天再试！");
            }
        }
        qingSiCache.put(CommonConst.CODE_MAIL + place + "_" + flag, code, 300);

        return R.success();
    }


    @Override
    public R updateForForgetPassword(String place, Integer flag, String code, String password) {
        password = new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(password));

        Integer codeCache = JSON.parseObject(qingSiCache.get(CommonConst.FORGET_PASSWORD + place + "_" + flag), Integer.class);
        if (codeCache == null || codeCache != Integer.parseInt(code)) {
            return R.fail("验证码错误！");
        }

        qingSiCache.del(CommonConst.FORGET_PASSWORD + place + "_" + flag);

        if (flag == 1) {
            User user = lambdaQuery().eq(User::getPhoneNumber, place).one();
            if (user == null) {
                return R.fail("该手机号未绑定账号！");
            }

            if (user.getUserStatus() == 0) {
                return R.fail("账号被冻结！");
            }

            lambdaUpdate().eq(User::getPhoneNumber, place).set(User::getPassword, encodePassword(password)).update();
            sessions.revokeAll(user.getId());
            qingSiCache.del(CommonConst.USER_CACHE + user.getId().toString());
        } else if (flag == 2) {
            User user = lambdaQuery().eq(User::getEmail, place).one();
            if (user == null) {
                return R.fail("该邮箱未绑定账号！");
            }

            if (user.getUserStatus() == 0) {
                return R.fail("账号被冻结！");
            }

            lambdaUpdate().eq(User::getEmail, place).set(User::getPassword, encodePassword(password)).update();
            sessions.revokeAll(user.getId());
            qingSiCache.del(CommonConst.USER_CACHE + user.getId().toString());
        }

        return R.success();
    }

    @Override
    public R<PageResult> listUser(BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<User> lambdaQuery = lambdaQuery();

        if (baseReqVO.getUserStatus() != null) {
            lambdaQuery.eq(User::getUserStatus, baseReqVO.getUserStatus());
        }

        if (baseReqVO.getUserType() != null) {
            lambdaQuery.eq(User::getUserType, baseReqVO.getUserType());
        }

        if (StringUtils.hasText(baseReqVO.getSearchKey())) {
            lambdaQuery.and(lq -> lq.like(User::getUsername, baseReqVO.getSearchKey())
                    .or()
                    .like(User::getPhoneNumber, baseReqVO.getSearchKey())
                    .or()
                    .like(User::getEmail, baseReqVO.getSearchKey()));
        }

        lambdaQuery.orderByDesc(User::getCreateTime).page(baseReqVO);

        List<User> records = baseReqVO.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            records.forEach(u -> {
                u.setPassword(null);
                u.setOpenId(null);
            });
        }
        return R.success(PageResult.fromMpPage(baseReqVO));
    }

    @Override
    public R<List<UserVO>> getUserByUsername(String username) {
        List<User> users = lambdaQuery().select(User::getId, User::getUsername, User::getAvatar, User::getGender, User::getIntroduction).like(User::getUsername, username).last("limit 5").list();
        List<UserVO> userVOS = users.stream().map(u -> {
            UserVO userVO = new UserVO();
            userVO.setId(u.getId());
            userVO.setUsername(u.getUsername());
            userVO.setAvatar(u.getAvatar());
            userVO.setIntroduction(u.getIntroduction());
            userVO.setGender(u.getGender());
            return userVO;
        }).collect(Collectors.toList());
        return R.success(userVOS);
    }

    @Override
    public R<AuthSessionVO> token(String userToken) {
        AuthSessionService.Resolved resolved = sessions.resolve(userToken, SessionScope.WEB, false);
        return R.success(AuthSessionVO.from(resolved.session(), resolved.user(), null));
    }


    /**
     * todo 订阅属于行为处理   behHandle
     *
     * @param labelId
     * @param flag
     * @return
     */
    @Override
    public R<UserVO> subscribe(Long labelId, Boolean flag) {
        UserVO userVO = null;
        User one = lambdaQuery().eq(User::getId, BlogUtil.getUserId()).one();
        // 订阅列表存的是标签 ID（bigint 主键），按 ID 一律 Long 的约定解析
        List<Long> sub = JSON.parseArray(one.getSubscribe(), Long.class);
        if (sub == null) sub = new ArrayList<>();
        if (flag) {
            if (!sub.contains(labelId)) {
                sub.add(labelId);
                User user = new User();
                user.setId(one.getId());
                user.setSubscribe(JSON.toJSONString(sub));
                updateById(user);

                // 记录订阅行为
                BehaviorState behaviorState = new BehaviorState();
                behaviorState.setUserId(BlogUtil.getUserId());
                behaviorState.setBizType(BizTypeEnum.TAG.getCode()); // 2表示标签类型
                behaviorState.setBizId(labelId);
                behaviorState.setAction(BehActionType.SUBSCRIBE.getCode());
                behaviorState.setCreateTime(LocalDateTime.now());
                // behaviorLogService.save(behaviorState);

                userVO = new UserVO();
                BeanUtils.copyProperties(one, userVO);
                userVO.setPassword(null);
                userVO.setSubscribe(user.getSubscribe());
                userVO.setAccessToken(BlogUtil.getToken());
            }
        } else {
            if (sub.contains(labelId)) {
                sub.remove(labelId);
                User user = new User();
                user.setId(one.getId());
                user.setSubscribe(JSON.toJSONString(sub));
                updateById(user);

                // 记录取消订阅行为
                BehaviorState behaviorState = new BehaviorState();
                behaviorState.setUserId(BlogUtil.getUserId());
                behaviorState.setBizType(BizTypeEnum.TAG.getCode()); // 2表示标签类型
                behaviorState.setBizId(labelId);
                behaviorState.setAction(BehActionType.UN_SUBSCRIBE.getCode());
                behaviorState.setCreateTime(LocalDateTime.now());
                // behaviorLogService.save(behaviorState);

                userVO = new UserVO();
                BeanUtils.copyProperties(one, userVO);
                userVO.setPassword(null);
                userVO.setSubscribe(user.getSubscribe());
                userVO.setAccessToken(BlogUtil.getToken());
            }
        }
        return R.success(userVO);
    }

    /**
     * 获取图形验证码
     *
     * @param flag
     * @param email
     * @return
     */
    @Override
    public Map<String, String> getCaptcha(Boolean flag, String email) {
        // String captcha = captchaUtils.getPngCaptcha(4);
        Map<String, String> captcha = svgCaptchaUtils.getSvgBase64();
        String code = captcha.get("getCode");

        // 缓存
        String key = UUID.randomUUID().toString();
        qingSiCache.put(CommonConst.CAPTCHA_PREFIX + key, code, 120);
        // 封装返回
        return new HashMap<String, String>() {{
            put("uuid", key);
            put("img", captcha.get("svg"));
        }};
    }

    @Override
    public Boolean checkCaptcha(String uuid, String code) {
        if (!StringUtils.hasText(uuid) || !StringUtils.hasText(code)) {
            return false;
        }
        String key = CommonConst.CAPTCHA_PREFIX + uuid;
        String saved = qingSiCache.get(key);
        return StringUtils.hasText(saved) && saved.equalsIgnoreCase(code);
    }

    /**
     * 根据类型发送邮箱验证码
     *
     * @param bizType
     * @param receiver
     * @return
     */
    @Override
    public R mailCode(String bizType, String receiver) {
        // 随机6位数字 字符串
        String code = RandomUtil.randomNumbers(6);

        log.debug("{}---邮箱验证码---{}", receiver, code);

        List<String> mail = new ArrayList<>();
        mail.add(receiver);// 收件人
        String text = getCodeMail(code, bizType);// 邮件内容
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);

        AtomicInteger count = JSON.parseObject(qingSiCache.get(CommonConst.CODE_MAIL + mail.get(0)), AtomicInteger.class);
        if (count == null || count.get() < CommonConst.CODE_MAIL_COUNT) {
            mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfo == null ? CommonConst.WEB_DEFAULT_TITLE : webInfo.getWebName()) + "的回执！", text);
            if (count == null) {
                qingSiCache.put(CommonConst.CODE_MAIL + mail.get(0), new AtomicInteger(1).toString(), CommonConst.CODE_EXPIRE);
            } else {
                count.incrementAndGet();
            }
        } else {
            return R.fail("验证码发送次数过多，请明天再试！");
        }

        // TODO 暂未找到获取的地方 qingSiCache.put(CommonConst.CODE_MAIL + place + "_" + flag, getCode, 300);
        qingSiCache.put(CommonConst.CODE_MAIL + receiver + "_" + EmailBizType.keyToPfx(bizType), code, 300);

        return R.success();
    }

    /**
     * 重置密码
     *
     * @param email   邮箱
     * @param bizType 业务类型
     * @param code    验证码
     * @param pwd
     * @return
     */
    @Override
    public R resetPwdForFgtPwd(String email, String bizType, String code, String pwd) {
        // dec
        pwd = AESUtil.decrypt(pwd);

        String codeCache = qingSiCache.get(CommonConst.CODE_MAIL + email + "_" + EmailBizType.keyToPfx(bizType));
        if (StrUtil.isBlank(codeCache) || !StrUtil.equals(codeCache, code)) {
            return R.fail("验证码错误！");
        }

        qingSiCache.del(CommonConst.CODE_MAIL + email + "_" + EmailBizType.keyToPfx(bizType));

        // User user = lambdaQuery().eq(User::getEmail, email).one();
        User user = getOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
        if (Objects.isNull(user)) {
            return R.fail("该邮箱未绑定账号！");
        }

        if (!Integer.valueOf(1).equals(user.getUserStatus())) {
            return R.fail("账号被冻结！");
        }

        lambdaUpdate().eq(User::getEmail, email).set(User::getPassword, encodePassword(pwd)).update();
        sessions.revokeAll(user.getId());
        qingSiCache.del(CommonConst.USER_CACHE + user.getId().toString());

        return R.success();
    }

    /**
     * @param code
     * @return
     */
    private String getCodeMail(int code) {
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        String webName = (webInfo == null ? CommonConst.WEB_DEFAULT_TITLE : webInfo.getWebName());

        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.imMail, BlogUtil.getAdminUser().getUsername()),
                BlogUtil.getAdminUser().getUsername(),
                String.format(codeFormat, code),
                "",
                webName);
    }

    private String getCodeMail(String code) {
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        String webName = (webInfo == null ? CommonConst.WEB_DEFAULT_TITLE : webInfo.getWebName());

        // 【Corrain】218360为本次验证的验证码，请在5分钟内完成验证。为保证账号安全，请勿泄漏此验证码。
        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.imMail, BlogUtil.getAdminUser().getUsername()),
                BlogUtil.getAdminUser().getUsername(),
                String.format(codeFormat, code),
                "",
                webName);
    }

    /**
     * 根据业务类型生成邮件内容
     *
     * @param code
     * @param bizType
     * @return
     */
    private String getCodeMail(String code, String bizType) {
        String info = "";

        if (EmailBizType.REGISTER.getCode().equals(bizType)) {
            info = String.format(codeFormat, code);
        } else if (EmailBizType.RESET_PWD.getCode().equals(bizType)) {
            info = String.format(fwdFormat, code);
        } else if (EmailBizType.LOGIN.getCode().equals(bizType)) {
            info = String.format(codeFormat, code);
        }
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        String webName = (webInfo == null ? CommonConst.WEB_DEFAULT_TITLE : webInfo.getWebName());

        // 【Corrain】218360为本次验证的验证码，请在5分钟内完成验证。为保证账号安全，请勿泄漏此验证码。
        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.imMail, BlogUtil.getAdminUser().getUsername()),
                BlogUtil.getAdminUser().getUsername(),
                info,
                "",
                webName);
    }

    // ========== BCrypt密码加密与迁移 ==========

    /**
     * 使用BCrypt编码密码
     */
    private String encodePassword(String rawPassword) {
        return credentials.encode(rawPassword);
    }

    /**
     * 验证密码（兼容MD5旧密码，不触发升级）
     *
     * @param rawPassword 明文密码
     * @param storedHash  数据库中存储的密码哈希
     * @return true-验证通过
     */
    private boolean verifyPassword(String rawPassword, String storedHash) {
        return credentials.matches(rawPassword, storedHash);
    }

    @Override
    public User getOwner() {
        return getOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUserType, UserTypeEnum.ADMIN.getCode())
                .orderByAsc(User::getId)
                .last("limit 1"));
    }

}


