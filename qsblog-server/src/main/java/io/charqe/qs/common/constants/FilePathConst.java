package io.charqe.qs.common.constants;

/**
 * 所有文件上传、资源路径的 type 常量，供 OSS / 本地磁盘 / CDN 统一使用
 *
 * @author sjq
 * @since 2026-01-07 20:25
 */
public final class FilePathConst {
    private FilePathConst() {
    }

    /* ====================== 单资源类型 ====================== */
    public static final String GRAFFITI = "graffiti";
    public static final String ARTICLE_PICTURE = "articlePicture";
    public static final String USER_AVATAR = "userAvatar";
    public static final String ARTICLE_COVER = "articleCover";
    public static final String WEB_BACKGROUND_IMAGE = "webBackgroundImage";
    public static final String WEB_AVATAR = "webAvatar";
    public static final String RANDOM_AVATAR = "randomAvatar";
    public static final String RANDOM_COVER = "randomCover";
    public static final String COMMENT_PICTURE = "commentPicture";
    public static final String INTERNET_MEME = "internetMeme";

    /* ====================== IM ====================== */
    public static final String IM_GROUP_AVATAR = "im/groupAvatar";
    public static final String IM_GROUP_MESSAGE = "im/groupMessage";
    public static final String IM_FRIEND_MESSAGE = "im/friendMessage";

    /* ====================== 好玩 & 收藏 & 视频 ====================== */
    public static final String FUNNY_URL = "funnyUrl";
    public static final String FUNNY_COVER = "funnyCover";
    public static final String FAVORITES_COVER = "favoritesCover";
    public static final String VIDEO_ARTICLE = "video/article";

    /* ====================== 爱情相册 ====================== */
    public static final String LOVE_COVER = "love/bgCover";
    public static final String LOVE_MAN = "love/manCover";
    public static final String LOVE_WOMAN = "love/womanCover";

    /* ====================== 静态资源 ====================== */
    public static final String ASSETS = "assets";

    /* ====================== 聚合资源类型（业务侧透传） ====================== */
    public static final String FRIEND_URL = "friendUrl";
    public static final String FUNNY = "funny";
    public static final String FAVORITES = "favorites";
    public static final String LOVE_PHOTO = "lovePhoto";

    /* ====================== 其他 ====================== */
    public static final String RESOURCE_ARTICLE_DOC = "resource_article_doc";
}
