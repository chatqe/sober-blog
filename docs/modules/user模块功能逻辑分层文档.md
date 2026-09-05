# User 模块功能逻辑详细分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + Redis 缓存 + BCrypt/MD5 双密码体系 + t-io WebSocket

---

## 模块概览

```
user/
  ├── controller/
  │   ├── web/
  │   │   └── UserController.java               # 用户端：注册/登录/登出/更新信息/验证码/绑定/找回密码/订阅/图形验证码/邮箱验证码
  │   └── admin/
  │       └── AdminUserController.java          # 管理端：用户列表/状态修改/赞赏修改/类型修改
  ├── service/
  │   ├── UserService.java
  │   └── UserServiceImpl.java                  # 核心业务逻辑 (~910行，含 BCrypt+MD5 双密码体系)
  ├── domain/
  │   ├── pojo/User.java                        # 用户实体
  │   ├── dao/UserMapper.java                   # 继承 CoreMapper<User>
  │   └── dto/vo/                               # UserVO（含 accessToken、code 等业务字段）
  ├── convert/
  │   └── UserConvert.java                      # MapStruct 转换器
  └── enums/
      └── UserTypeEnum.java                     # 用户类型枚举：ADMIN(0)/DEV(1)/USER(2)
```

### 依赖的外部模块

| 依赖模块 | 用途 |
|----------|------|
| `moment` | 注册时自动创建"到此一游"动态 |
| `im` (t-io WebSocket) | 自动加入默认群、自动添加站长为好友、登出时断开连接 |
| `behavior` | 订阅/取消订阅行为日志记录（BehaviorState + BehActionType） |
| `webInfo` | 邮件模板中使用网站名称缓存 |
| `mail` | 验证码邮件发送 |

---

## 实体字段说明

**`User`** (继承 `BaseDO`)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 雪花ID |
| username | String | 用户名 |
| password | String | BCrypt 哈希（旧数据为 MD5 32位） |
| phoneNumber | String | 手机号（唯一） |
| email | String | 邮箱（唯一） |
| userStatus | Integer | 用户状态：0=冻结，1=正常 |
| gender | Integer | 性别：0=保密，1=男，2=女 |
| openId | String | 第三方登录标识 |
| avatar | String | 头像URL |
| admire | String | 赞赏信息 |
| subscribe | String | 订阅的标签ID JSON数组 |
| introduction | String | 个人简介 |
| userType | Integer | 用户类型：0=站长，1=管理员，2=普通用户 |
| updateBy | String | 最后修改人 |
| isDel | Integer | 逻辑删除：1=已删除，0=正常（BaseDO） |
| createTime | LocalDateTime | 创建时间（BaseDO） |
| updateTime | LocalDateTime | 更新时间（BaseDO） |

**`UserVO`** 扩展字段

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | String | 登录令牌 |
| code | String | 验证码 |
| isBoss | Boolean | 是否为管理员 |

---

## 常量与配置

| 常量 | 值 | 说明 |
|------|-----|------|
| `CommonConst.CRYPOTJS_KEY` | String | AES 解密密钥（前端 JS 加密用） |
| `CommonConst.TOKEN_EXPIRE` | 864000秒 (10天) | Token 有效期 |
| `CommonConst.CODE_EXPIRE` | 86400秒 (1天) | 验证码发送频率限制窗口 |
| `CommonConst.CODE_MAIL_COUNT` | 3 | 每日最大邮件验证码发送次数 |
| `CommonConst.USER_ACCESS_TOKEN` | "user_access_token_" | 普通用户 Token 前缀 |
| `CommonConst.ADMIN_ACCESS_TOKEN` | "admin_access_token_" | 管理员 Token 前缀 |
| `CommonConst.USER_TOKEN` | "user_token_" | 用户ID -> Token 映射键前缀 |
| `CommonConst.ADMIN_TOKEN` | "admin_token_" | 用户ID -> AdminToken 映射键前缀 |
| `CommonConst.USER_CACHE` | "user_" | 用户缓存键前缀 |
| `CommonConst.CAPTCHA_PREFIX` | "captcha_prefix" | 图形验证码缓存键前缀 |
| `CommonConst.CODE_MAIL` | "code_mail_" | 邮件发送计数器键前缀 |
| `CommonConst.USER_CODE` | "user_code_" | 用户验证码缓存键前缀 |
| `CommonConst.FORGET_PASSWORD` | "forget_password_" | 找回密码验证码键前缀 |
| `UserTypeEnum.ADMIN` | 0 | 站长 |
| `UserTypeEnum.DEV` | 1 | 管理员 |
| `UserTypeEnum.USER` | 2 | 普通用户 |

---

## 场景一：用户登录

**Controller** `POST /user/login`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| account | String | 是 | 用户名 / 邮箱 / 手机号（三者之一） |
| password | String | 是 | AES 加密后的密码（前端 CryptoJS 加密） |
| isAdmin | Boolean | 否 | 是否管理员登录，默认 false |

**Service 核心事务流程：**

