package com.joy.qs.utils;

import cn.hutool.core.util.RandomUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 图形验证码工具类
 * Hutool 生成“上图同款”SVG图形验证码（Base64内嵌）
 *
 * @author sjq
 * @since 2025-11-27 18:46
 */
public class CaptchaUtil {
    /** 生成验证码并返回 Map：code = 明文, image = data URI */
    public  Map<String, String> createSvgCaptcha(int width, int height, int length) {
        // 1. 随机码
        String code = RandomUtil.randomString(length);

        // 2. 拼 SVG
        StringBuilder svg = new StringBuilder(2048);
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='")
                .append(width).append("' height='").append(height).append("'>")
                .append("<rect width='100%' height='100%' fill='#f8fff8'/>");

        // 2.1 干扰线
        for (int i = 0; i < 3; i++) {
            svg.append(String.format(
                    "<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='%s' stroke-width='%.1f' stroke-opacity='0.7'/>",
                    RandomUtil.randomInt(width), RandomUtil.randomInt(height),
                    RandomUtil.randomInt(width), RandomUtil.randomInt(height),
                    RandomUtil.randomEle("#e5989b,#a8dadc,#6d6875".split(",")),
                    RandomUtil.randomDouble(0.5, 1.2)));
        }
        // 2.2 噪点圆
        for (int i = 0; i < 20; i++) {
            svg.append(String.format(
                    "<circle cx='%d' cy='%d' r='%.1f' fill='%s' fill-opacity='%.2f'/>",
                    RandomUtil.randomInt(width), RandomUtil.randomInt(height),
                    RandomUtil.randomDouble(0.2, 1),
                    RandomUtil.randomEle("#48cae4,#ffb4a2,#ade8f4,#90e0ef".split(",")),
                    RandomUtil.randomDouble(0.3, 0.7)));
        }
        // 2.3 文字（逐字符旋转）
        int x = 10;
        for (char c : code.toCharArray()) {
            int rotate = RandomUtil.randomInt(-15, 15);
            String color = RandomUtil.randomEle("#2a9d8f,#bb3e03,#e5989b".split(","));
            svg.append(String.format(
                    "<text x='%d' y='%d' font-size='%d' transform='rotate(%d %d %d)' font-family='Arial' fill='%s' font-weight='bold'>%s</text>",
                    x, height * 0.7, 30, rotate, x, (int)(height * 0.7), color, c));
            x += 22;
        }
        svg.append("</svg>");

        // 3. Base64
        String base64 = Base64.getEncoder().encodeToString(svg.toString().getBytes(StandardCharsets.UTF_8));
        String dataUrl = "data:image/svg+xml;base64," + base64;

        return new HashMap<>();
    }

}
