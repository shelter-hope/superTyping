package com.typinggame.enums;

import lombok.Getter;

/**
 * 好友关系状态枚举
 * 0-申请中 1-已成为好友 2-已拉黑 3-已拒绝
 */
@Getter
public enum FriendRelationStatus {
    /**
     * 好友申请中（发起方已发送申请，接收方未处理）
     */
    PENDING(0, "申请中"),

    /**
     * 已成为好友（接收方同意申请）
     */
    FRIEND(1, "已成为好友"),

    /**
     * 已拉黑（单方/双方拉黑好友）
     */
    BLOCKED(2, "已拉黑"),

    /**
     * 已拒绝（接收方拒绝好友申请）
     */
    REJECTED(3, "已拒绝");

    // 状态编码（对应数据库存储的数值：0/1/2/3）
    private final Integer code;
    // 状态描述（用于前端展示/日志输出）
    private final String desc;

    // 构造方法（枚举的构造方法默认私有）
    FriendRelationStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // ========== 核心工具方法（方便业务使用） ==========
    /**
     * 根据状态编码获取枚举实例（最常用，数据库查出来的code转枚举）
     * @param code 状态编码（0/1/2/3）
     * @return 对应的枚举实例，无匹配时返回null（也可抛异常，根据业务选择）
     */
    public static FriendRelationStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FriendRelationStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        // 可选：抛自定义异常，比如 throw new IllegalArgumentException("无效的好友关系状态码：" + code);
        return null;
    }

    /**
     * 根据状态描述模糊匹配枚举（可选，用于前端传描述反查枚举）
     * @param desc 状态描述（如“申请中”）
     * @return 对应的枚举实例
     */
    public static FriendRelationStatus getByDesc(String desc) {
        if (desc == null || desc.isEmpty()) {
            return null;
        }
        for (FriendRelationStatus status : values()) {
            if (status.desc.contains(desc)) {
                return status;
            }
        }
        return null;
    }
}