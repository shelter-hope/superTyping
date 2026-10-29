package com.typinggame.game.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "monster_parent_relation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonsterParentRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long relationId;
    private Long childAttemptId;
    @Column(name = "parent_a_attempt_id")
    private Long parentAAttemptId;
    @Column(name = "parent_b_attempt_id")
    private Long parentBAttemptId;
    private Integer crossoverPoint;
    private BigDecimal mutationRate;
    private String mutationPositions;
    private String mutationChars;
    private Integer generation;
    private String algorithmVersion;
    private Date createdAt;
}
