package com.typinggame.dto.game;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GameRoundCompleteDTO {
    @NotNull
    private Long sessionId;
    @NotNull
    private Integer roundNum;
    @NotNull
    private Long planVersion;
    @Size(max = 500)
    private List<MonsterDataDTO> monsters;
}
