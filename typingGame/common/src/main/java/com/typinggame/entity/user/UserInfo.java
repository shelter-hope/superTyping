package com.typinggame.entity.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.util.Date;

/**
 * 用户信息实体 （对应user_info表）
 */
@Entity // 必须加@Entity：标记这是JPA实体类（核心注解
@Table(name="user_info") // 实体类Profile 映射 数据库表user_info
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自增主键（对应 MySQL 的 AUTO_INCREMENT）
    private Integer userId; // 用户唯一ID（自增）
    @Column(unique = true, nullable = false, length = 50)
    private String username; // 登录账号（唯一）
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password; // 加密密码
    private String nickname; // 昵称
    private String signature; // 个性签名
    private String avatar; // 用户头像URL
    private Integer bestScore; // 历史最佳积分
    private Integer bestRound; // 历史最佳轮次
    private BigDecimal bestSpeed; // 历史最佳速度（字符/分钟）
    private BigDecimal bestAccuracy; // 历史最佳准确率（%）
    private BigDecimal avgSpeed; // 平均打字速度（字符/分钟）
    private BigDecimal avgAccuracy; // 平均准确率（%）
    private BigDecimal peakAccuracy; // 峰值准确率（%）
    private Integer gameNum; // 游戏总场数
    private String role; // 角色（user/admin）
    private Boolean enabled = true; // 禁用后拒绝登录和即时连接
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间,同样作为上次登录时间
}
