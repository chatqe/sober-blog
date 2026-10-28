package io.charqe.qs.modules.file.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 服务端 Key 生成：格式、字符集与扩展名兜底。 */
class FileKeyGeneratorTest {

    private final FileKeyGenerator generator = new FileKeyGenerator();

    @Test
    void ulidIs26CharsOfCrockfordBase32() {
        String ulid = generator.ulid();
        assertEquals(26, ulid.length());
        assertTrue(ulid.matches("^[0-9A-HJKMNP-TV-Z]{26}$"), "非 Crockford Base32 字符：" + ulid);
    }

    @Test
    void ulidIsUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertTrue(seen.add(generator.ulid()), "ULID 出现重复");
        }
    }

    @Test
    void ulidPrefixIsTimeSortable() {
        String first = generator.ulid();
        String second = generator.ulid();
        // 前 10 位编码毫秒时间戳，同毫秒内相等，跨毫秒必然递增
        assertTrue(first.substring(0, 10).compareTo(second.substring(0, 10)) <= 0);
    }

    @Test
    void keyFollowsSceneOwnerMonthUlidLayout() {
        String key = generator.build("avatar", 31L, "头像.PNG", "image/png");
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        assertTrue(key.matches("^avatar/31/" + month + "/[0-9A-HJKMNP-TV-Z]{26}\\.png$"), key);
        assertTrue(key.length() < 255);
    }

    @Test
    void missingOwnerFallsBackToZero() {
        assertTrue(generator.build("system", null, "logo.png", "image/png").startsWith("system/0/"));
    }

    @Test
    void extensionIsSanitizedAndLowercased() {
        assertEquals("jpg", generator.extension("A.JPG", "image/jpeg"));
        assertEquals("png", generator.extension("a.p+n g", "image/png"));
        assertEquals("bin", generator.extension("noext", "application/octet-stream"));
        assertEquals("mp4", generator.extension("noext", "video/mp4"));
        assertEquals("abcdefgh", generator.extension("x.abcdefghijkl", "image/png"));
    }

    @Test
    void distinctCallsProduceDistinctKeys() {
        assertNotEquals(generator.build("avatar", 1L, "a.png", "image/png"),
                generator.build("avatar", 1L, "a.png", "image/png"));
    }
}