```
public R<UserVO> login(String account, String password, Boolean isAdmin) {

    // Step 1: 密码 AES 解密（前端 CryptoJS 加密）
    password = AESDecrypt(password, CommonConst.CRYPOTJS_KEY);

    // Step 2: 多条件查询用户（用户名 OR 邮箱 OR 手机号）
    User user = lambdaQuery()
        .and(w -> w.eq(User::getUsername, account)
                   .or().eq(User::getEmail, account)
                   .or().eq(User::getPhoneNumber, account))
        .one();

    if (user == null) {
        return R.fail("账号不存在！请先注册");
    }

    // Step 3: 账号状态校验
    if (user.getUserStatus() == 0) {
        return R.fail("账号被冻结！");
    }

    // Step 4: 密码验证 + 自动升级（BCrypt / MD5 双密码体系）
    if (!verifyAndUpgrade(password, user)) {
        return R.fail("账号/密码错误，请重新输入！");
    }

    // Step 5: Token 管理（区分管理员/普通用户）
    String token;
    if (isAdmin) {
        // 校验是否为管理员类型
        if (user.getUserType() != UserTypeEnum.ADMIN.code()
            && user.getUserType() != UserTypeEnum.DEV.code()) {
            return R.fail("请输入管理员账号！");
        }
        // 尝试复用已有 admin token
        token = qingSiCache.get(CommonConst.ADMIN_TOKEN + user.getId());
        if (!StringUtils.hasText(token)) {
            token = CommonConst.ADMIN_ACCESS_TOKEN + UUID;
            // 双向缓存：token -> User 对象；userId -> token
            qingSiCache.put(token, user, CommonConst.TOKEN_EXPIRE);
            qingSiCache.put(CommonConst.ADMIN_TOKEN + user.getId(), token, CommonConst.TOKEN_EXPIRE);
        }
    } else {
        token = qingSiCache.get(CommonConst.USER_TOKEN + user.getId());
        if (!StringUtils.hasText(token)) {
            token = CommonConst.USER_ACCESS_TOKEN + UUID;
            qingSiCache.put(token, user, CommonConst.TOKEN_EXPIRE);
            qingSiCache.put(CommonConst.USER_TOKEN + user.getId(), token, CommonConst.TOKEN_EXPIRE);
        }
    }

    // Step 6: 构建响应（排除 password 字段）
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(user, vo);
    vo.setPassword(null);
    vo.setAccessToken(token);
    if (isAdmin && user.getUserType() == UserTypeEnum.ADMIN.code()) {
        vo.setIsBoss(true);
    }
    return R.success(vo);
}
```

**密码验证与自动升级逻辑：**

```
private boolean verifyAndUpgrade(String rawPassword, User user) {
    String storedHash = user.getPassword();

    // BCrypt 密码（$2a$ 开头）：直接使用 Spring Security BCrypt 验证
    if (storedHash.startsWith("$2a$")) {
        return passwordEncoder.matches(rawPassword, storedHash);
    }

    // 旧 MD5 密码（32位 hex）：验证通过后自动升级为 BCrypt
    if (storedHash.length() == 32) {
        boolean matched = DigestUtils.md5DigestAsHex(rawPassword.getBytes()).equals(storedHash);
        if (matched) {
            // 自动升级：将 MD5 替换为 BCrypt 哈希
            String newHash = passwordEncoder.encode(rawPassword);
            lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getPassword, newHash)
                .update();
            user.setPassword(newHash);
            log.info("用户 {} 的密码已从MD5自动升级为BCrypt", user.getId());
        }
        return matched;
    }
    return false;
}
```

**关键设计点：**
- 登录支持三种身份标识：用户名、邮箱、手机号
- 密码解密在客户端完成（CryptoJS AES），服务端接收密文后再解密
- BCrypt/MD5 双密码体系：新密码走 BCrypt，旧 MD5 密码在登录时透明升级到 BCrypt
- Token 采用 Redis 双向映射：`token -> User` 和 `userId -> token`
- 管理员 Token 与普通用户 Token 分离（不同前缀），且管理员登录需校验 userType

---

## 场景二：Token 登录

**Controller** `POST /user/token`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userToken | String | 是 | AES 加密后的 Token |

**Service 流程：**

```
public R<UserVO> token(String userToken) {
    // Step 1: AES 解密
    userToken = AESDecrypt(userToken);

    // Step 2: 从 Redis 查询用户
    User user = JSON.parseObject(qingSiCache.get(userToken), User.class);
    if (user == null) {
        throw new BizException("登录已过期，请重新登陆！");
    }

    // Step 3: 构建响应
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(user, vo);
    vo.setPassword(null);
    vo.setAccessToken(userToken);
    return R.success(vo);
}
```

---

## 场景三：退出登录

**Controller** `GET /user/logout`

| 拦截器 | 说明 |
|--------|------|
| `@LoginCheck` | 必须已登录 |

**Service 流程：**

