package io.charqe.qs.utils;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.img.ImgUtil;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * 图形验证码工具类
 * Hutool 生成“上图同款”SVG图形验证码（Base64内嵌）
 *
 * @author sjq
 * @since 2025-11-27 18:46
 */
@Component
public class CaptchaUtils extends CaptchaUtil {

    /**
     * 数据流前缀
     */
    private static final String pngPrefix = "data:image/png;base64,";

    // 默认参数
    private static final int width = 100;
    private static final int height = 40;
    private static final int lineCount = 4;
    private static final double lineWidth = 0.9;

    private Color backgroundColor = new Color(0xF8F8FF);


    /**
     * 生成 PNG 图形验证码
     */
    public String getPngCaptcha(Integer length) {
        // 1. 先拿 LineCaptcha 生成 100×40 的底图
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(width, height, length, 15);
        captcha.setBackground(backgroundColor);   // 同 SVG 背景

        // 2. 手动再画 3 条粗一点的干扰线（颜色随机）
        Graphics2D g = captcha.getImage().createGraphics();
        g.setStroke(new BasicStroke(0.9f)); // 线宽
        g.setColor(new Color(0xB5838D)); // 线 1
        g.drawLine(14, 37, 78, 0);
        g.setColor(new Color(0xE5989B)); // 线 2
        g.drawLine(17, 12, 96, 25);
        g.setColor(new Color(0xFFB4A2)); // 线 3
        g.drawLine(9, 29, 96, 12);
        g.dispose();   // 画完线立即释放画笔

        // 3. 再叠 15 个随机实心圆（半径 0.2~1.0、透明度 0.3~0.8）
        // drawRandomCircles(captcha.getImage(), 15);
        // CircleCaptcha cc = new CircleCaptcha(100, 40, 4, 15);
        // cc.setBackground(new Color(0, 0, 0, 0)); // 透明底，直接叠在上面
        // cc.createCode();                      // 触发画圆
        // captcha.getImage().getGraphics().drawImage(cc.getImage(), 0, 0, null);

        return pngPrefix + captcha.getImageBase64();
    }


    /**
     * 混合 shearCaptcha lineCaptcha 图形验证码
     *
     * @param length 验证码长度
     * @return 数据流字符串
     */
    public String getPngCaptcha1(Integer length) {
        // 用 ShearCaptcha 生成底图（含扭曲字符 + 噪点背景）
        ShearCaptcha shear = CaptchaUtil.createShearCaptcha(width, height, length, 1);
        shear.setBackground(new Color(0xF8F8FF));   // 同 SVG 淡灰底
        String code = shear.getCode();              // 先触发生成
        BufferedImage img = shear.getImage();       // 拿到底图
        drawLines(img, lineCount);
        return pngPrefix + imageToBase64(img);
    }

    /**
     * 把 BufferedImage 转成 PNG 再转 Base64（**无换行**）
     */
    private static String imageToBase64(BufferedImage img) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();// Hutool 提供的字节数组输出流
        ImgUtil.write(img, "PNG", out);                // 默认格式就是 PNG
        return Base64.encode(out.toByteArray()); // Hutool 的 Base64，无换行
    }

    /**
     * 模拟 LineCaptcha 的“画干扰线”逻辑，画到指定 BufferedImage
     *
     * @param img   目标图
     * @param count 要画的线条数
     */
    private static void drawLines(BufferedImage img, int count) {
        Graphics2D g = img.createGraphics();
        applyHighQuality(g);
        int w = img.getWidth();
        int h = img.getHeight();
        for (int i = 0; i < count; i++) {
            g.setColor(new Color(
                    (int) (Math.random() * 200),
                    (int) (Math.random() * 200),
                    (int) (Math.random() * 200)));
            g.setStroke(new BasicStroke((float) lineWidth));
            int x1 = (int) (Math.random() * w);
            int y1 = (int) (Math.random() * h);
            int x2 = (int) (Math.random() * w);
            int y2 = (int) (Math.random() * h);
            g.drawLine(x1, y1, x2, y2);
        }
        g.dispose();
    }


    /**
     * 开启「高质量抗锯齿」全套开关
     *
     * @param g2
     */
    private static void applyHighQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }


    public String getPngCaptcha2(Integer length) {


        return pngPrefix;
    }

    // 新增配置参数
    private static final int SCALE_FACTOR = 3;  // 分辨率放大倍数，推荐2-4倍

    /**
     * 使用高分辨率渲染后缩放优化抗锯齿效果（新增方法）
     * 保持原有逻辑不变，只是增加了分辨率缩放步骤
     */
    public String getPngCaptcha3(Integer length) {
        // 1. 使用原有逻辑生成高分辨率验证码
        BufferedImage highResImage = generateHighResWithOriginalLogic(length);

        // 2. 高质量缩放到底分辨率
        BufferedImage finalImage = scaleDownWithQuality(highResImage);

        return pngPrefix + imageToBase64(finalImage);
    }

    /**
     * 使用原有逻辑生成高分辨率验证码（新增方法）
     */
    private BufferedImage generateHighResWithOriginalLogic(Integer length) {
        // 计算高分辨率尺寸
        int highResWidth = width * SCALE_FACTOR;
        int highResHeight = height * SCALE_FACTOR;

        // 使用原有 ShearCaptcha 逻辑，但调整尺寸
        ShearCaptcha shear = CaptchaUtil.createShearCaptcha(highResWidth, highResHeight, length, 1);
        shear.setBackground(backgroundColor);   // 同 SVG 淡灰底
        String code = shear.getCode();              // 先触发生成
        BufferedImage highResImg = shear.getImage(); // 拿到底图

        // 使用原有 drawLines 逻辑，但适配高分辨率
        drawHighResLines(highResImg, lineCount);

        return highResImg;
    }

    /**
     * 高分辨率版本的干扰线绘制（新增方法）
     * 保持原有逻辑，只是线宽按比例放大
     */
    private static void drawHighResLines(BufferedImage img, int count) {
        Graphics2D g = img.createGraphics();
        applyHighQuality(g);
        int w = img.getWidth();
        int h = img.getHeight();

        // 线宽按分辨率比例放大
        float highResLineWidth = (float) lineWidth * SCALE_FACTOR;

        for (int i = 0; i < count; i++) {
            g.setColor(new Color(
                    (int) (Math.random() * 200),
                    (int) (Math.random() * 200),
                    (int) (Math.random() * 200)));
            g.setStroke(new BasicStroke(highResLineWidth));
            int x1 = (int) (Math.random() * w);
            int y1 = (int) (Math.random() * h);
            int x2 = (int) (Math.random() * w);
            int y2 = (int) (Math.random() * h);
            g.drawLine(x1, y1, x2, y2);
        }
        g.dispose();
    }

    /**
     * 高质量图像缩放（新增方法）
     */
    private static BufferedImage scaleDownWithQuality(BufferedImage highResImage) {
        // 创建目标尺寸的图像
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = scaledImage.createGraphics();

        // 应用高质量缩放设置
        applyScalingQuality(g2);

        // 使用双线性插值进行高质量缩放
        g2.drawImage(highResImage, 0, 0, width, height, null);
        g2.dispose();

        return scaledImage;
    }

    /**
     * 应用缩放质量设置（新增方法）
     */
    private static void applyScalingQuality(Graphics2D g2) {
        // 继承原有的高质量设置
        applyHighQuality(g2);

        // 额外添加缩放专用的高质量设置
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }
}


