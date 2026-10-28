package io.charqe.qs.modules.file.enums;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件业务场景：决定资源归属与 C 端可上传白名单。
 * <p>
 * 管理端可登记任意场景，C 端只允许 {@code clientUploadable} 为 true 的场景，
 * 未匹配到枚举的场景一律按 {@code unknown} 落库。
 *
 * @author Joy
 * @since 2026-10-26
 */
public enum FileSceneEnum {

    /** 用户头像 */
    AVATAR("avatar", true),

    /** 动态图片 */
    MOMENT("moment", true),

    /** 评论 / 留言涂鸦 */
    COMMENT("comment", true),

    /** 文章封面 */
    ARTICLE_COVER("article-cover", false),

    /** 文章正文图片 */
    ARTICLE_CONTENT("article-content", false),

    /** 站点级素材（背景、图标等） */
    SYSTEM("system", false),

    UNKNOWN("unknown", false);

    private final String code;

    private final boolean clientUploadable;

    FileSceneEnum(String code, boolean clientUploadable) {
        this.code = code;
        this.clientUploadable = clientUploadable;
    }

    public String getCode() {
        return code;
    }

    public boolean isClientUploadable() {
        return clientUploadable;
    }

    /** 缺省场景收敛为 unknown，其余原样保留以兼容后续新增场景。 */
    public static String normalize(String scene) {
        return StringUtils.hasText(scene) ? scene.trim() : UNKNOWN.code;
    }

    /** 按 code 精确匹配；未登记的场景返回 null，供下发上传凭证时拒绝自造场景。 */
    public static FileSceneEnum of(String scene) {
        String code = normalize(scene);
        for (FileSceneEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static boolean clientUploadable(String scene) {
        FileSceneEnum value = of(scene);
        return value != null && value.clientUploadable;
    }

    /** C 端可上传场景清单，用于 /file/config 下发。 */
    public static List<String> clientCodes() {
        List<String> codes = new ArrayList<>();
        for (FileSceneEnum value : values()) {
            if (value.clientUploadable) {
                codes.add(value.code);
            }
        }
        return codes;
    }
}
