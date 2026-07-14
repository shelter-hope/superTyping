package com.typinggame.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.typinggame.dto.stats.CharacterStatsUpdateDTO;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class GameRecordUploadDTO {
    private String gameMode; // 对应 gameMode
    private String selectedChars; // 对应 selectedChars
    private Integer roundNum; // 对应 roundNum
    private Integer totalScore; // 对应 totalScore
    private Integer totalChars; // 对应 totalChars
    private Integer correctChars; // 对应 correctChars
    private BigDecimal avgSpeed; // 对应 avgSpeed
    private BigDecimal peakSpeed; // 对应 peakSpeed
    private BigDecimal avgAccuracy; // 对应 avgAccuracy
    private BigDecimal peakAccuracy; // 对应 peakAccuracy
    private Integer gameDuration; // 对应 gameDuration

    private List<MonsterDataDTO> monsters; // 游戏过程中的怪物数据

    /** 本局使用的自定义词库ID，词库模式下用于回写词条练习统计；可为空 */
    private List<Integer> customDictIds;

    /** 本局使用的系统词库ID，保留用于后续系统词库统计；可为空 */
    private List<Integer> systemDictIds;

    /** 本局每个字符的输入结果，用于更新 character_stats 表；前端在结算时按击键顺序上报，可为空 */
    @JsonProperty("characterStatsUpdates")
    private List<CharacterStatsUpdateDTO> characterStatsUpdates;
}
