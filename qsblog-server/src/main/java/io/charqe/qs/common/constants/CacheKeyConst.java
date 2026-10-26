package io.charqe.qs.common.constants;

/**
 * Redis / 本地缓存的 key 模板
 *
 * @author sjq
 * @since 2026-01-07 20:24
 */
public final class CacheKeyConst {
    private CacheKeyConst() {}

    /* ====================== Token 相关 ====================== */
    public static final String USER_TOKEN           = "user_token_";
    public static final String ADMIN_TOKEN          = "admin_token_";
    public static final String USER_TOKEN_INTERVAL  = "user_token_interval_";
    public static final String ADMIN_TOKEN_INTERVAL = "admin_token_interval_";
    public static final String USER_ACCESS_TOKEN    = "user_access_token_";
    public static final String ADMIN_ACCESS_TOKEN   = "admin_access_token_";

    /* ====================== 保存次数限制 ====================== */
    public static final String SAVE_COUNT_IP        = "save_count_ip_";
    public static final String SAVE_COUNT_USER_ID   = "save_count_user_id_";

    /* ====================== IP 历史统计 ====================== */
    public static final String IP_HISTORY               = "ip_history";
    public static final String IP_HISTORY_STATISTICS    = "ip_history_statistics";
    public static final String IP_HISTORY_PROVINCE      = "ip_history_province";
    public static final String IP_HISTORY_IP            = "ip_history_ip";
    public static final String IP_HISTORY_HOUR          = "ip_history_hour";
    public static final String IP_HISTORY_COUNT         = "ip_history_count";
    public static final String IP_DETAIL_TODAY          = "ip_detail_today";
    public static final String PROVINCE_TODAY           = "province_today";

    /* ====================== 邮件 & 验证码 ====================== */
    public static final String COMMENT_IM_MAIL  = "comment_im_mail_";
    public static final String USER_CODE        = "user_code_";
    public static final String FORGET_PASSWORD  = "forget_password_";

    /* ====================== 业务对象缓存 ====================== */
    public static final String USER_CACHE           = "user_";
    public static final String COMMENT_COUNT_CACHE  = "comment_count_";
    public static final String USER_ARTICLE_LIST    = "user_article_list_";
    public static final String ARTICLE_LIST         = "article_list";
    public static final String SORT_ARTICLE_LIST    = "sort_article_list";
    public static final String USER_LIKES           = "user_likes_";

    /* ====================== 下拉选项缓存（结构版本 v2） ====================== */

    /**
     * 分类下拉选项缓存前缀，后缀为口径编码（public / all），两种口径不可互相复用
     */
    public static final String CATEGORY_OPTIONS     = "category:options:v2:";

    /**
     * 标签下拉选项缓存（tag 表无状态列，两种口径数据一致，共用一个 key）
     */
    public static final String TAG_OPTIONS          = "tag:options:v2";

    /**
     * 公开标签按分类分片的缓存前缀，失效时必须按前缀批量清理
     */
    public static final String TAG_PUBLIC           = "tag:public:cat_";
}