```
public R exit() {
    String token = BlogUtil.getToken();
    Long userId = BlogUtil.getUserId();

    if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
        // 普通用户：清除 user 双向缓存
        qingSiCache.del(CommonConst.USER_TOKEN + userId);
        // IM WebSocket 断连
        Tio.removeUser(tioConfig, String.valueOf(userId), "remove user");
    } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
        // 管理员：清除 admin 双向缓存
        qingSiCache.del(CommonConst.ADMIN_TOKEN + userId);
    }
    // 清除 token -> User 缓存
    qingSiCache.del(token);
    return R.success();
}
```

**关键设计点：**
- 根据 Token 前缀判断用户类型，分别清除对应的缓存
- 普通用户退出时自动断开 IM WebSocket 连接（`Tio.removeUser`）
- 管理员退出仅清除 Token 缓存，不断开 WebSocket

---

## 场景四：用户注册

**Controller** `POST /user/register`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名（不能是11位纯数字，不能包含@） |
| password | String | 是 | AES 加密后的密码 |
| phoneNumber | String | 否 | 手机号（与 email 二选一） |
| email | String | 否 | 邮箱（与 phoneNumber 二选一） |
| code | String | 是 | 验证码（手机号或邮箱验证码） |

**Service 核心事务流程：**

```
@Transactional(rollbackFor = Exception.class)
public R<UserVO> regist(UserVO user) {

    // Step 1: 用户名校验
    if (username 匹配 11位数字) {
        return R.fail("用户名不能为11位数字！");
    }
    if (username 包含 "@") {
        return R.fail("用户名不能包含@！");
    }

    // Step 2: 手机号与邮箱二选一校验
    if (有手机号 && 有邮箱) {
        return R.fail("手机号与邮箱只能选择其中一个！");
    }
    if (无手机号 && 无邮箱) {
        return R.fail("请输入邮箱或手机号！");
    }

    // Step 3: 验证码校验
    if (手机号) {
        codeCache = qingSiCache.get(FORGET_PASSWORD + phone + "_1");
        if (codeCache != 用户提交的code) {
            return R.fail("验证码错误！");
        }
        qingSiCache.del(FORGET_PASSWORD + phone + "_1");
    } else {
        codeCache = qingSiCache.get(FORGET_PASSWORD + email + "_2");
        // 同上
    }

    // Step 4: 密码 AES 解密 + BCrypt 编码
    rawPassword = AESDecrypt(user.getPassword());
    hashedPassword = BCryptEncode(rawPassword);

    // Step 5: 唯一性校验
    if (username 已存在) {
        return R.fail("用户名重复！");
    }
    if (手机号已存在) {
        return R.fail("手机号重复！");
    }
    if (邮箱已存在) {
        return R.fail("邮箱重复！");
    }

    // Step 6: 创建用户
    User u = new User();
    u.setUsername(user.getUsername());
    u.setPhoneNumber(user.getPhoneNumber());
    u.setEmail(user.getEmail());
    u.setPassword(hashedPassword);
    u.setAvatar(qingsiUtil.getRandomAvatar());  // 随机头像
    save(u);

    // Step 7: 自动登录（生成 Token）
    User created = getById(u.getId());
    String token = CommonConst.USER_ACCESS_TOKEN + UUID;
    qingSiCache.put(token, created, CommonConst.TOKEN_EXPIRE);
    qingSiCache.put(CommonConst.USER_TOKEN + created.getId(), token, CommonConst.TOKEN_EXPIRE);

    // Step 8: 自动创建"到此一游"动态
    Moment moment = new Moment();
    moment.setUserId(created.getId());
    moment.setContent("到此一游");
    moment.setHasText(true);
    moment.setVisibility(PUBLIC);
    moment.setSource(1);
    moment.setStatus(1);
    momentService.save(moment);

    // Step 9: 自动加入 IM 默认群
    ImChatGroupUser groupUser = new ImChatGroupUser();
    groupUser.setGroupId(DEFAULT_GROUP_ID);
    groupUser.setUserId(created.getId());
    groupUser.setUserStatus(GROUP_USER_STATUS_PASS);
    imChatGroupUserMapper.insert(groupUser);

    // Step 10: 自动添加站长为好友（双向）
    ImChatUserFriend friend1 = new ImChatUserFriend();
    friend1.setUserId(created.getId());
    friend1.setFriendId(ADMIN_USER_ID);
    friend1.setRemark("站长");
    friend1.setFriendStatus(FRIEND_STATUS_PASS);
    imChatUserFriendMapper.insert(friend1);

    ImChatUserFriend friend2 = new ImChatUserFriend();
    friend2.setUserId(ADMIN_USER_ID);
    friend2.setFriendId(created.getId());
    friend2.setFriendStatus(FRIEND_STATUS_PASS);
    imChatUserFriendMapper.insert(friend2);

    // Step 11: 构建响应
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(created, vo);
    vo.setPassword(null);
    vo.setAccessToken(token);
    return R.success(vo);
}
```

**注册后置自动化流程：**

