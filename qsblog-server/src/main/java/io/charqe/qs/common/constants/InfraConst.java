package io.charqe.qs.common.constants;

/**
 * 保留通用开关、默认值、过期时间、顶级 ID 等纯技术常量
 *
 * @author sjq
 * @since 2026-01-07 20:23
 */
public final class InfraConst {

    private InfraConst() {}

    /* ====================== 系统级 ID / 默认值 ====================== */
    public static final int    ADMIN_USER_ID          = 1;
    public static final int    FIRST_COMMENT          = 0;
    public static final String ADMIN                  = "admin";
    public static final String ADMIN_FAMILY           = "adminFamily";
    public static final String FAMILY_LIST            = "familyList";
    public static final String WEB_INFO               = "webInfo";
    public static final String WEB_DEFAULT_TITLE      = "YoungWanton";
    public static final String SORT_INFO              = "sortInfo";
    public static final String ADMIRE                 = "admire";
    public static final String TOKEN_HEADER            = "Authorization";

    /* ====================== 过期时间（秒） ====================== */
    public static final long TOKEN_EXPIRE             = 864000;   // 10 天
    public static final long CODE_EXPIRE              = 86400;    // 1 天
    public static final long TOKEN_INTERVAL           = 3600;     // 1 小时
    public static final long EXPIRE                   = 1800;     // 30 分钟
    public static final long SAVE_EXPIRE              = 86400;    // 1 天

    /* ====================== 次数限制 ====================== */
    public static final int  SAVE_MAX_COUNT           = 15;
    public static final int  COMMENT_IM_MAIL_COUNT    = 1;
    public static final int  CODE_MAIL_COUNT          = 3;
    public static final int  TREE_HOLE_COUNT          = 100;

    /* ====================== 内容长度 / 源标识 ====================== */
    public static final int  SUMMARY                  = 80;
    public static final int  TREE_HOLE_COMMENT_SOURCE = 0;

    /* ====================== 加密 / 邮件前缀 ====================== */
    public static final String CRYPOTJS_KEY           = "huo!!!jiuzhegeya";
    public static final String COMMENT_MAIL           = "comment_mail_";
    public static final String CODE_MAIL              = "code_mail_";
    public static final String CAPTCHA_PREFIX         = "captcha_prefix";

    /* ====================== 动态 & 友链 ====================== */
    public static final String MOMENT_TYPE_FRIEND     = "weblinks";
    public static final String MOMENT_TYPE_NEWS       = "news";
    public static final String DEFAULT_FRIEND         = "🥇友情链接";
}
