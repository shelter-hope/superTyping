package com.typinggame.entity.game;

import com.typinggame.entity.user.UserInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_game_mode_configs")
@NoArgsConstructor
@AllArgsConstructor
public class UserGameModeConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Comment("主键ID")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_game_mode_user"))
    @Comment("关联用户ID")
    private UserInfo user;

    @Column(name = "mode_name", nullable = false)
    @Comment("游戏模式")
    private String modeName;

    @Column(name = "monster_init_num", nullable = false, columnDefinition = "INT DEFAULT 3")
    @Comment("初始化首轮怪物生成数量")
    private Integer monsterInitNum = 3;

    @Column(name = "monster_type_pro", nullable = false, length = 255, columnDefinition = "VARCHAR(255) NOT NULL")
    @Comment("各种怪物的类型的占比，以json存储")
    private String monsterTypePro;

    @Column(name = "create_time", insertable = true, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Comment("创建时间")
    @CreationTimestamp
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = true, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @Comment("更新时间")
    @CreationTimestamp
    private LocalDateTime updateTime;
}