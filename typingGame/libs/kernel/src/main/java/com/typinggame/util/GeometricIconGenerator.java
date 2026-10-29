package com.typinggame.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

/**
 * 随机生成用户头像的工具类
 */
public class GeometricIconGenerator {

    private static final int SIZE = 300;
    private static final int GRID_SIZE = 5;
    private static final int CELL_SIZE = SIZE / GRID_SIZE;

    /**
     * 根据 seed 生成 300x300 的 Identicon 风格头像
     *
     * @param seed 任意字符串（如用户ID）
     * @return BufferedImage 图像对象
     */
    public static BufferedImage generate(String seed) {
        if (seed == null || seed.isEmpty()) {
            seed = "default";
        }

        byte[] hash = md5(seed);
        Color bgColor = new Color(240, 240, 240); // 浅灰背景
        Color fgColor = pickColor(hash);

        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 填充背景
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, SIZE, SIZE);

        // 构建 5x5 网格（只取前15字节：5行 x 3列，右侧镜像）
        boolean[][] grid = new boolean[GRID_SIZE][GRID_SIZE];
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < 3; col++) { // 只处理左半+中间
                int index = row * 3 + col;
                if (index < hash.length) {
                    boolean filled = (hash[index] & 1) == 1;
                    grid[row][col] = filled;
                    grid[row][GRID_SIZE - 1 - col] = filled; // 镜像
                }
            }
        }

        // 绘制方块
        g2d.setColor(fgColor);
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (grid[row][col]) {
                    g2d.fillRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        g2d.dispose();
        return image;
    }

    // 从哈希值生成柔和的前景色
    private static Color pickColor(byte[] hash) {
        if (hash.length < 3) return Color.GRAY;
        // 使用前3字节生成 RGB，限制在柔和范围（80~200）
        int r = 80 + Math.abs(hash[0]) % 120;
        int g = 80 + Math.abs(hash[1]) % 120;
        int b = 80 + Math.abs(hash[2]) % 120;
        return new Color(r, g, b);
    }

    // 计算 MD5 哈希
    private static byte[] md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(input.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    // 示例：保存为 PNG 文件
    public static void main(String[] args) throws Exception {
        BufferedImage icon = generate("admin123");
        javax.imageio.ImageIO.write(icon, "png", new java.io.File("identicon_user123.png"));
        System.out.println("✅ Identicon saved as identicon_user123.png");
    }
}