```
注册成功
  ├── 自动登录（生成 User Token）
  ├── 自动创建"到此一游" Moment
  ├── 自动加入 IM 默认群（groupId=DEFAULT_GROUP_ID，状态=已通过）
  └── 自动添加站长好友（双向关系，friendStatus=已通过）
```

---

## 场景五：更新用户信息

**Controller** `POST /user/updateUserInfo`

| 拦截器 | 说明 |
|--------|------|
| `@LoginCheck` | 必须已登录 |

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 否 | 新用户名 |
| avatar | String | 否 | 新头像URL |
| gender | Integer | 否 | 新性别 |
| introduction | String | 否 | 新简介 |

**Service 流程：**

```
public R<UserVO> updateUserInfo(UserVO user) {

    // Step 1: 用户名变更校验（如果提供了新用户名）
    if (StringUtils.hasText(user.getUsername())) {
        if (用户名是11位数字 || 包含"@") {
            return R.fail("用户名格式不合法");
        }
        if (用户名已存在(排除自己)) {
            return R.fail("用户名重复！");
        }
    }

    // Step 2: 构建更新对象
    User u = new User();
    u.setId(currentUserId());
    u.setUsername(user.getUsername());
    u.setAvatar(user.getAvatar());
    u.setGender(user.getGender());
    u.setIntroduction(user.getIntroduction());
    updateById(u);

    // Step 3: 查询最新用户 + 刷新缓存
    User updated = getById(currentUserId());
    qingSiCache.put(token, updated, CommonConst.TOKEN_EXPIRE);
    qingSiCache.put(CommonConst.USER_TOKEN + updated.getId(), token, CommonConst.TOKEN_EXPIRE);

    // Step 4: 构建响应
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(updated, vo);
    vo.setPassword(null);
    vo.setAccessToken(token);
    return R.success(vo);
}
```

**Controller 层额外处理：**
- 调用 Service 前先删除 `USER_CACHE + userId` 缓存，确保下次查询获取最新数据

---

## 场景六：验证码获取

### 6.1 已登录用户获取验证码（改绑/修改用）

**Controller** `GET /user/getCode`

| 拦截器 | 说明 |
|--------|------|
| `@LoginCheck` | 必须已登录 |
| `@SaveCheck` | 防重复提交 |

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| flag | Integer | 是 | 1=手机号，2=邮箱 |

**Service 流程：**

```
public R getCode(Integer flag) {
    User user = getCurrentUser();

    // Step 1: 前置校验
    if (flag == 1 && !user 有手机号) {
        return R.fail("请先绑定手机号！");
    }
    if (flag == 2 && !user 有邮箱) {
        return R.fail("请先绑定邮箱！");
    }

    // Step 2: 生成6位随机验证码 (100000~999999)
    int code = random.nextInt(900000) + 100000;

    // Step 3: 邮箱场景额外处理
    if (flag == 2) {
        // 邮件发送频率限制：每天最多 CODE_MAIL_COUNT(3) 次
        count = qingSiCache.get(CODE_MAIL + email);
        if (count >= 3) {
            return R.fail("验证码发送次数过多，请明天再试！");
        }
        // 发送邮件（使用 WebInfo 缓存中的网站名称）
        mailUtil.sendMail(email, subject, buildCodeMailText(code));
        // 更新计数器
        qingSiCache.put(CODE_MAIL + email, count + 1, CommonConst.CODE_EXPIRE);
    }

    // Step 4: 缓存验证码（5分钟有效）
    qingSiCache.put(USER_CODE + userId + "_" + flag, code, 300);

    return R.success();
}
```

### 6.2 绑定手机号/邮箱获取验证码

**Controller** `GET /user/getCodeForBind`

| 拦截器 | 说明 |
|--------|------|
| `@LoginCheck` | 必须已登录 |
| `@SaveCheck` | 防重复提交 |

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| place | String | 是 | 手机号或邮箱 |
| flag | Integer | 是 | 1=手机号，2=邮箱 |

**流程：** 与 6.1 类似，但验证码缓存键使用 `USER_CODE + userId + "_" + place + "_" + flag`，以便 updateSecretInfo 时按 place 精确匹配。

### 6.3 注册/找回密码获取验证码

**Controller** `GET /user/getCodeByRegister`

| 拦截器 | 说明 |
|--------|------|
| `@SaveCheck` | 防重复提交（不需要登录） |

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| place | String | 是 | 手机号或邮箱 |
| flag | Integer | 是 | 1=手机号，2=邮箱 |

**流程：** 生成6位随机数，邮箱场景同样受每日3次限制，验证码缓存在 `FORGET_PASSWORD + place + "_" + flag`。

### 6.4 新邮箱验证码接口

**Controller** `GET /user/mailCode`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bizType | String | 是 | 业务类型：register / reset_password / login |
| email | String | 是 | 邮箱地址 |

**流程：** 生成6位随机数，按 bizType 构建不同邮件内容模板，验证码缓存在 `CODE_MAIL + email + "_" + EmailBizType.keyToPfx(bizType)`。

---

