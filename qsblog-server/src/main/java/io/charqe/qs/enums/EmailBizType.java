package io.charqe.qs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邮箱验证码业务枚举
 *
 * @author sjq
 * @since 2025-12-05 21:07
 */
@Getter
@AllArgsConstructor
public enum EmailBizType {

    REGISTER("REGISTER", "用户注册", "user.register", 1),
    LOGIN("LOGIN", "免密登录", "user.login", 2),
    RESET_PWD("RESET_PWD", "忘记密码", "user.resetPwd", 3);

    private final String key;
    private final String desc;  // 默认描述，作为fallback
    private final String i18nKey;      // 国际化key
    private final Integer type;

    /**
     * 编码到枚举的映射缓存（线程安全，不可变）
     */
    // private static final Map<String, EmailBizType> CODE_MAP;
    //
    // static {
    //     CODE_MAP = Collections.unmodifiableMap(
    //             Arrays.stream(values())
    //                     .collect(Collectors.toMap(EmailBizType::getKey,
    //                             Function.identity(),
    //                             (v1, v2) -> {
    //                                 throw new IllegalStateException("duplicate code: " + v1);
    //                             })));
    // }


    /**
     * 根据业务类型编码获取业务类型前缀
     *
     * @param key
     * @return
     */
    public static Integer keyToPfx(String key) {
        for (EmailBizType e : EmailBizType.values()) {
            if (e.key.equals(key)) {
                return e.type;
            }
        }
        return 0;
    }


    /**
     * 根据业务类型编码获取对应的枚举实例
     *
     * @param code 业务类型编码，不能为空
     * @return 对应的枚举实例
     * @throws IllegalArgumentException 当编码不存在时抛出
     * @apiNote 推荐使用此方法进行强校验，确保编码有效性
     * @see #safeOf(String)
     */
    // public static EmailBizType of(String code) {
    //     return Optional.ofNullable(CODE_MAP.get(code))
    //             .orElseThrow(() -> new IllegalArgumentException("unknown bizType: " + code));
    // }


    /**
     * 获取业务类型描述
     *
     * @return 默认的业务描述
     * @implNote 当前返回的是默认描述，如需国际化支持可扩展此方法
     * @see #i18nKey 国际化资源键
     */
    // public String getDesc() {
    //     return desc;
    // }

    /**
     * 根据业务类型编码安全获取枚举实例
     *
     * @param code 业务类型编码，可为空
     * @return Optional包装的枚举实例，编码不存在时返回Optional.empty()
     * @apiNote 适用于编码可能不存在的场景，避免抛出异常
     * @see #of(String)
     */
    // public static Optional<EmailBizType> safeOf(String code) {
    //     return Optional.ofNullable(CODE_MAP.get(code));
    // }

    /**
     * 验证业务类型编码是否有效
     *
     * @param code 待验证的业务类型编码
     * @return true-编码有效，false-编码无效
     * @apiNote 适用于仅需验证编码而不需要枚举实例的场景
     * @see #of(String)
     * @see #safeOf(String)
     */
    // public static boolean isValid(String code) {
    //     return CODE_MAP.containsKey(code);
    // }

    /**
     * 获取所有业务类型编码的集合（只读视图）
     *
     * @return 所有有效的业务类型编码集合
     * @implNote 返回的集合为不可变视图，防止外部修改
     */
    // public static Map<String, EmailBizType> getAllCodes() {
    //     return CODE_MAP;
    // }

    // public static List<ProjectAuditStatusEnum> getAllEnum() {
    //     return Arrays.asList(ProjectAuditStatusEnum.values());
    // }
}

