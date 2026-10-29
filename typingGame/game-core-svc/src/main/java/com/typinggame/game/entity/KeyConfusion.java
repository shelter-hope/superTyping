package com.typinggame.game.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 按键混淆统计（对应 key_confusion 表）。
 *
 * <p>记录「本该输入 expectedChar，却打成了 typedChar」的累计次数。它是遗传算法定向变异的
 * 数据来源：下一局生成怪物时，优先把这条混淆关系编进词串，让玩家在真实语境里再练一次。</p>
 */
@Entity
@Table(name = "key_confusion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyConfusion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键
    private Integer userId; // 关联用户ID
    @Column(name = "expected_char", nullable = false, length = 10,
            columnDefinition = "VARCHAR(10) NOT NULL COLLATE utf8mb4_bin")
    private String expectedChar; // 本该输入的字符（区分大小写）
    @Column(name = "typed_char", nullable = false, length = 10,
            columnDefinition = "VARCHAR(10) NOT NULL COLLATE utf8mb4_bin")
    private String typedChar; // 实际打出来的字符（区分大小写）
    private Integer confusionCount; // 累计混淆次数
    private Date updateTime; // 最后更新时间
}