## 场景七：绑定安全信息（手机号/邮箱/改密码）

**Controller** `POST /user/updateSecretInfo`

| 拦截器 | 说明 |
|--------|------|
| `@LoginCheck` | 必须已登录 |

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| place | String | 是 | 新手机号 / 新邮箱 / 原密码 |
| flag | Integer | 是 | 1=手机号，2=邮箱，3=密码 |
| code | String | 否 | 验证码（flag=1或2时必填） |
| password | String | 是 | 新手机号 / 新邮箱 / 新密码（AES加密） |

**Service 流程：**

```
public R<UserVO> updateSecretInfo(String place, Integer flag, String code, String password) {
    // Step 1: 密码解密
    rawPassword = AESDecrypt(password);

    User user = getCurrentUser();

    // Step 2: flag=1或2时校验原密码
    if (flag == 1 || flag == 2) {
        if (!verifyPassword(rawPassword, user.getPassword())) {
            return R.fail("密码错误！");
        }
        if (!StringUtils.hasText(code)) {
            return R.fail("请输入验证码！");
        }
    }

    // Step 3: 分场景处理
    if (flag == 1) {
        // 绑定/更换手机号
        if (place 手机号已存在) {
            return R.fail("手机号重复！");
        }
        if (验证码不匹配) {
            return R.fail("验证码错误！");
        }
        updateUser.setPhoneNumber(place);
    } else if (flag == 2) {
        // 绑定/更换邮箱
        if (place 邮箱已存在) {
            return R.fail("邮箱重复！");
        }
        if (验证码不匹配) {
            return R.fail("验证码错误！");
        }
        updateUser.setEmail(place);
    } else if (flag == 3) {
        // 修改密码
        if (verifyPassword(place, user.getPassword())) {  // place = 原密码
            updateUser.setPassword(BCryptEncode(rawPassword));
        } else {
            return R.fail("密码错误！");
        }
    }

    // Step 4: 更新 + 刷新缓存
    updateById(updateUser);
    qingSiCache.put(token, updatedUser, TOKEN_EXPIRE);
    qingSiCache.put(USER_TOKEN + userId, token, TOKEN_EXPIRE);

    // Step 5: 构建响应
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(updatedUser, vo);
    vo.setPassword(null);
    return R.success(vo);
}
```

---

## 场景八：忘记密码

### 8.1 旧版接口

**Controller** `POST /user/updateForForgetPassword`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| place | String | 是 | 手机号或邮箱 |
| flag | Integer | 是 | 1=手机号，2=邮箱 |
| code | String | 是 | 验证码 |
| password | String | 是 | 新密码（AES加密） |

**Service 流程：**

```
public R updateForForgetPassword(String place, Integer flag, String code, String password) {
    // Step 1: 密码解密
    rawPassword = AESDecrypt(password);

    // Step 2: 验证码校验
    codeCache = qingSiCache.get(FORGET_PASSWORD + place + "_" + flag);
    if (codeCache != code) {
        return R.fail("验证码错误！");
    }
    qingSiCache.del(FORGET_PASSWORD + place + "_" + flag);

    // Step 3: 根据 flag 查找用户
    if (flag == 1) {
        user = lambdaQuery().eq(User::getPhoneNumber, place).one();
    } else {
        user = lambdaQuery().eq(User::getEmail, place).one();
    }
    if (user == null) {
        return R.fail("该xxx未绑定账号！");
    }
    if (user.getUserStatus() == 0) {
        return R.fail("账号被冻结！");
    }

    // Step 4: BCrypt 编码并更新密码
    lambdaUpdate()
        .eq(flag == 1 ? User::getPhoneNumber : User::getEmail, place)
        .set(User::getPassword, BCryptEncode(rawPassword))
        .update();

    // Step 5: 清除用户缓存
    qingSiCache.del(USER_CACHE + user.getId());
    return R.success();
}
```

### 8.2 新版邮箱密码重置接口

**Controller** `POST /user/resetPwdForFgtPwd`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | String | 是 | 邮箱地址 |
| bizType | String | 是 | 业务类型 |
| code | String | 是 | 邮箱验证码 |
| password | String | 是 | 新密码（AES加密） |

**Service 流程：**

```
public R resetPwdForFgtPwd(String email, String bizType, String code, String pwd) {
    // Step 1: AES 解密
    pwd = AESDecrypt(pwd);

    // Step 2: 邮箱验证码校验（从 CODE_MAIL 缓存读取）
    codeCache = qingSiCache.get(CODE_MAIL + email + "_" + EmailBizType.keyToPfx(bizType));
    if (codeCache != code) {
        return R.fail("验证码错误！");
    }
    qingSiCache.del(...);

    // Step 3: 用户校验
    user = lambdaQuery().eq(User::getEmail, email).one();
    if (user == null) { return R.fail("该邮箱未绑定账号！"); }
    if (user.getUserStatus() == 0) { return R.fail("账号被冻结！"); }

    // Step 4: 注意：此处使用 MD5 而非 BCrypt（可能是遗留代码）
    lambdaUpdate()
        .eq(User::getEmail, email)
        .set(User::getPassword, MD5(pwd))
        .update();

    qingSiCache.del(USER_CACHE + user.getId());
    return R.success();
}
```

