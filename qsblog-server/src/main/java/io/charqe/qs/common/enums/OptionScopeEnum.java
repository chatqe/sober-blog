package io.charqe.qs.common.enums;

/**
 * 下拉选项口径
 *
 * <p>PUBLIC 只返回可对外展示的数据；ALL 额外包含停用/隐藏项并带上状态字段，
 * 仅供后台管理端使用，公开端点不得接受该口径（避免通过参数放宽可见性）。
 *
 * @author Joy
 * @since 2026-10-26
 */
public enum OptionScopeEnum {

    /**
     * 公开口径：启用且展示
     */
    PUBLIC("public"),

    /**
     * 后台口径：全部未删除数据，含停用与隐藏
     */
    ALL("all");

    private final String code;

    OptionScopeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 解析口径编码，非法值一律降级为 PUBLIC（失败关闭，不放宽可见性）
     *
     * @param code 口径编码
     * @return 口径枚举
     */
    public static OptionScopeEnum of(String code) {
        return ALL.code.equals(code) ? ALL : PUBLIC;
    }
}
