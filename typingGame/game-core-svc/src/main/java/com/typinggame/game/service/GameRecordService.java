package com.typinggame.game.service;

import com.typinggame.dto.game.GameRecordUploadDTO;
import com.typinggame.dto.game.GameRecordUploadResultDTO;
import com.typinggame.entity.game.GameRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

public interface GameRecordService {
    GameRecordUploadResultDTO saveRecord(Integer userId, GameRecordUploadDTO recordDTO);

    List<GameRecord> getRecords(Integer userId, String mode, Integer limit, Date startDate, Date endDate);

    Page<GameRecord> getAllRecords(Pageable pageable, String searchTerm, String gameMode);

    boolean updateRecord(Integer recordId, GameRecord updates);

    void deleteRecord(Integer recordId);

    GameRecord getRecordDetail(Long id);

    GameRecord getBestRecord(Integer userId, String mode);
}