**注意：** `resetPwdForFgtPwd` 使用 `AESUtil.md5Hex()` 编码密码，而 `updateForForgetPassword` 使用 BCrypt。两个接口的密码编码策略不一致，可能存在遗留问题。

---

## 场景九：订阅/取消订阅

**Controller** 由其他模块（如标签/分类模块）调用，通过 `UserService.subscribe(labelId, flag)` 实现。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| labelId | Integer | 是 | 标签/分类ID |
| flag | Boolean | 是 | true=订阅，false=取消订阅 |

**Service 流程：**

```
public R<UserVO> subscribe(Integer labelId, Boolean flag) {
    User user = getById(currentUserId());

    // Step 1: 解析现有订阅列表（JSON 数组）
    List<Integer> subs = JSON.parseArray(user.getSubscribe(), Integer.class);
    if (subs == null) { subs = new ArrayList<>(); }

    if (flag) {
        // 订阅：去重后添加
        if (!subs.contains(labelId)) {
            subs.add(labelId);
            updateUser.setSubscribe(JSON.toJSONString(subs));
            updateById(updateUser);

            // 记录行为日志（TODO 未启用）
            // behaviorLogService.save(behaviorState);
        }
    } else {
        // 取消订阅
        if (subs.contains(labelId)) {
            subs.remove(Integer.valueOf(labelId));
            updateUser.setSubscribe(JSON.toJSONString(subs));
            updateById(updateUser);

            // 记录行为日志（TODO 未启用）
            // behaviorLogService.save(behaviorState);
        }
    }

    // Step 2: 返回更新后的用户信息
    UserVO vo = buildUserVO(user);
    vo.setSubscribe(updateUser.getSubscribe());
    return R.success(vo);
}
```

**与行为模块集成（TODO）：**
- `BehaviorState` 记录订阅/取消订阅行为
- `BehActionType.SUBSCRIBE` / `UN_SUBSCRIBE`
- `BizTypeEnum.TAG.code() = 2`
- 当前行为日志保存代码被注释，待接入行为策略工厂

---

## 场景十：图形验证码

### 10.1 获取图形验证码

**Controller** `GET /user/captcha`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| flag | Boolean | 是 | 是否注册场景 |
| email | String | 是 | 邮箱地址 |

**Service 流程：**

```
public Map<String, String> getCaptcha(Boolean flag, String email) {
    // Step 1: 生成 SVG 图形验证码
    Map<String, String> captcha = svgCaptchaUtils.getSvgBase64();
    String code = captcha.get("code");

    // Step 2: 缓存验证码（UUID 作为 key）
    String key = UUID.randomUUID().toString();
    qingSiCache.put(CAPTCHA_PREFIX + key, code, 120);  // 120秒 TTL

    // Step 3: 返回
    return { "uuid": key, "img": captcha.get("svg") };
}
```

### 10.2 校验图形验证码

**Controller** `POST /user/captchaCheck`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| uuid | String | 是 | 验证码ID |
| code | String | 是 | 验证码内容 |

**Service 流程：**

```
public Boolean checkCaptcha(String uuid, String code) {
    if (!StringUtils.hasText(uuid) || !StringUtils.hasText(code)) {
        return false;
    }
    String saved = qingSiCache.get(CAPTCHA_PREFIX + uuid);
    return StringUtils.hasText(saved) && saved.equalsIgnoreCase(code);
}
```

---

## 场景十一：管理端用户列表

**Controller** `POST /admin/user/list`

| 拦截器 | 说明 |
|--------|------|
| `@LoginCheck(0)` | 管理员权限 |

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userStatus | Integer | 否 | 按状态筛选 |
| userType | Integer | 否 | 按类型筛选 |
| searchKey | String | 否 | 关键词搜索（用户名/手机号/邮箱模糊匹配） |
| page | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页条数 |

**Service 流程：**

```
public R<Page> listUser(BaseReqVO baseReqVO) {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

    // 条件筛选
    if (baseReqVO.getUserStatus() != null) {
        wrapper.eq(User::getUserStatus, baseReqVO.getUserStatus());
    }
    if (baseReqVO.getUserType() != null) {
        wrapper.eq(User::getUserType, baseReqVO.getUserType());
    }
    if (StringUtils.hasText(baseReqVO.getSearchKey())) {
        wrapper.and(w -> w.like(User::getUsername, searchKey)
                .or().like(User::getPhoneNumber, searchKey)
                .or().like(User::getEmail, searchKey));
    }

    // 按创建时间倒序
    wrapper.orderByDesc(User::getCreateTime);

    // 分页查询
    Page<User> page = page(baseReqVO.toPage(), wrapper);

    // 脱敏：移除 password 和 openId
    for (User u : page.getRecords()) {
        u.setPassword(null);
        u.setOpenId(null);
    }

    return R.success(baseReqVO);
}
```

