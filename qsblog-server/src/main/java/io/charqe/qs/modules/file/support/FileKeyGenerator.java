package io.charqe.qs.modules.file.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 存储 Key 生成器：{@code {scene}/{ownerId}/{yyyyMM}/{ulid}.{ext}}。
 * <p>
 * Key 一律由服务端生成，前端不再自造路径；ULID 前 10 位编码毫秒时间戳，
 * 同前缀下天然按时间有序，便于后台列举与归档。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Component
public class FileKeyGenerator {

    /** Crockford Base32（去掉 I/L/O/U，避免人工识读歧义） */
    private static final char[] CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private static final SecureRandom RANDOM = new SecureRandom();

    /** MIME 缺省扩展名：文件名无后缀时兜底，避免生成无扩展名对象 */
    private static final Map<String, String> EXT_BY_MIME = new HashMap<>();

    static {
        EXT_BY_MIME.put("image/jpeg", "jpg");
        EXT_BY_MIME.put("image/png", "png");
        EXT_BY_MIME.put("image/gif", "gif");
        EXT_BY_MIME.put("image/webp", "webp");
        EXT_BY_MIME.put("image/avif", "avif");
        EXT_BY_MIME.put("image/bmp", "bmp");
        EXT_BY_MIME.put("video/mp4", "mp4");
        EXT_BY_MIME.put("video/webm", "webm");
        EXT_BY_MIME.put("video/quicktime", "mov");
        EXT_BY_MIME.put("audio/mpeg", "mp3");
        EXT_BY_MIME.put("audio/wav", "wav");
        EXT_BY_MIME.put("audio/ogg", "ogg");
        EXT_BY_MIME.put("application/pdf", "pdf");
        EXT_BY_MIME.put("text/plain", "txt");
    }

    /**
     * 组装对象存储 Key。
     *
     * @param scene        已校验的业务场景
     * @param ownerId      拥有者 ID
     * @param originalName 原始文件名，用于取扩展名
     * @param mimeType     声明的 MIME，文件名无扩展名时兜底
     */
    public String build(String scene, Long ownerId, String originalName, String mimeType) {
        String ext = extension(originalName, mimeType);
        return scene + "/" + (ownerId == null ? "0" : ownerId) + "/"
                + LocalDate.now().format(MONTH) + "/" + ulid() + "." + ext;
    }

    /** 生成 26 位 ULID（48 位毫秒时间戳 + 80 位随机数）。 */
    public String ulid() {
        char[] out = new char[26];
        long time = System.currentTimeMillis();
        for (int i = 9; i >= 0; i--) {
            out[i] = CROCKFORD[(int) (time & 0x1F)];
            time >>>= 5;
        }
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);
        int buffer = 0;
        int bits = 0;
        int cursor = 10;
        for (byte b : random) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                out[cursor++] = CROCKFORD[(buffer >>> bits) & 0x1F];
            }
        }
        return new String(out);
    }

    /** 扩展名只保留 [a-z0-9]，长度上限 8，防止拼出穿越路径或超长 Key。 */
    public String extension(String originalName, String mimeType) {
        String ext = StringUtils.getFilenameExtension(originalName);
        String cleaned = ext == null ? "" : ext.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (cleaned.length() > 8) {
            cleaned = cleaned.substring(0, 8);
        }
        if (!cleaned.isEmpty()) {
            return cleaned;
        }
        String mime = mimeType == null ? "" : mimeType.trim().toLowerCase(Locale.ROOT);
        return EXT_BY_MIME.getOrDefault(mime, "bin");
    }
}
