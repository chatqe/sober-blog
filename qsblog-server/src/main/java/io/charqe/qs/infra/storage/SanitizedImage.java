package io.charqe.qs.infra.storage;

import lombok.Getter;

/**
 * 净化后的上传载荷。
 * <p>
 * {@code filename} 与 {@code mimeType} 是重编码后的真实类型，调用方必须用它们重建存储 Key，
 * 避免扩展名与实际字节不一致。
 */
@Getter
public class SanitizedImage {

    /** 可直接写入存储的字节；未重编码时就是原始字节。 */
    private final byte[] bytes;

    /** 校正扩展名后的文件名。 */
    private final String filename;

    /** 嗅探得到的真实 MIME。 */
    private final String mimeType;

    /** 重绘后的宽；未能解码时为 0。 */
    private final int width;

    /** 重绘后的高；未能解码时为 0。 */
    private final int height;

    /** 是否真正重编码（false 表示保留原字节）。 */
    private final boolean rewritten;

    public SanitizedImage(byte[] bytes, String filename, String mimeType,
                          int width, int height, boolean rewritten) {
        this.bytes = bytes;
        this.filename = filename;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
        this.rewritten = rewritten;
    }
}