---

## 场景十二：管理端修改用户状态

**Controller** `GET /admin/user/changeUserStatus`

| 拦截器 | 说明 |
|--------|------|
| `@LoginCheck(0)` | 管理员权限 |

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Integer | 是 | 目标用户ID |
| flag | Boolean | 是 | true=解禁，false=封禁 |

**Service 流程：**

```
public R changeUserStatus(Integer userId, Boolean flag) {
    // 站长不可被封禁
    if (userId == BlogUtil.getAdminUser().getId()) {
        return R.fail("站长状态不能修改！");
    }

    // 条件更新：只有目标状态与当前状态不同时才执行
    LambdaUpdateChainWrapper<User> updater = lambdaUpdate()
        .eq(User::getId, userId);

    if (flag) {
        // 解禁：仅当当前为冻结状态时执行
        updater.eq(User::getUserStatus, DISABLE.code())
               .set(User::getUserStatus, ENABLE.code());
    } else {
        // 封禁：仅当当前为正常状态时执行
        updater.eq(User::getUserStatus, ENABLE.code())
               .set(User::getUserStatus, DISABLE.code());
    }
    updater.update();

    // 强制下线
    logout(userId);
    return R.success();
}
```

---

## 场景十三：管理端修改用户类型

**Controller** `GET /admin/user/changeUserType`

| 拦截器 | 说明 |
|--------|------|
| `@LoginCheck(0)` | 管理员权限 |

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Integer | 是 | 目标用户ID |
| userType | Integer | 是 | 新类型：0=站长，1=管理员，2=普通用户 |

**Service 流程：**

```
public R changeUserType(Integer userId, Integer userType) {
    if (userId == BlogUtil.getAdminUser().getId()) {
        return R.fail("站长类型不能修改！");
    }
    if (userType 不在 [0, 1, 2]) {
        return R.fail(PARAMETER_ERROR);
    }
    lambdaUpdate().eq(User::getId, userId).set(User::getUserType, userType).update();
    logout(userId);
    return R.success();
}
```

---

## 场景十四：管理端修改用户赞赏信息

**Controller** `GET /admin/user/changeUserAdmire`

| 拦截器 | 说明 |
|--------|------|
| `@LoginCheck(0)` | 管理员权限 |

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Integer | 是 | 目标用户ID |
| admire | String | 是 | 新赞赏信息 |

**Service 流程：**

```
public R changeUserAdmire(Integer userId, String admire) {
    lambdaUpdate()
        .eq(User::getId, userId)
        .set(User::getAdmire, admire)
        .update();
    qingSiCache.del(CommonConst.ADMIN);  // 清除全站缓存
    return R.success();
}
```

---

## 公共方法：管理员强制下线

**AdminUserController.logout(userId)**

```
private void logout(Integer userId) {
    // 清除管理员 Token 双向缓存
    if (qingSiCache.get(ADMIN_TOKEN + userId) != null) {
        String token = qingSiCache.get(ADMIN_TOKEN + userId);
        qingSiCache.del(ADMIN_TOKEN + userId);
        qingSiCache.del(token);
    }

    // 清除普通用户 Token 双向缓存
    if (qingSiCache.get(USER_TOKEN + userId) != null) {
        String token = qingSiCache.get(USER_TOKEN + userId);
        qingSiCache.del(USER_TOKEN + userId);
        qingSiCache.del(token);
    }

    // 断开 IM WebSocket 连接
    TioWebsocketStarter tio = TioUtil.getTio();
    if (tio != null) {
        Tio.removeUser(tio.getServerTioConfig(), String.valueOf(userId), "remove user");
    }
}
```

---

## 附录A：密码体系详解

### BCrypt + MD5 双密码兼容

| 密码格式 | 特征 | 验证方式 | 升级策略 |
|----------|------|----------|----------|
| BCrypt | `$2a$` 开头 | `passwordEncoder.matches()` | 无需升级 |
| MD5 | 32位十六进制字符串 | `DigestUtils.md5DigestAsHex()` | 登录时自动升级为 BCrypt |

**升级时机：** 仅在登录场景的 `verifyAndUpgrade()` 中触发。注册和找回密码场景统一使用 BCrypt。

**注意：** `resetPwdForFgtPwd` 接口使用 MD5 编码新密码，与整体 BCrypt 策略不一致，建议后续统一。

### 密码加密链路

```
前端                          后端
  |                             |
  |  AES(明文密码, CRYPOTJS_KEY) |
  |--------------------------->|
  |                             |  AES 解密
  |                             |  BCrypt 编码
  |                             |
  |  <==== Token + UserVO ======|
```

---

## 附录B：Redis 缓存键规范

