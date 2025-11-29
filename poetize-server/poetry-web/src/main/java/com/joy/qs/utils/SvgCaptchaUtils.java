package com.joy.qs.utils;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.RandomUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 图形验证码工具类
 *
 * @author sjq
 * @since 2025-11-29 18:56
 */
@Component
public class SvgCaptchaUtils {

    private static final String BASE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final String[] FONT_FAMILIES = {"Arial, sans-serif", "Helvetica, sans-serif", "Verdana, sans-serif"};
    private static final String[] BG_COLORS = {"#f8f8ff", "#f0f8ff", "#fafafa", "#fffaf0"};

    private RandomGenerator codeGenerator;
    private int width = 100;
    private int height = 40;
    private int codeLength = 4;
    private int lineCount = 3;
    private int noiseCount = 15;

    public SvgCaptchaUtils() {
        this.codeGenerator = new RandomGenerator(BASE_CHARS, codeLength);
    }

    public SvgCaptchaUtils(int width, int height, int codeLength) {
        this.width = width;
        this.height = height;
        this.codeLength = codeLength;
        this.codeGenerator = new RandomGenerator(BASE_CHARS, codeLength);
    }

    /**
     * 生成SVG验证码
     *
     * @return Map包含code和svg两个字段
     */
    public Map<String, String> generate() {
        String code = codeGenerator.generate();
        String svg = generateSvgCode(code);

        Map<String, String> result = new HashMap<>();
        result.put("code", code);
        result.put("svg", svg);
        return result;
    }

    /**
     * 生成Base64编码的SVG验证码
     *
     * @return Map包含code和svg两个字段
     */
    public Map<String, String> getSvgBase64() {
        Map<String, String> result = generate();
        String base64Svg = Base64.encode(result.get("svg"));
        result.put("svg", "data:image/svg+xml;base64," + base64Svg);
        return result;
    }

    public String getSvgBase64Str() {
        String code = codeGenerator.generate();
        String svg = generateSvgCode(code);
        String base64Svg = Base64.encode(svg);
        return "data:image/svg+xml;base64," + base64Svg;
    }


    private String generateSvgCode(String code) {
        StringBuilder svg = new StringBuilder();

        // SVG头部
        svg.append(String.format(
                "<svg xmlns='http://www.w3.org/2000/svg' width='%d' height='%d' viewBox='0 0 %d %d'>",
                width, height, width, height
        ));

        // 背景
        svg.append(String.format(
                "<rect width='100%%' height='100%%' fill='%s'/>",
                RandomUtil.randomEle(BG_COLORS)
        ));

        // 干扰线
        svg.append(generateInterferenceLines());

        // 噪点
        svg.append(generateNoise());

        // 验证码文字
        svg.append(generateText(code));

        svg.append("</svg>");

        return svg.toString();
    }

