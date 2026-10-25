package io.charqe.qs.infra.storage;

import io.charqe.qs.common.FileVO;
import io.charqe.qs.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地存储：目录归一 + 写盘前净化。
 */
class LocalUtilTest {

    @TempDir
    Path root;

    private LocalUtil localUtil;

    @BeforeEach
    void setUp() {
        localUtil = new LocalUtil();
        ReflectionTestUtils.setField(localUtil, "uploadUrl", root.toString().replace('\\', '/') + "/");
        ReflectionTestUtils.setField(localUtil, "downloadUrl", "http://localhost/static/");
        ReflectionTestUtils.setField(localUtil, "imageSanitizer", new ImageSanitizer());
    }

    @Test
    void acceptsTrailingSlashDirectoryFromServerKey() throws IOException {
        FileVO vo = vo("cover/22/202610/", "01HX8Z3Q.png", "image/png", png(20, 10));

        FileVO result = localUtil.saveFile(vo);

        Path written = root.resolve("cover/22/202610/01HX8Z3Q.png");
        assertTrue(Files.exists(written), "文件应写入服务端 Key 对应的目录");
        assertEquals("http://localhost/static/cover/22/202610/01HX8Z3Q.png", result.getVisitPath());
        assertEquals("01HX8Z3Q.png", result.getOriginalName());
    }

    @Test
    void writesRedrawnBytesInsteadOfOriginal() throws IOException {
        byte[] raw = jpeg(200, 100);
        FileVO vo = vo("avatar/22/", "photo.jpg", "image/jpeg", raw);

        localUtil.saveFile(vo);

        byte[] saved = Files.readAllBytes(root.resolve("avatar/22/photo.jpg"));
        assertTrue(saved.length > 0);
        // 重绘后的字节与原图不同：元数据被剥离、像素重新编码
        assertFalse(Arrays.equals(raw, saved));
    }

    @Test
    void rejectsPathTraversal() {
        FileVO vo = vo("../etc/", "a.png", "image/png", new byte[]{1, 2, 3});

        assertThrows(BizException.class, () -> localUtil.saveFile(vo));
    }

    @Test
    void rejectsEmptyDirectory() {
        FileVO vo = vo("/", "a.png", "image/png", new byte[]{1, 2, 3});

        assertThrows(BizException.class, () -> localUtil.saveFile(vo));
    }

    @Test
    void refusesToOverwriteExistingObject() throws IOException {
        FileVO first = vo("cover/22/", "a.png", "image/png", png(10, 10));
        localUtil.saveFile(first);

        FileVO second = vo("cover/22/", "a.png", "image/png", png(10, 10));

        BizException error = assertThrows(BizException.class, () -> localUtil.saveFile(second));
        assertTrue(error.getMsg().contains("已存在"));
    }

    @Test
    void rejectsDeclaredImageWithForeignPayload() {
        FileVO vo = vo("cover/22/", "fake.png", "image/png",
                "%PDF-1.4".getBytes(StandardCharsets.US_ASCII));

        assertThrows(BizException.class, () -> localUtil.saveFile(vo));
    }

    private static FileVO vo(String relativePath, String originalName, String contentType, byte[] bytes) {
        FileVO vo = new FileVO();
        vo.setType("articlePicture");
        vo.setStoreType("local");
        vo.setRelativePath(relativePath);
        vo.setOriginalName(originalName);
        vo.setFile(new MockMultipartFile("file", originalName, contentType, bytes));
        return vo;
    }

    private static byte[] png(int width, int height) throws IOException {
        return image("png", width, height);
    }

    private static byte[] jpeg(int width, int height) throws IOException {
        return image("jpg", width, height);
    }

    private static byte[] image(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0x224466);
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
