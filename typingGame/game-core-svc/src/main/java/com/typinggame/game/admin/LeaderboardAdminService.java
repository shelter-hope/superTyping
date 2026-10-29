package com.typinggame.game.admin;

import com.typinggame.common.admin.AdminFilter;
import com.typinggame.common.admin.AdminPageSupport;
import com.typinggame.game.entity.LeaderboardEntry;
import com.typinggame.game.repository.GameLeaderboardEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@Service
public class LeaderboardAdminService {
    private final GameLeaderboardEntryRepository leaderboardEntryRepository;
    private final RecordAdminService recordAdminService;

    public LeaderboardAdminService(GameLeaderboardEntryRepository leaderboardEntryRepository,
                                   RecordAdminService recordAdminService) {
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.recordAdminService = recordAdminService;
    }

    public Map<String, Object> getLeaderboardEntries(int page, int pageSize, Integer userId, String gameMode) {
        AdminFilter<LeaderboardEntry> filter = AdminFilter.of(LeaderboardEntry.class).eq("userId", userId).eq("gameMode", gameMode);
        int size = Math.min(200, Math.max(1, pageSize));
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "totalScore"));
        return AdminPageSupport.pageMap("items", leaderboardEntryRepository.findAll(filter, pageRequest));
    }

    public Map<String, Object> getOverview() {
        Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        return AdminPageSupport.map(
                "recordCount", recordAdminService.countRecords(),
                "recordsToday", recordAdminService.countRecordsSince(todayStart),
                "playersToday", recordAdminService.countDistinctUsersSince(todayStart));
    }
}
