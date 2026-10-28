package io.charqe.qs.infra.storage;

import io.charqe.qs.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * 上传载荷净化：魔数嗅探 → 像素上限 → 解码重绘 → 去元数据再编码。
 * <p>
 * 只对经过服务端的字节生效（{@code StoreService#saveFile}）。图片一律重绘，
 * 从而剥离 EXIF/GPS 并按 Orientation 纠正方向；动图与 JDK 无解码器的格式保留原字节，
 * 仅做尺寸校验。
 */
@Slf4j
@Component
public class ImageSanitizer {

    /** 允许解码的最大像素总数，默认 4000 万（约 7300×5470），防解压炸弹。 */
    @Value("${upload.image.max-pixels:40000000}")
    private long maxPixels = 40_000_000L;

    /** JPEG 重编码质量。 */
    @Value("${upload.image.quality:0.85}")
    private float quality = 0.85f;

    /**
     * 净化上传字节。
     *
     * @param raw          原始字节
     * @param filename     调用方给出的文件名（可能带路径）
     * @param declaredMime 调用方声明的 MIME，可为空
     * @return 可写入存储的载荷；文件名与 MIME 已按真实类型校正
     */
    public SanitizedImage sanitize(byte[] raw, String filename, String declaredMime) {
        if (raw == null || raw.length == 0) {
            throw new BizException("文件内容为空");
        }
        Magic magic = Magic.detect(raw);
        String name = safeName(filename);
        if (!magic.image) {
            rejectDeclaredImage(declaredMime, name);
            return new SanitizedImage(raw, name, magic.mimeType, 0, 0, false);
        }
        switch (magic) {
            case GIF:
                // 动图重编码会丢帧，只做像素校验后原样保留
                int[] size = dimensions(raw);
                assertPixels(size[0], size[1]);
                return new SanitizedImage(raw, withExtension(name, magic.extension), magic.mimeType, 0, 0, false);
            case WEBP:
            case AVIF:
                // JDK 无内置解码器；保留原字节，尺寸无法读取时跳过像素校验
                log.debug("跳过硬解码格式的重绘：magic={}", magic);
                return new SanitizedImage(raw, withExtension(name, magic.extension), magic.mimeType, 0, 0, false);
            default:
                return rewrite(raw, name, magic);
        }
    }

    /** 解码 → 纠正方向 → 重绘到无元数据的新图 → 再编码。 */
    private SanitizedImage rewrite(byte[] raw, String filename, Magic magic) {
        Magic target = magic == Magic.BMP ? Magic.PNG : magic;
        int[] size = new int[2];
        BufferedImage decoded;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(raw))) {
            if (input == null) {
                throw new BizException("图片解码失败，已拒绝上传");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BizException("图片解码失败，已拒绝上传");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                size[0] = reader.getWidth(0);
                size[1] = reader.getHeight(0);
                assertPixels(size[0], size[1]);
                decoded = reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof BizException) {
                throw (BizException) e;
            }
            log.warn("图片解码失败：magic={}，原因={}", magic, e.getMessage());
            throw new BizException("图片解码失败，已拒绝上传");
        }
        if (decoded == null) {
            throw new BizException("图片解码失败，已拒绝上传");
        }

        int orientation = magic == Magic.JPEG ? ExifOrientation.of(raw) : 1;
        BufferedImage redrawn = redraw(decoded, orientation, target.alpha);
        byte[] encoded = encode(redrawn, target);
        return new SanitizedImage(encoded, withExtension(filename, target.extension),
                target.mimeType, redrawn.getWidth(), redrawn.getHeight(), true);
    }

    /** 重绘到新画布：天然丢弃原图所有元数据（EXIF/GPS/ICC）。 */
    private BufferedImage redraw(BufferedImage source, int orientation, boolean alpha) {
        int type = alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        int width = source.getWidth();
        int height = source.getHeight();
        boolean swapped = orientation >= 5;
        int targetWidth = swapped ? height : width;
        int targetHeight = swapped ? width : height;
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, type);

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int sourceX;
                int sourceY;
                switch (orientation) {
                    case 2: sourceX = width - 1 - x; sourceY = y; break;
                    case 3: sourceX = width - 1 - x; sourceY = height - 1 - y; break;
                    case 4: sourceX = x; sourceY = height - 1 - y; break;
                    case 5: sourceX = y; sourceY = x; break;
                    case 6: sourceX = y; sourceY = height - 1 - x; break;
                    case 7: sourceX = width - 1 - y; sourceY = height - 1 - x; break;
                    case 8: sourceX = width - 1 - y; sourceY = x; break;
                    default: sourceX = x; sourceY = y; break;
                }
                if (sourceX < 0 || sourceX >= width || sourceY < 0 || sourceY >= height) {
                    continue;
                }
                target.setRGB(x, y, source.getRGB(sourceX, sourceY));
            }
        }
        return target;
    }

    /** 按目标格式再编码；JPEG 显式设置质量，PNG 走默认无损。 */
    private byte[] encode(BufferedImage image, Magic target) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            if (target == Magic.JPEG) {
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                if (!writers.hasNext()) {
                    throw new IOException("没有可用的 JPEG 编码器");
                }
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.min(1f, Math.max(0.1f, quality)));
                try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
                    writer.setOutput(stream);
                    writer.write(null, new IIOImage(image, null, null), param);
                } finally {
                    writer.dispose();
                }
            } else if (!ImageIO.write(image, target.format, output)) {
                throw new IOException("没有可用的编码器：" + target.format);
            }
        } catch (IOException e) {
            log.warn("图片重编码失败：target={}，原因={}", target, e.getMessage());
            throw new BizException("图片处理失败，已拒绝上传");
        }
        return output.toByteArray();
    }

    /** 读取宽高但不解码像素；读不到时返回 {0,0}。 */
    private int[] dimensions(byte[] raw) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(raw))) {
            if (input == null) {
                return new int[]{0, 0};
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return new int[]{0, 0};
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            log.debug("读取图片尺寸失败：{}", e.getMessage());
            return new int[]{0, 0};
        }
    }

    private void assertPixels(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if ((long) width * height > maxPixels) {
            throw new BizException("图片像素过大（" + width + "×" + height + "），请压缩后重试");
        }
    }

    /** 声明是图片但内容不是图片时拒绝，避免伪造扩展名直通存储。 */
    private void rejectDeclaredImage(String declaredMime, String filename) {
        boolean declared = declaredMime != null && declaredMime.toLowerCase(Locale.ROOT).startsWith("image/");
        if (!declared) {
            String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
            declared = lower.matches(".*\\.(png|jpe?g|gif|webp|bmp|avif|heic)$");
        }
        if (declared) {
            log.warn("文件内容与图片声明不符：name={}，declaredMime={}", filename, declaredMime);
            throw new BizException("文件内容与图片类型不符，已拒绝上传");
        }
    }

    /** 只取 basename，避免写穿存储目录。 */
    private String safeName(String filename) {
        String name = StringUtils.hasText(filename) ? filename.trim() : "upload_" + System.currentTimeMillis();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.length() > 120) {
            int dot = name.lastIndexOf('.');
            String ext = dot > 0 && name.length() - dot <= 10 ? name.substring(dot) : "";
            name = name.substring(0, 120 - ext.length()) + ext;
        }
        return name;
    }

    private String withExtension(String filename, String extension) {
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        return base + "." + extension;
    }

    /** 常用格式的魔数嗅探；图片与非图片分开判断。 */
    enum Magic {
        JPEG("image/jpeg", "jpg", "jpeg", false, true),
        PNG("image/png", "png", "png", true, true),
        GIF("image/gif", "gif", "gif", true, true),
        WEBP("image/webp", "webp", "webp", true, true),
        BMP("image/bmp", "bmp", "bmp", false, true),
        AVIF("image/avif", "avif", "avif", true, true),
        PDF("application/pdf", "pdf", "pdf", false, false),
        MP4("video/mp4", "mp4", "mp4", false, false),
        WEBM("video/webm", "webm", "webm", false, false),
        OGG("audio/ogg", "ogg", "ogg", false, false),
        WAV("audio/wav", "wav", "wav", false, false),
        MP3("audio/mpeg", "mp3", "mp3", false, false),
        ZIP("application/zip", "zip", "zip", false, false),
        UNKNOWN("application/octet-stream", "bin", "bin", false, false);

        private final String mimeType;
        private final String extension;
        private final String format;
        private final boolean alpha;
        private final boolean image;

        Magic(String mimeType, String extension, String format, boolean alpha, boolean image) {
            this.mimeType = mimeType;
            this.extension = extension;
            this.format = format;
            this.alpha = alpha;
            this.image = image;
        }

        static Magic detect(byte[] b) {
            if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return JPEG;
            }
            if (startsWith(b, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
                return PNG;
            }
            if (startsWith(b, 'G', 'I', 'F', '8')) {
                return GIF;
            }
            if (startsWith(b, 'R', 'I', 'F', 'F') && ascii(b, 8, 4).equals("WEBP")) {
                return WEBP;
            }
            if (startsWith(b, 'R', 'I', 'F', 'F') && ascii(b, 8, 4).equals("WAVE")) {
                return WAV;
            }
            if (startsWith(b, 'B', 'M')) {
                return BMP;
            }
            if (startsWith(b, 0x1A, 0x45, 0xDF, 0xA3)) {
                return WEBM;
            }
            if (startsWith(b, 'O', 'g', 'g', 'S')) {
                return OGG;
            }
            if (startsWith(b, '%', 'P', 'D', 'F')) {
                return PDF;
            }
            if (startsWith(b, 'P', 'K', 0x03, 0x04)) {
                return ZIP;
            }
            if (startsWith(b, 'I', 'D', '3') || (b.length >= 2 && (b[0] & 0xFF) == 0xFF
                    && ((b[1] & 0xFF) == 0xFB || (b[1] & 0xFF) == 0xF3))) {
                return MP3;
            }
            if (b.length >= 12 && ascii(b, 4, 4).equals("ftyp")) {
                String brand = ascii(b, 8, 4);
                return "avif".equals(brand) || "avis".equals(brand) ? AVIF : MP4;
            }
            return UNKNOWN;
        }

        private static String ascii(byte[] bytes, int offset, int length) {
            if (offset + length > bytes.length) {
                return "";
            }
            StringBuilder builder = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                builder.append((char) (bytes[offset + i] & 0xFF));
            }
            return builder.toString();
        }

        private static boolean startsWith(byte[] bytes, int... prefix) {
            if (bytes == null || bytes.length < prefix.length) {
                return false;
            }
            for (int i = 0; i < prefix.length; i++) {
                if ((bytes[i] & 0xFF) != (prefix[i] & 0xFF)) {
                    return false;
                }
            }
            return true;
        }
    }
}