| 键模式 | 示例 | TTL | 说明 |
|--------|------|-----|------|
| `user_access_token_{uuid}` | `user_access_token_abc123` | 10天 | Token -> User 对象（JSON） |
| `admin_access_token_{uuid}` | `admin_access_token_xyz789` | 10天 | Token -> User 对象（JSON） |
| `user_token_{userId}` | `user_token_1` | 10天 | userId -> Token 映射 |
| `admin_token_{userId}` | `admin_token_1` | 10天 | userId -> AdminToken 映射 |
| `user_{userId}` | `user_1` | - | 用户信息缓存 |
| `user_code_{userId}_{place}_{flag}` | `user_code_1_13800138000_1` | 5分钟 | 登录用户验证码 |
| `forget_password_{place}_{flag}` | `forget_password_13800138000_1` | - | 注册/找回密码验证码 |
| `code_mail_{email}` | `code_mail_test@qq.com` | 1天 | 邮件发送计数器（AtomicInteger） |
| `captcha_prefix_{uuid}` | `captcha_prefix_a1b2c3` | 120秒 | 图形验证码 |
| `webInfo` | `webInfo` | - | 网站信息缓存 |
| `admin` | `admin` | - | 全站管理员缓存 |

---

## 附录C：状态与权限矩阵

| 操作 | Controller | 拦截器 | 校验规则 |
|------|-----------|--------|---------|
| 用户注册 | `POST /user/register` | 无 | 用户名/手机号/邮箱唯一性，验证码正确 |
| 用户登录 | `POST /user/login` | 无 | 账号存在，状态正常，密码正确 |
| Token 登录 | `POST /user/token` | 无 | Token 有效且在缓存中 |
| 退出登录 | `GET /user/logout` | `@LoginCheck` | 清除 Token + IM 断连 |
| 更新用户信息 | `POST /user/updateUserInfo` | `@LoginCheck` | 用户名唯一性 |
| 获取验证码(已登录) | `GET /user/getCode` | `@LoginCheck` + `@SaveCheck` | 已绑定手机号/邮箱，每日3次限制 |
| 获取验证码(绑定) | `GET /user/getCodeForBind` | `@LoginCheck` + `@SaveCheck` | 每日3次限制 |
| 获取验证码(注册) | `GET /user/getCodeByRegister` | `@SaveCheck` | 每日3次限制 |
| 绑定安全信息 | `POST /user/updateSecretInfo` | `@LoginCheck` | 原密码校验 + 验证码 + 唯一性 |
| 忘记密码 | `POST /user/updateForForgetPassword` | 无 | 验证码 + 账号未冻结 |
| 邮箱验证码 | `GET /user/mailCode` | 无 | 每日3次限制 |
| 邮箱重置密码 | `POST /user/resetPwdForFgtPwd` | 无 | 邮箱验证码 + 账号未冻结 |
| 图形验证码 | `GET /user/captcha` | 无 | - |
| 图形验证码校验 | `POST /user/captchaCheck` | 无 | - |
| 用户名搜索 | `GET /user/getUserByUsername` | `@LoginCheck` | 模糊匹配，限5条 |
| 管理端用户列表 | `POST /admin/user/list` | `@LoginCheck(0)` | 管理员权限，密码/openId 脱敏 |
| 管理端修改状态 | `GET /admin/user/changeUserStatus` | `@LoginCheck(0)` | 站长不可操作，强制下线 |
| 管理端修改类型 | `GET /admin/user/changeUserType` | `@LoginCheck(0)` | 站长不可操作，强制下线 |
| 管理端修改赞赏 | `GET /admin/user/changeUserAdmire` | `@LoginCheck(0)` | 全站缓存清除 |

---

## 附录D：IM 集成

| 事件 | 处理方式 |
|------|----------|
| 注册 | 自动加入默认群 + 自动添加站长为好友（双向） |
| 登录 | 无特殊处理（WebSocket 由前端维护） |
| 退出登录 | `Tio.removeUser(serverConfig, userId, "remove user")` |
| 管理员封禁/改类型 | 强制下线（清除 Token + 断开 WebSocket） |

**常量参考：**
- `ImConfigConst.DEFAULT_GROUP_ID` - 默认群ID
- `ImConfigConst.GROUP_USER_STATUS_PASS` - 群成员状态：已通过
- `ImConfigConst.FRIEND_STATUS_PASS` - 好友状态：已通过
- `ImConfigConst.GROUP_USER_STATUS_NOT_PASS` - 群成员状态：未通过

---

## 待处理事项

| 项目 | 优先级 | 说明 |
|------|--------|------|
| `resetPwdForFgtPwd` 密码编码统一 | P1 | 该接口使用 MD5 编码，与其他 BCrypt 策略不一致 |
| 订阅行为日志 | P2 | `behaviorLogService.save()` 被注释，待接入行为策略工厂 |
| 图形验证码接口 | P3 | `getCaptcha` 中 `flag` 和 `email` 参数未被使用 |
| `getCodeByRegister` 计数器类型不一致 | P3 | 使用 `AtomicInteger.toString()` 存储，与其他地方直接存 `AtomicInteger` 不一致 |
| 手机号验证码发送 | P3 | `flag=1` 时只打日志，未实际发送短信（代码中有占位） |
