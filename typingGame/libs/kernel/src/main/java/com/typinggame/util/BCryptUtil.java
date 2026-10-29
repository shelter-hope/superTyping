package com.typinggame.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt 加密工具类
 * 特点：不可逆加密，自带盐值，同一明文每次加密结果不同，但校验时可匹配
 * 适用场景：用户密码加密存储、敏感信息不可逆加密
 */
public class BCryptUtil {

    /**
     * 加密强度（工作因子）：推荐10-12，值越大加密越慢、越安全
     */
    private static final int DEFAULT_STRENGTH = 10;

    /**
     * 生成加密后的字符串（自动生成盐值）
     * @param plainText 明文（如用户密码）
     * @return 加密后的字符串（包含盐值，无需单独存储盐值）
     * @throws IllegalArgumentException 明文为空时抛出异常
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            throw new IllegalArgumentException("加密明文不能为空");
        }
        // 自动生成盐值 + 加密，加密结果包含盐值
        return BCrypt.hashpw(plainText.trim(), BCrypt.gensalt(DEFAULT_STRENGTH));
    }

    /**
     * 自定义加密强度生成加密字符串
     * @param plainText 明文
     * @param strength 加密强度（4-31，推荐10-12）
     * @return 加密后的字符串
     */
    public static String encrypt(String plainText, int strength) {
        if (plainText == null || plainText.trim().isEmpty()) {
            throw new IllegalArgumentException("加密明文不能为空");
        }
        if (strength < 4 || strength > 31) {
            throw new IllegalArgumentException("加密强度必须在4-31之间");
        }
        return BCrypt.hashpw(plainText.trim(), BCrypt.gensalt(strength));
    }

    /**
     * 校验明文是否匹配加密后的字符串
     * @param plainText 明文（用户输入的密码）
     * @param encryptedText 加密后的字符串（从数据库查询的密文）
     * @return true-匹配，false-不匹配
     * @throws IllegalArgumentException 入参为空时抛出异常
     */
    public static boolean verify(String plainText, String encryptedText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            throw new IllegalArgumentException("待校验明文不能为空");
        }
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            throw new IllegalArgumentException("加密字符串不能为空");
        }
        try {
            // BCrypt自动从encryptedText中提取盐值进行校验
            return BCrypt.checkpw(plainText.trim(), encryptedText.trim());
        } catch (IllegalArgumentException e) {
            // 密文格式错误（如手动修改过），直接返回不匹配
            return false;
        }
    }

    // 测试示例
    public static void main(String[] args) {
        // 1. 加密示例
        String plainPwd = "test123";
        String encryptedPwd = BCryptUtil.encrypt(plainPwd);
        System.out.println("加密后的密码：" + encryptedPwd);

        // 2. 校验示例（正确密码）
        boolean isMatch1 = BCryptUtil.verify(plainPwd, encryptedPwd);
        System.out.println("正确密码校验结果：" + isMatch1); // 输出true

        // 3. 校验示例（错误密码）
        boolean isMatch2 = BCryptUtil.verify("wrongPwd123", encryptedPwd);
        System.out.println("错误密码校验结果：" + isMatch2); // 输出false

        // 4. 自定义加密强度（12）
        String encryptedPwd2 = BCryptUtil.encrypt(plainPwd, 12);
        System.out.println("强度12加密后的密码：" + encryptedPwd2);
        boolean isMatch3 = BCryptUtil.verify(plainPwd, encryptedPwd2);
        System.out.println("强度12校验结果：" + isMatch3); // 输出true
    }
}