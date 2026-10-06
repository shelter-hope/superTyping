package com.typinggame.entity.game;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "game_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;
    private Integer userId;
    private String gameMode;
    private String status;
    private Integer currentRound;
    private Date startTime;
    private Date endTime;
    @Version
    private Long version;
    private Date createdAt;
    private Date updatedAt;
}
