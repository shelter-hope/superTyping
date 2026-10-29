package com.typinggame.dto.game;

import com.typinggame.enums.MonsterType;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 字符怪物实体（含遗传进化核心属性）
 * 对应文档：字符怪物核心属性/遗传进化机制
 */
@Data
public class CharMonster {
    // 基础属性
    private String monsterId;          // 怪物唯一标识
    private List<Character> charList;  // 携带的字符串（每个字符=1点生命值）
    private BigDecimal speed;          // 行进速度（像素/帧）
    private Integer size;              // 怪物大小（像素）
    private Integer xPos;              // X坐标（顶部随机生成）
    private Integer yPos;              // Y坐标（初始为0）
    private Long survivalTime;         // 存活时间（毫秒）
    private Boolean isAlive;           // 是否存活
    private Long eliminateCostTime;    // 消灭耗时（毫秒，未消灭则为null）
    private Integer errorInputCount;    // 输入错误次数

    // 类型属性
    private MonsterType monsterType;    // 怪物类型（NORMAL/ATTACKER/EVASIVE/SMART）
    private Integer score;             // 击杀积分

    // 遗传进化标记
    private Boolean isParentQualified; // 是否为优质父本（可参与繁殖）


}