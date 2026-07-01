package com.typinggame.enums;

/**
 * 用户角色
 */
public enum UserRole {
    user("user","用户"),
    admin ("admin","管理员");

    private String name;
    private  String desc;

    UserRole(String user, String desc) {
        this.name = user;
        this.desc= desc;
    }

    /**
     * 根据 name 获取对应枚举
     * @param name 名称
     * @return 对应枚举，未找到返回 null
     */
    public static UserRole fromName(String name) {
        for (UserRole role : UserRole.values()) {
            if (role.name.equals(name)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 根据 desc 获取对应枚举
     * @param desc 描述
     * @return 对应枚举，未找到返回 null
     */
    public static UserRole fromDesc(String desc) {
        for (UserRole role : UserRole.values()) {
            if (role.desc.equals(desc)) {
                return role;
            }
        }
        return null;
    }

}
