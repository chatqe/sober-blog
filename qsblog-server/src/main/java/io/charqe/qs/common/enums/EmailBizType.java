package io.charqe.qs.common.enums;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 邮箱验证码业务枚举
 *
 * @author sjq
 * @since 2025-12-05 21:07
 */
public enum EmailBizType implements BaseEnum<String> {
    /**
     * 用户注册
     */
    REGISTER("REGISTER", "用户注册", "user.register", 1),
    
    /**
     * 免密登录
     */
    LOGIN("LOGIN", "免密登录", "user.login", 2),
    
    /**
     * 忘记密码
     */
    RESET_PWD("RESET_PWD", "忘记密码", "user.resetPwd", 3);

    private final String key;
    private final String desc;  // 默认描述，作为fallback
    private final String i18nKey;      // 国际化key
    private final Integer type;

    EmailBizType(String key, String desc, String i18nKey, Integer type) {
        this.key = key;
        this.desc = desc;
        this.i18nKey = i18nKey;
        this.type = type;
    }

    @Override
    public String getCode() {
        return key;
    }
    
    @Override
    public String getDesc() {
        return desc;
    }
    
    public String getI18nKey() {
        return i18nKey;
    }
    
    public Integer getType() {
        return type;
    }

    /**
     * 根据业务类型编码获取业务类型前缀
     *
     * @param key 业务类型编码
     * @return 业务类型前缀，不存在则返回0
     */
    public static Integer keyToPfx(String key) {
        EmailBizType enumItem = getByCode(key);
        return enumItem != null ? enumItem.type : 0;
    }

    /**
     * 根据业务类型编码获取对应的枚举实例
     *
     * @param code 业务类型编码，可为空
     * @return 对应的枚举实例，若不存在则返回null
     */
    public static EmailBizType getByCode(String code) {
        if (Objects.isNull(code)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(enumItem -> enumItem.key.equals(code))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根据业务类型编码获取对应的枚举实例（强校验）
     *
     * @param code 业务类型编码，不能为空
     * @return 对应的枚举实例
     * @throws IllegalArgumentException 当编码不存在时抛出
     * @apiNote 推荐使用此方法进行强校验，确保编码有效性
     */
    public static EmailBizType of(String code) {
        return Optional.ofNullable(getByCode(code))
                .orElseThrow(() -> new IllegalArgumentException("unknown bizType: " + code));
    }
    
    /**
     * 根据业务类型编码安全获取枚举实例
     *
     * @param code 业务类型编码，可为空
     * @return Optional包装的枚举实例，编码不存在时返回Optional.empty()
     * @apiNote 适用于编码可能不存在的场景，避免抛出异常
     */
    public static Optional<EmailBizType> safeOf(String code) {
        return Optional.ofNullable(getByCode(code));
    }
    
    /**
     * 验证业务类型编码是否有效
     *
     * @param code 待验证的业务类型编码
     * @return true-编码有效，false-编码无效
     * @apiNote 适用于仅需验证编码而不需要枚举实例的场景
     */
    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }
}

