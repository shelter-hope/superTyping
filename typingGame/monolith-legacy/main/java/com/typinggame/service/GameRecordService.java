package com.typinggame.service;

import com.typinggame.dto.game.GameRecordUploadDTO;
import com.typinggame.dto.game.GameRecordUploadResultDTO;
import com.typinggame.entity.game.GameRecord;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 处理游戏记录相关业务逻辑
 */
public interface GameRecordService {
    /**
     * 保存游戏记录
     * @param userId 用户ID
     * @param recordDTO 游戏记录数据
     * @return 结算摘要（含 recordId、本局结算后难度 Dₜ 等）
     */
    GameRecordUploadResultDTO saveRecord(Integer userId, GameRecordUploadDTO recordDTO);

    /**
     * 获取历史记录列表
     * @param userId 用户ID
     * @param mode 游戏模式
     * @param limit 限制数量
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 历史记录列表
     */
    List<GameRecord> getRecords(Integer userId, String mode, Integer limit, Date startDate, Date endDate);

    /**
     * 获取所有游戏记录（分页、搜索、过滤）
     * @param pageable 分页信息
     * @param searchTerm 搜索关键词（用户ID）
     * @param gameMode 游戏模式过滤
     * @return 游戏记录分页
     */
    Page<GameRecord> getAllRecords(Pageable pageable, String searchTerm, String gameMode);

    /**
     * 更新游戏记录
     * @param recordId 记录ID
     * @param updates 更新内容
     * @return 是否成功
     */
    boolean updateRecord(Integer recordId, GameRecord updates);

    /**
     * 删除游戏记录
     * @param recordId 记录ID
     */
    void deleteRecord(Integer recordId);

    /**
     * 获取单局详情
     * @param id 记录ID
     * @return 记录详情
     */
    GameRecord getRecordDetail(Long id);

    /**
     * 获取个人最佳成绩
     * @param userId 用户ID
     * @param mode 游戏模式
     * @return 最佳成绩
     */
    GameRecord getBestRecord(Integer userId, String mode);
}
