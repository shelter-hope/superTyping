package com.typinggame.game.service;

import com.typinggame.dto.game.GameRecordSummaryDTO;
import com.typinggame.dto.stats.WeakCharDTO;

import java.util.Date;
import java.util.List;

/**
 * 对外提供的玩家数据只读能力（内部接口用）。
 */
public interface InternalPlayerDataService {

    /**
     * 最近对局摘要。
     */
    List<GameRecordSummaryDTO> recentRecords(Integer userId, int limit);

    /**
     * 指定时间区间内的对局摘要（左闭右闭）。
     */
    List<GameRecordSummaryDTO> recordsBetween(Integer userId, Date startDate, Date endDate);

    /**
     * 按平均准确率升序返回弱项字符。
     */
    List<WeakCharDTO> weakChars(Integer userId, int limit);
}
