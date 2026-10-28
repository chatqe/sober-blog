package io.charqe.qs.infra.storage;

import io.charqe.qs.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传净化：魔数嗅探、像素上限、方向纠正与元数据剥离。
 */
class ImageSanitizerTest {

    private final ImageSanitizer sanitizer = new ImageSanitizer();

    @Test
    void jpegIsRedrawnWithoutExif() throws IOException {
        byte[] raw = image("jpg", 320, 180, BufferedImage.TYPE_INT_RGB);

        SanitizedImage result = sanitizer.sanitize(raw, "photo.JPG", "image/jpeg");

        assertTrue(result.isRewritten());
        assertEquals("photo.jpg", result.getFilename());
        assertEquals("image/jpeg", result.getMimeType());
        assertEquals(320, result.getWidth());
        assertEquals(180, result.getHeight());
        assertFalse(contains(result.getBytes(), "Exif"));
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.getBytes()));
        assertEquals(320, decoded.getWidth());
        assertEquals(180, decoded.getHeight());
    }

    @Test
    void orientationSixSwapsDimensions() throws IOException {
        byte[] raw = withOrientation(image("jpg", 200, 100, BufferedImage.TYPE_INT_RGB), 6);

        SanitizedImage result = sanitizer.sanitize(raw, "rotated.jpg", "image/jpeg");

        assertTrue(result.isRewritten());
        assertEquals(100, result.getWidth());
        assertEquals(200, result.getHeight());
        assertFalse(contains(result.getBytes(), "Exif"));
    }

    @Test
    void orientationOneKeepsDimensions() throws IOException {
        byte[] raw = withOrientation(image("jpg", 200, 100, BufferedImage.TYPE_INT_RGB), 1);

        SanitizedImage result = sanitizer.sanitize(raw, "plain.jpg", "image/jpeg");

        assertEquals(200, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void declaredImageWithPdfContentIsRejected() {
        byte[] raw = "%PDF-1.4 fake".getBytes(StandardCharsets.US_ASCII);

        assertThrows(BizException.class, () -> sanitizer.sanitize(raw, "evil.png", "image/png"));
    }

    @Test
    void declaredImageByExtensionAloneIsRejected() {
        byte[] raw = "%PDF-1.4 fake".getBytes(StandardCharsets.US_ASCII);

        assertThrows(BizException.class, () -> sanitizer.sanitize(raw, "evil.jpeg", null));
    }

    @Test
    void nonImageOnlyChecksMagic() {
        byte[] raw = "%PDF-1.4 fake".getBytes(StandardCharsets.US_ASCII);

        SanitizedImage result = sanitizer.sanitize(raw, "doc.pdf", "application/pdf");

        assertFalse(result.isRewritten());
        assertArrayEquals(raw, result.getBytes());
        assertEquals("application/pdf", result.getMimeType());
    }

    @Test
    void gifKeepsOriginalBytes() {
        byte[] raw = Base64.getDecoder()
                .decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

        SanitizedImage result = sanitizer.sanitize(raw, "anim.gif", "image/gif");

        assertFalse(result.isRewritten());
        assertArrayEquals(raw, result.getBytes());
        assertEquals("image/gif", result.getMimeType());
    }

    @Test
    void oversizedPixelsAreRejected() throws IOException {
        ReflectionTestUtils.setField(sanitizer, "maxPixels", 100L);
        byte[] raw = image("png", 300, 200, BufferedImage.TYPE_INT_ARGB);

        BizException error = assertThrows(BizException.class,
                () -> sanitizer.sanitize(raw, "huge.png", "image/png"));
        assertTrue(error.getMsg().contains("像素过大"));
    }

    @Test
    void bmpIsNormalizedToPng() throws IOException {
        byte[] raw = image("bmp", 20, 10, BufferedImage.TYPE_INT_RGB);

        SanitizedImage result = sanitizer.sanitize(raw, "legacy.bmp", "image/bmp");

        assertTrue(result.isRewritten());
        assertEquals("legacy.png", result.getFilename());
        assertEquals("image/png", result.getMimeType());
    }

    @Test
    void pathInFilenameIsStripped() throws IOException {
        byte[] raw = image("png", 10, 10, BufferedImage.TYPE_INT_ARGB);

        SanitizedImage result = sanitizer.sanitize(raw, "../../etc/passwd.png", "image/png");

        assertEquals("passwd.png", result.getFilename());
    }

    @Test
    void emptyBytesAreRejected() {
        assertThrows(BizException.class, () -> sanitizer.sanitize(new byte[0], "a.png", "image/png"));
    }

    @Test
    void unknownPayloadKeepsDeclaredName() {
        byte[] raw = "hello world".getBytes(StandardCharsets.UTF_8);

        SanitizedImage result = sanitizer.sanitize(raw, "note.txt", "text/plain");

        assertFalse(result.isRewritten());
        assertEquals("note.txt", result.getFilename());
        assertArrayEquals(raw, result.getBytes());
    }

    private static byte[] image(String format, int width, int height, int type) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0x3366CC);
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output), "缺少编码器：" + format);
        return output.toByteArray();
    }

    /** 在 SOI 之后插入仅含 Orientation 的 APP1/Exif 段（little-endian TIFF）。 */
    private static byte[] withOrientation(byte[] jpeg, int orientation) {
        byte[] tiff = new byte[]{
                'I', 'I', 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00, // TIFF 头
                0x01, 0x00,                                     // IFD0 条目数
                0x12, 0x01, 0x03, 0x00,                         // tag=0x0112, type=SHORT
                0x01, 0x00, 0x00, 0x00,                         // count=1
                (byte) orientation, 0x00, 0x00, 0x00,           // value
                0x00, 0x00, 0x00, 0x00                          // 下一个 IFD
        };
        byte[] exifBody = new byte[6 + tiff.length];
        exifBody[0] = 'E';
        exifBody[1] = 'x';
        exifBody[2] = 'i';
        exifBody[3] = 'f';
        System.arraycopy(tiff, 0, exifBody, 6, tiff.length);

        int length = exifBody.length + 2;
        byte[] output = new byte[jpeg.length + 2 + length];
        output[0] = jpeg[0];
        output[1] = jpeg[1];
        output[2] = (byte) 0xFF;
        output[3] = (byte) 0xE1;
        output[4] = (byte) (length >> 8);
        output[5] = (byte) length;
        System.arraycopy(exifBody, 0, output, 6, exifBody.length);
        System.arraycopy(jpeg, 2, output, 6 + exifBody.length, jpeg.length - 2);
        return output;
    }

    private static boolean contains(byte[] bytes, String text) {
        byte[] needle = text.getBytes(StandardCharsets.US_ASCII);
        outer:
        for (int i = 0; i + needle.length <= bytes.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (bytes[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
