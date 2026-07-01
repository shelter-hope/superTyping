package com.typinggame.dto.stats;

import lombok.Data;
import java.util.List;

/**
 * 批量字符统计更新请求
 */
@Data
public class CharacterStatsBatchUpdateDTO {
    private List<CharacterStatsUpdateDTO> updates;
}
