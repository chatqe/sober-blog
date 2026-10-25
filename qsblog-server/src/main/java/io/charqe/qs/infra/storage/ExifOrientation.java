package io.charqe.qs.infra.storage;

/**
 * 最小 EXIF Orientation 解析：只走 JPEG 的 APP1 段，不引入额外依赖。
 * <p>
 * 返回值语义：1 正常、2 水平镜像、3 旋转 180°、4 垂直镜像、5 转置、
 * 6 顺时针 90°、7 反转置、8 逆时针 90°；无法解析时按 1 处理。
 */
final class ExifOrientation {

    private static final int TAG_ORIENTATION = 0x0112;

    private ExifOrientation() {
    }

    static int of(byte[] bytes) {
        if (bytes == null || bytes.length < 4
                || (bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8) {
            return 1;
        }
        int pos = 2;
        while (pos + 4 <= bytes.length) {
            if ((bytes[pos] & 0xFF) != 0xFF) {
                pos++;
                continue;
            }
            int marker = bytes[pos + 1] & 0xFF;
            // 无载荷的标记：SOI、TEM、RSTn
            if (marker == 0xD8 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                pos += 2;
                continue;
            }
            // SOS 之后是压缩数据，元数据段已结束
            if (marker == 0xDA || marker == 0xD9) {
                return 1;
            }
            int length = ((bytes[pos + 2] & 0xFF) << 8) | (bytes[pos + 3] & 0xFF);
            if (length < 2 || pos + 2 + length > bytes.length) {
                return 1;
            }
            boolean app1 = marker == 0xE1 && length >= 8
                    && bytes[pos + 4] == 'E' && bytes[pos + 5] == 'x'
                    && bytes[pos + 6] == 'i' && bytes[pos + 7] == 'f'
                    && bytes[pos + 8] == 0 && bytes[pos + 9] == 0;
            if (app1) {
                return parseTiff(bytes, pos + 10, length - 8);
            }
            pos += 2 + length;
        }
        return 1;
    }

    /** 解析 TIFF 头与 IFD0，取出 Orientation 标签。 */
    private static int parseTiff(byte[] bytes, int offset, int available) {
        if (available < 8 || offset + 8 > bytes.length) {
            return 1;
        }
        boolean littleEndian = bytes[offset] == 'I' && bytes[offset + 1] == 'I';
        boolean bigEndian = bytes[offset] == 'M' && bytes[offset + 1] == 'M';
        if (!littleEndian && !bigEndian) {
            return 1;
        }
        int ifd = readInt(bytes, offset + 4, littleEndian);
        if (ifd < 8 || ifd + 2 > available || offset + ifd + 2 > bytes.length) {
            return 1;
        }
        int count = readShort(bytes, offset + ifd, littleEndian);
        for (int i = 0; i < count; i++) {
            int entry = offset + ifd + 2 + i * 12;
            if (entry + 12 > offset + available || entry + 12 > bytes.length) {
                break;
            }
            if (readShort(bytes, entry, littleEndian) == TAG_ORIENTATION) {
                int value = readShort(bytes, entry + 8, littleEndian);
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }

    private static int readShort(byte[] bytes, int offset, boolean littleEndian) {
        if (offset + 2 > bytes.length) {
            return 0;
        }
        int a = bytes[offset] & 0xFF;
        int b = bytes[offset + 1] & 0xFF;
        return littleEndian ? (b << 8) | a : (a << 8) | b;
    }

    private static int readInt(byte[] bytes, int offset, boolean littleEndian) {
        if (offset + 4 > bytes.length) {
            return 0;
        }
        int a = bytes[offset] & 0xFF;
        int b = bytes[offset + 1] & 0xFF;
        int c = bytes[offset + 2] & 0xFF;
        int d = bytes[offset + 3] & 0xFF;
        return littleEndian
                ? (d << 24) | (c << 16) | (b << 8) | a
                : (a << 24) | (b << 16) | (c << 8) | d;
    }
}
