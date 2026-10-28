package io.charqe.qs.modules.file.enums;

/**
 * 派生图变体：宽度固定三档，库内不落派生文件，由 CDN 变换按需生成。
 * <p>
 * 变体只对可变换的静态图片生效（七牛 {@code imageView2} 会丢弃动图帧），
 * 其他类型由调用方降级为原图。
 *
 * @author Joy
 * @since 2026-10-26
 */
public enum FileVariantEnum {

    /** 列表缩略图 */
    THUMB("thumb", 320),

    /** 卡片 / 封面 */
    CARD("card", 800),

    /** 详情大图 */
    HERO("hero", 1600);

    private final String code;

    private final int width;

    FileVariantEnum(String code, int width) {
        this.code = code;
        this.width = width;
    }

    public String getCode() {
        return code;
    }

    public int getWidth() {
        return width;
    }
}