    /**
     * 生成随机颜色（十六进制格式）
     */
    private String generateRandomColor() {
        // 生成随机的RGB颜色
        int r = RandomUtil.randomInt(30, 230);  // 避免太亮或太暗的颜色
        int g = RandomUtil.randomInt(30, 230);
        int b = RandomUtil.randomInt(30, 230);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    /**
     * 生成随机柔和颜色（适合文字）
     */
    private String generateRandomSoftColor() {
        // 生成柔和的颜色，避免过于刺眼
        int base = RandomUtil.randomInt(0, 6);
        int r, g, b;

        switch (base) {
            case 0: // 蓝色系
                r = RandomUtil.randomInt(40, 120);
                g = RandomUtil.randomInt(100, 180);
                b = RandomUtil.randomInt(150, 230);
                break;
            case 1: // 绿色系
                r = RandomUtil.randomInt(80, 160);
                g = RandomUtil.randomInt(120, 200);
                b = RandomUtil.randomInt(80, 160);
                break;
            case 2: // 红色系
                r = RandomUtil.randomInt(150, 230);
                g = RandomUtil.randomInt(80, 160);
                b = RandomUtil.randomInt(80, 160);
                break;
            case 3: // 紫色系
                r = RandomUtil.randomInt(120, 200);
                g = RandomUtil.randomInt(80, 160);
                b = RandomUtil.randomInt(150, 230);
                break;
            case 4: // 橙色系
                r = RandomUtil.randomInt(200, 240);
                g = RandomUtil.randomInt(120, 180);
                b = RandomUtil.randomInt(40, 100);
                break;
            default: // 棕色系
                r = RandomUtil.randomInt(120, 180);
                g = RandomUtil.randomInt(100, 150);
                b = RandomUtil.randomInt(80, 130);
                break;
        }

        return String.format("#%02x%02x%02x", r, g, b);
    }

    /**
     * 生成随机线条颜色（较深的颜色）
     */
    private String generateRandomLineColor() {
        int base = RandomUtil.randomInt(0, 4);
        int r, g, b;

        switch (base) {
            case 0: // 深蓝
                r = RandomUtil.randomInt(50, 120);
                g = RandomUtil.randomInt(80, 150);
                b = RandomUtil.randomInt(120, 190);
                break;
            case 1: // 深绿
                r = RandomUtil.randomInt(60, 130);
                g = RandomUtil.randomInt(100, 170);
                b = RandomUtil.randomInt(70, 140);
                break;
            case 2: // 深红
                r = RandomUtil.randomInt(130, 200);
                g = RandomUtil.randomInt(70, 140);
                b = RandomUtil.randomInt(70, 140);
                break;
            default: // 深紫
                r = RandomUtil.randomInt(100, 170);
                g = RandomUtil.randomInt(70, 140);
                b = RandomUtil.randomInt(120, 190);
                break;
        }

        return String.format("#%02x%02x%02x", r, g, b);
    }

    private String generateInterferenceLines() {
        StringBuilder lines = new StringBuilder();

        for (int i = 0; i < lineCount; i++) {
            int x1 = RandomUtil.randomInt(width);
            int y1 = RandomUtil.randomInt(height);
            int x2 = RandomUtil.randomInt(width);
            int y2 = RandomUtil.randomInt(height);

            lines.append(String.format(
                    "<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='%s' stroke-width='%.1f' stroke-opacity='%.1f'/>",
                    x1, y1, x2, y2,
                    generateRandomLineColor(),
                    RandomUtil.randomDouble(0.3, 1.2),
                    RandomUtil.randomDouble(0.4, 0.8)
            ));
        }
        return lines.toString();
    }

    private String generateNoise() {
        StringBuilder noise = new StringBuilder();

        for (int i = 0; i < noiseCount; i++) {
            int cx = RandomUtil.randomInt(width);
            int cy = RandomUtil.randomInt(height);
            double radius = RandomUtil.randomDouble(0.1, 1.5);

            noise.append(String.format(
                    "<circle cx='%d' cy='%d' r='%.1f' fill='%s' fill-opacity='%.2f'/>",
                    cx, cy, radius,
                    generateRandomColor(),
                    RandomUtil.randomDouble(0.2, 0.7)
            ));
        }
        return noise.toString();
    }

    private String generateText(String code) {
        StringBuilder text = new StringBuilder();
        int charWidth = width / (codeLength + 1);

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            int baseX = (i + 1) * charWidth - charWidth / 2;
            int baseY = height * 2 / 3;
            int fontSize = RandomUtil.randomInt(22, 30);

            // 随机变换参数
            int rotation = RandomUtil.randomInt(-12, 12);
            int dx = RandomUtil.randomInt(-3, 4);
            int dy = RandomUtil.randomInt(-3, 4);
            String transform = String.format("transform='rotate(%d %d %d) translate(%d %d)'",
                    rotation, baseX, baseY, dx, dy);

            // 文字阴影效果
            text.append(String.format(
                    "<text x='%d' y='%d' font-size='%d' font-family='%s' fill='rgba(0,0,0,0.15)' font-weight='%s' %s>%c</text>",
                    baseX - 1, baseY - 1, fontSize,
                    RandomUtil.randomEle(FONT_FAMILIES),
                    RandomUtil.randomBoolean() ? "bold" : "normal",
                    transform, c
            ));

            // 主文字 - 使用柔和颜色
            text.append(String.format(
                    "<text x='%d' y='%d' font-size='%d' font-family='%s' fill='%s' font-weight='%s' %s>%c</text>",
                    baseX, baseY, fontSize,
                    RandomUtil.randomEle(FONT_FAMILIES),
                    generateRandomSoftColor(),
                    RandomUtil.randomBoolean() ? "bold" : "normal",
                    transform, c
            ));
        }

        return text.toString();
    }
}
