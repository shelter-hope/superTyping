package com.typinggame.controller;

import com.typinggame.dto.model.UserModelAdaptLogDTO;
import com.typinggame.entity.config.SystemGameModeConfig;
import com.typinggame.entity.config.SystemMonsterCommoConfig;
import com.typinggame.entity.config.SystemMonsterTypeConfig;
import com.typinggame.entity.config.SystemParam;
import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.DictionaryWord;
import com.typinggame.entity.dictionaries.SystemDictionary;
import com.typinggame.entity.game.GameRecord;
import com.typinggame.entity.game.SystemMonsterConfig;
import com.typinggame.entity.leaderboard.LeaderboardEntry;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.ModelInitParams;
import com.typinggame.entity.model.UserModelAdaptLog;
import com.typinggame.entity.model.UserModelParams;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.entity.user.CharacterStats;
import com.typinggame.entity.message.FriendRelation;
import com.typinggame.entity.message.FriendChatMessage;
import com.typinggame.entity.message.SystemNotification;
import com.typinggame.entity.message.UserConversation;
import com.typinggame.entity.config.UserAppConfig;
import com.typinggame.entity.game.UserGameModeConfig;
import com.typinggame.entity.game.UserMonsterConfig;
import com.typinggame.entity.game.MonsterData;
import com.typinggame.entity.game.StringParent;
import com.typinggame.entity.user.UserDailyStats;
import com.typinggame.entity.user.UserInfoSnapshot;
import com.typinggame.entity.message.AiConversation;
import com.typinggame.entity.message.AiChatMessage;
import com.typinggame.repository.CustomDictionaryRepository;
import com.typinggame.repository.CharacterStatsRepository;
import com.typinggame.repository.DictionaryWordRepository;
import com.typinggame.repository.GameModeConfigRepository;
import com.typinggame.repository.LeaderboardEntryRepository;
import com.typinggame.repository.ModelConstantRepository;
import com.typinggame.repository.ModelInitParamsRepository;
import com.typinggame.repository.SystemDictionaryRepository;
import com.typinggame.repository.SystemDictionaryWordRepository;
import com.typinggame.repository.SystemMonsterCommoConfigRepository;
import com.typinggame.repository.SystemMonsterTypeConfigRepository;
import com.typinggame.repository.SystemParamRepository;
import com.typinggame.repository.UserModelAdaptLogRepository;
import com.typinggame.repository.UserInfoRepository;
import com.typinggame.repository.UserDailyStatsRepository;
import com.typinggame.repository.FriendRelationRepository;
import com.typinggame.repository.FriendChatMessageRepository;
import com.typinggame.repository.SystemNotificationRepository;
import com.typinggame.repository.UserConversationRepository;
import com.typinggame.repository.UserAppConfigRepository;
import com.typinggame.repository.UserGameModeConfigRepository;
import com.typinggame.repository.UserMonsterConfigRepository;
import com.typinggame.repository.MonsterDataRepository;
import com.typinggame.repository.StringParentRepository;
import com.typinggame.repository.UserInfoSnapshotRepository;
import com.typinggame.repository.AiConversationRepository;
import com.typinggame.repository.AiChatMessageRepository;
import com.typinggame.repository.UserModelParamsRepository;
import com.typinggame.service.GameRecordService;
import com.typinggame.service.MonsterConfigService;
import com.typinggame.service.UserService;
import com.typinggame.vo.ResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理后台 API
 */
@RestController
@Tag(name = "管理后台 API")
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')") // 与 UserDetails 中 ROLE_ADMIN 一致
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private GameRecordService gameRecordService;

    @Autowired
    private MonsterConfigService monsterConfigService;
    @Autowired
    private ModelConstantRepository modelConstantRepository;
    @Autowired
    private ModelInitParamsRepository modelInitParamsRepository;
    @Autowired
    private UserModelAdaptLogRepository userModelAdaptLogRepository;
    @Autowired
    private GameModeConfigRepository gameModeConfigRepository;

    @Autowired
    private SystemParamRepository systemParamRepository;

    @Autowired
    private SystemDictionaryRepository systemDictionaryRepository;

    @Autowired
    private CustomDictionaryRepository customDictionaryRepository;

    @Autowired
    private LeaderboardEntryRepository leaderboardEntryRepository;

    @Autowired
    private SystemMonsterTypeConfigRepository systemMonsterTypeConfigRepository;

    @Autowired
    private SystemMonsterCommoConfigRepository systemMonsterCommoConfigRepository;

    @Autowired
    private CharacterStatsRepository characterStatsRepository;

    @Autowired
    private DictionaryWordRepository dictionaryWordRepository;

    @Autowired
    private SystemDictionaryWordRepository systemDictionaryWordRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private UserDailyStatsRepository userDailyStatsRepository;

    @Autowired
    private FriendRelationRepository friendRelationRepository;

    @Autowired
    private FriendChatMessageRepository friendChatMessageRepository;

    @Autowired
    private SystemNotificationRepository systemNotificationRepository;

    @Autowired
    private UserConversationRepository userConversationRepository;

    @Autowired
    private UserAppConfigRepository userAppConfigRepository;

    @Autowired
    private UserGameModeConfigRepository userGameModeConfigRepository;

    @Autowired
    private UserMonsterConfigRepository userMonsterConfigRepository;

    @Autowired
    private MonsterDataRepository monsterDataRepository;

    @Autowired
    private StringParentRepository stringParentRepository;

    @Autowired
    private UserInfoSnapshotRepository userInfoSnapshotRepository;

    @Autowired
    private AiConversationRepository aiConversationRepository;

    @Autowired
    private AiChatMessageRepository aiChatMessageRepository;

    @Autowired
    private UserModelParamsRepository userModelParamsRepository;

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    @Operation(summary = "获取用户列表")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "searchTerm", required = false) String searchTerm) {
        
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<UserInfo> userPage = userService.getUsers(pageable, searchTerm);

        Map<String, Object> result = new HashMap<>();
        result.put("users", userPage.getContent());
        result.put("total", userPage.getTotalElements());

        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "更新用户角色")
    public ResponseEntity<ResultMessage<Void>> updateUserRole(
            @PathVariable("userId") Integer userId,
            @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (userService.updateUserRole(userId, role)) {
            return ResponseEntity.ok(ResultMessage.success());
        }
        return ResponseEntity.badRequest().body(ResultMessage.error("更新失败，用户不存在"));
    }

    @DeleteMapping("/users/{userId}/game-data")
    @Operation(summary = "删除用户游戏数据")
    public ResponseEntity<ResultMessage<Void>> deleteUserGameData(@PathVariable("userId") Integer userId) {
        userService.deleteUserGameData(userId);
        return ResponseEntity.ok(ResultMessage.success());
    }

    // ==================== 游戏记录管理 ====================

    @GetMapping("/game-records")
    @Operation(summary = "获取游戏记录列表")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getGameRecords(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "gameMode", required = false) String gameMode) {

        // 前端 api.ts 中 searchTerm 其实是 userId，但作为 String 传入
        // getAllRecords 内部会处理转换
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<GameRecord> recordPage = gameRecordService.getAllRecords(pageable, userId, gameMode);

        Map<String, Object> result = new HashMap<>();
        result.put("records", recordPage.getContent());
        result.put("total", recordPage.getTotalElements());

        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @PatchMapping("/game-records/{recordId}")
    @Operation(summary = "更新游戏记录")
    public ResponseEntity<ResultMessage<Void>> updateGameRecord(
            @PathVariable("recordId") Integer recordId,
            @RequestBody GameRecord updates) {
        if (gameRecordService.updateRecord(recordId, updates)) {
            return ResponseEntity.ok(ResultMessage.success());
        }
        return ResponseEntity.badRequest().body(ResultMessage.error("更新失败，记录不存在"));
    }

    @DeleteMapping("/game-records/{recordId}")
    @Operation(summary = "删除游戏记录")
    public ResponseEntity<ResultMessage<Void>> deleteGameRecord(@PathVariable("recordId") Integer recordId) {
        gameRecordService.deleteRecord(recordId);
        return ResponseEntity.ok(ResultMessage.success());
    }

    // ==================== 怪物配置管理 ====================

    @GetMapping("/monster-configs")
    @Operation(summary = "获取怪物配置")
    public ResponseEntity<ResultMessage<List<SystemMonsterConfig>>> getMonsterConfigs() {
        return ResponseEntity.ok(ResultMessage.success(monsterConfigService.getAllConfigs()));
    }

    @PatchMapping("/monster-configs/{type}")
    @Operation(summary = "更新怪物配置")
    public ResponseEntity<ResultMessage<Void>> updateMonsterConfig(
            @PathVariable("type") String type,
            @RequestBody SystemMonsterConfig updates) { // 前端只传 score, 但 Spring 可以部分映射
        // 前端传的是 { score: 123 }
        // SystemMonsterConfig 字段匹配即可
        // 实际上前端 api.ts: updateMonsterConfig(type, newScore) -> updates is number?
        // Wait, api.ts says: updateMonsterConfig(type: string, updates: Partial<MonsterConfig>)
        // Frontend implementation: return updateMonsterConfig(config.type, newScore); 
        // Wait, check frontend implementation again.
        
        // MonsterConfigManagement.tsx:
        // updateMonsterConfig(config.type, newScore);
        
        // api.ts:
        // export async function updateMonsterConfig(type: string, updates: Partial<MonsterConfig>): Promise<void>
        // But in MonsterConfigManagement.tsx it passes `newScore` which is a number!
        // This is a TypeScript error in frontend potentially or mismatched implementation.
        // Let's check MonsterConfigManagement.tsx lines 54-58
        // const newScore = editedScores[config.type];
        // return updateMonsterConfig(config.type, newScore);
        
        // api.ts definition:
        // export async function updateMonsterConfig(type: string, updates: Partial<MonsterConfig>): Promise<void> {
        //    await apiClient.patch(`/admin/monster-configs/${type}`, updates);
        // }
        
        // If frontend passes a number as second argument, axios will send it as body.
        // So body will be `123`. This is NOT valid JSON object for `SystemMonsterConfig`.
        // I need to fix the frontend call to pass an object: `{ score: newScore }`.
        
        // For now, in backend, I can handle both or assume frontend is fixed.
        // I will fix frontend as well.
        
        if (monsterConfigService.updateConfig(type, updates) != null) {
            return ResponseEntity.ok(ResultMessage.success());
        }
        return ResponseEntity.badRequest().body(ResultMessage.error("更新失败"));
    }

    // ==================== 系统参数管理 ====================

    @GetMapping("/system-params")
    @Operation(summary = "获取系统参数列表")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getSystemParams(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "paramName", required = false) String paramName) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, "paramName"));
        Page<SystemParam> paramPage = systemParamRepository.findAll(pageable);

        List<SystemParam> content = paramPage.getContent().stream()
                .filter(p -> paramName == null || (p.getParamName() != null && p.getParamName().contains(paramName.trim())))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", paramPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    // ==================== 字符统计总览（后台） ====================

    @GetMapping("/character-stats")
    @Operation(summary = "获取字符统计列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getCharacterStatsAdmin(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "charType", required = false) String charType,
            @RequestParam(value = "charValue", required = false) String charValue) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "errorCount"));
        Page<CharacterStats> statsPage = characterStatsRepository.findAll(pageable);

        List<CharacterStats> content = statsPage.getContent().stream()
                .filter(s -> userId == null || (s.getUserId() != null && s.getUserId().equals(userId)))
                .filter(s -> charType == null || (s.getCharType() != null && s.getCharType().equalsIgnoreCase(charType)))
                .filter(s -> charValue == null || (s.getCharValue() != null && s.getCharValue().equals(charValue)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", statsPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    // ==================== 词库管理（后台视角） ====================

    @GetMapping("/system-dictionaries")
    @Operation(summary = "获取系统词库列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getSystemDictionariesAdmin(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<SystemDictionary> dictPage = systemDictionaryRepository.findAll(pageable);

        List<SystemDictionary> content = dictPage.getContent().stream()
                .filter(d -> name == null || (d.getName() != null && d.getName().contains(name)))
                .filter(d -> category == null || (d.getCategory() != null && d.getCategory().equals(category)))
                .filter(d -> isActive == null || (d.getIsActive() != null && d.getIsActive().equals(isActive)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", dictPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/system-dictionary-words")
    @Operation(summary = "获取系统词库单词列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getSystemDictionaryWordsAdmin(
            @RequestParam("dictId") Integer dictId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "50") int pageSize) {

        List<com.typinggame.entity.dictionaries.SystemDictionaryWord> all = systemDictionaryWordRepository.findByDictId(dictId);
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<com.typinggame.entity.dictionaries.SystemDictionaryWord> pageContent =
                from >= all.size() ? Collections.emptyList() : all.subList(from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("items", pageContent);
        result.put("total", all.size());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/custom-dictionary-words")
    @Operation(summary = "获取自定义词库单词列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getCustomDictionaryWordsAdmin(
            @RequestParam("dictId") Integer dictId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "50") int pageSize) {

        List<DictionaryWord> all = dictionaryWordRepository.findByDictId(dictId);
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<DictionaryWord> pageContent =
                from >= all.size() ? Collections.emptyList() : all.subList(from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("items", pageContent);
        result.put("total", all.size());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/custom-dictionaries")
    @Operation(summary = "获取自定义词库列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getCustomDictionariesAdmin(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<CustomDictionary> dictPage = customDictionaryRepository.findAll(pageable);

        List<CustomDictionary> content = dictPage.getContent().stream()
                .filter(d -> userId == null || (d.getUserId() != null && d.getUserId().equals(userId)))
                .filter(d -> name == null || (d.getName() != null && d.getName().contains(name)))
                .filter(d -> isPublic == null || (d.getIsPublic() != null && d.getIsPublic().equals(isPublic)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", dictPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    // ==================== 排行榜条目管理 ====================

    @GetMapping("/leaderboard-entries")
    @Operation(summary = "获取排行榜条目列表")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getLeaderboardEntries(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "gameMode", required = false) String gameMode) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "totalScore"));
        Page<LeaderboardEntry> entryPage = leaderboardEntryRepository.findAll(pageable);

        List<LeaderboardEntry> content = entryPage.getContent().stream()
                .filter(e -> userId == null || (e.getUserId() != null && e.getUserId().equals(userId)))
                .filter(e -> gameMode == null || (e.getGameMode() != null && e.getGameMode().equals(gameMode)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", entryPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    // ==================== 好友关系与会话管理 ====================

    @GetMapping("/friend-relations")
    @Operation(summary = "获取好友关系列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getFriendRelations(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "friendUserId", required = false) Integer friendUserId,
            @RequestParam(value = "relationStatus", required = false) Integer relationStatus) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<FriendRelation> relationPage = friendRelationRepository.findAll(pageable);

        List<FriendRelation> content = relationPage.getContent().stream()
                .filter(r -> userId == null || (r.getUserId() != null && r.getUserId().equals(userId)))
                .filter(r -> friendUserId == null || (r.getFriendUserId() != null && r.getFriendUserId().equals(friendUserId)))
                .filter(r -> relationStatus == null || (r.getRelationStatus() != null && r.getRelationStatus().equals(relationStatus)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", relationPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/user-conversations")
    @Operation(summary = "获取用户会话列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getUserConversations(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "lastMessageTime"));
        Page<UserConversation> convPage = userConversationRepository.findAll(pageable);

        List<UserConversation> content = convPage.getContent().stream()
                .filter(c -> userId == null || (c.getUserId() != null && c.getUserId().equals(userId)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", convPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/friend-chat-messages")
    @Operation(summary = "获取好友聊天消息列表（后台，按会话）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getFriendChatMessages(
            @RequestParam("conversationId") Integer conversationId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "50") int pageSize) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "sendTime"));
        List<FriendChatMessage> list = friendChatMessageRepository.findByConversationIdOrderBySendTimeDesc(conversationId, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("items", list);
        result.put("total", list.size());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    // ==================== 系统通知管理 ====================

    @GetMapping("/system-notifications")
    @Operation(summary = "获取系统通知列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getSystemNotifications(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "notificationType", required = false) Integer notificationType,
            @RequestParam(value = "isRead", required = false) Boolean isRead) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "sendTime"));
        Page<SystemNotification> notifyPage = systemNotificationRepository.findAll(pageable);

        List<SystemNotification> content = notifyPage.getContent().stream()
                .filter(n -> userId == null || (n.getUserId() != null && n.getUserId().equals(userId)))
                .filter(n -> notificationType == null || (n.getNotificationType() != null && n.getNotificationType().equals(notificationType)))
                .filter(n -> isRead == null || (n.getIsRead() != null && n.getIsRead().equals(isRead)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", notifyPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    // ==================== 用户个性化配置管理 ====================

    @GetMapping("/user-app-configs")
    @Operation(summary = "获取用户应用配置列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getUserAppConfigs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<UserAppConfig> cfgPage = userAppConfigRepository.findAll(pageable);

        List<UserAppConfig> content = cfgPage.getContent().stream()
                .filter(c -> userId == null || (c.getUserId() != null && c.getUserId().equals(userId)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", cfgPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/user-game-mode-configs")
    @Operation(summary = "获取用户游戏模式配置列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getUserGameModeConfigs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "modeName", required = false) String modeName) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<UserGameModeConfig> cfgPage = userGameModeConfigRepository.findAll(pageable);

        List<UserGameModeConfig> content = cfgPage.getContent().stream()
                .filter(c -> userId == null || (c.getUser() != null && c.getUser().getUserId().equals(userId)))
                .filter(c -> modeName == null || (c.getModeName() != null && c.getModeName().equals(modeName)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", cfgPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/user-monster-configs")
    @Operation(summary = "获取用户怪物配置列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getUserMonsterConfigs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "type", required = false) String type) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<UserMonsterConfig> cfgPage = userMonsterConfigRepository.findAll(pageable);

        List<UserMonsterConfig> content = cfgPage.getContent().stream()
                .filter(c -> userId == null || (c.getUserId() != null && c.getUserId().equals(userId)))
                .filter(c -> type == null || (c.getType() != null && c.getType().equals(type)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", cfgPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/user-model-params")
    @Operation(summary = "获取用户个性化模型参数列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getUserModelParams(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "gameMode", required = false) String gameMode) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updateTime"));
        Page<UserModelParams> paramsPage = userModelParamsRepository.findAll(pageable);

        List<UserModelParams> content = paramsPage.getContent().stream()
                .filter(p -> userId == null || (p.getUserId() != null && p.getUserId().equals(userId)))
                .filter(p -> gameMode == null || (p.getGameMode() != null && p.getGameMode().equals(gameMode)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", paramsPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    // ==================== 怪物明细与父本（后台只读排查） ====================

    @GetMapping("/monster-data")
    @Operation(summary = "获取怪物明细列表（按游戏记录）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getMonsterData(
            @RequestParam("recordId") Integer recordId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {

        List<MonsterData> all = monsterDataRepository.findByRecordId(recordId);
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<MonsterData> pageContent = from >= all.size() ? Collections.emptyList() : all.subList(from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("items", pageContent);
        result.put("total", all.size());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/string-parents")
    @Operation(summary = "获取字符串父本记录列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getStringParents(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updateTime"));
        Page<StringParent> parentPage = stringParentRepository.findAll(pageable);

        List<StringParent> content = parentPage.getContent().stream()
                .filter(p -> userId == null || (p.getUserId() != null && p.getUserId().equals(userId)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", parentPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    // ==================== 用户每日统计 & 历史快照（后台） ====================

    @GetMapping("/user-daily-stats")
    @Operation(summary = "获取用户每日统计列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getUserDailyStats(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "statDate"));
        Page<UserDailyStats> statsPage = userDailyStatsRepository.findAll(pageable);

        List<UserDailyStats> content = statsPage.getContent().stream()
                .filter(s -> userId == null || (s.getUserId() != null && s.getUserId().equals(userId)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", statsPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/user-info-snapshots")
    @Operation(summary = "获取用户历史快照列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getUserInfoSnapshots(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "snapshotTime"));
        Page<UserInfoSnapshot> snapPage = userInfoSnapshotRepository.findAll(pageable);

        List<UserInfoSnapshot> content = snapPage.getContent().stream()
                .filter(s -> userId == null || (s.getUserId() != null && s.getUserId().equals(userId)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", snapPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    // ==================== AI 会话与消息（后台） ====================

    @GetMapping("/ai-conversations")
    @Operation(summary = "获取 AI 会话列表（后台）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getAiConversations(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "aiRobotId", required = false) Integer aiRobotId) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updateTime"));
        Page<AiConversation> convPage = aiConversationRepository.findAll(pageable);

        List<AiConversation> content = convPage.getContent().stream()
                .filter(c -> userId == null || (c.getUser() != null && c.getUser().getUserId().equals(userId)))
                .filter(c -> aiRobotId == null || (c.getAiRobotId() != null && c.getAiRobotId().equals(aiRobotId)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", content);
        result.put("total", convPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/ai-chat-messages")
    @Operation(summary = "获取 AI 聊天消息列表（后台，按会话）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getAiChatMessages(
            @RequestParam("conversationId") Integer conversationId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "50") int pageSize) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "sendTime"));
        List<AiChatMessage> list = aiChatMessageRepository.findByConversation_IdOrderBySendTimeDesc(conversationId, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("items", list);
        result.put("total", list.size());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    // ==================== 怪物公共与类型配置管理 ====================

    @GetMapping("/system-monster-type-configs")
    @Operation(summary = "获取系统怪物类型基础配置列表")
    public ResponseEntity<ResultMessage<List<SystemMonsterTypeConfig>>> getSystemMonsterTypeConfigs() {
        List<SystemMonsterTypeConfig> list = systemMonsterTypeConfigRepository.findAll(Sort.by(Sort.Direction.ASC, "type"));
        return ResponseEntity.ok(ResultMessage.success(list));
    }

    @GetMapping("/system-monster-common-configs")
    @Operation(summary = "获取系统怪物公共基础配置列表")
    public ResponseEntity<ResultMessage<List<SystemMonsterCommoConfig>>> getSystemMonsterCommonConfigs() {
        List<SystemMonsterCommoConfig> list = systemMonsterCommoConfigRepository.findAll();
        return ResponseEntity.ok(ResultMessage.success(list));
    }

    // ==================== 数学模型调参与 Dₜ 可视化 ====================

    @GetMapping("/model-constants")
    @Operation(summary = "获取模型常量列表")
    public ResponseEntity<ResultMessage<List<ModelConstant>>> getModelConstants() {
        return ResponseEntity.ok(ResultMessage.success(modelConstantRepository.findAll()));
    }

    @PatchMapping("/model-constants/{id}")
    @Operation(summary = "更新模型常量")
    public ResponseEntity<ResultMessage<ModelConstant>> updateModelConstant(
            @PathVariable("id") Integer id,
            @RequestBody ModelConstant updates) {
        return modelConstantRepository.findById(id)
                .map(c -> {
                    if (updates.getMaxSpeed() != null) c.setMaxSpeed(updates.getMaxSpeed());
                    if (updates.getRefPeakSpeed() != null) c.setRefPeakSpeed(updates.getRefPeakSpeed());
                    if (updates.getInitialDifficulty() != null) c.setInitialDifficulty(updates.getInitialDifficulty());
                    c.setUpdateTime(new java.util.Date());
                    return ResponseEntity.ok(ResultMessage.success(modelConstantRepository.save(c)));
                })
                .orElse(ResponseEntity.badRequest().body(ResultMessage.error("常量不存在")));
    }

    @GetMapping("/model-init-params")
    @Operation(summary = "获取模型初始化参数列表（按模式）")
    public ResponseEntity<ResultMessage<List<ModelInitParams>>> getModelInitParamsList() {
        return ResponseEntity.ok(ResultMessage.success(modelInitParamsRepository.findAll()));
    }

    @GetMapping("/model-init-params/mode/{gameMode}")
    @Operation(summary = "按游戏模式获取模型初始化参数")
    public ResponseEntity<ResultMessage<ModelInitParams>> getModelInitParamsByMode(@PathVariable("gameMode") String gameMode) {
        return modelInitParamsRepository.findFirstByGameMode(gameMode)
                .map(p -> ResponseEntity.ok(ResultMessage.success(p)))
                .orElse(ResponseEntity.ok(ResultMessage.success(null)));
    }

    @PatchMapping("/model-init-params/{paramId}")
    @Operation(summary = "更新模型初始化参数")
    public ResponseEntity<ResultMessage<ModelInitParams>> updateModelInitParams(
            @PathVariable("paramId") Integer paramId,
            @RequestBody ModelInitParams updates) {
        return modelInitParamsRepository.findById(paramId)
                .map(p -> {
                    if (updates.getInitDifficulty() != null) p.setInitDifficulty(updates.getInitDifficulty());
                    if (updates.getWeightRealAccuracy() != null) p.setWeightRealAccuracy(updates.getWeightRealAccuracy());
                    if (updates.getWeightRealSpeed() != null) p.setWeightRealSpeed(updates.getWeightRealSpeed());
                    if (updates.getWeightCharErrorRate() != null) p.setWeightCharErrorRate(updates.getWeightCharErrorRate());
                    if (updates.getWeightHistoryAccuracy() != null) p.setWeightHistoryAccuracy(updates.getWeightHistoryAccuracy());
                    if (updates.getWeightHistoryPeakSpeed() != null) p.setWeightHistoryPeakSpeed(updates.getWeightHistoryPeakSpeed());
                    if (updates.getWeightErrorCharRatio() != null) p.setWeightErrorCharRatio(updates.getWeightErrorCharRatio());
                    if (updates.getWeightCurrentRound() != null) p.setWeightCurrentRound(updates.getWeightCurrentRound());
                    if (updates.getSmoothAdjustCoeff() != null) p.setSmoothAdjustCoeff(updates.getSmoothAdjustCoeff());
                    if (updates.getMaxAdjustRatio() != null) p.setMaxAdjustRatio(updates.getMaxAdjustRatio());
                    return ResponseEntity.ok(ResultMessage.success(modelInitParamsRepository.save(p)));
                })
                .orElse(ResponseEntity.badRequest().body(ResultMessage.error("参数不存在")));
    }

    @GetMapping("/user-model-adapt-logs")
    @Operation(summary = "查询用户模型适配日志（支持按用户、模式、时间范围筛选）")
    public ResponseEntity<ResultMessage<Map<String, Object>>> getUserModelAdaptLogs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "gameMode", required = false) String gameMode,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "logTime"));
        Page<UserModelAdaptLog> logPage;

        if (userId == null) {
            logPage = userModelAdaptLogRepository.findAll(pageable);
        } else if (gameMode != null && !gameMode.trim().isEmpty()) {
            logPage = userModelAdaptLogRepository.findByUser_UserIdAndGameRecord_GameModeOrderByLogTimeDesc(userId, gameMode.trim(), pageable);
        } else if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            logPage = userModelAdaptLogRepository.findByUser_UserIdAndLogTimeBetweenOrderByLogTimeDesc(userId, start, end, pageable);
        } else {
            logPage = userModelAdaptLogRepository.findByUser_UserIdOrderByLogTimeDesc(userId, pageable);
        }

        List<UserModelAdaptLogDTO> dtos = logPage.getContent().stream()
                .map(log -> UserModelAdaptLogDTO.builder()
                        .logId(log.getLogId())
                        .userId(log.getUser() != null ? log.getUser().getUserId() : null)
                        .gameRecordId(log.getGameRecord() != null ? log.getGameRecord().getRecordId() : null)
                        .gameMode(log.getGameRecord() != null ? log.getGameRecord().getGameMode() : null)
                        .roundNum(log.getRoundNum())
                        .beforeDifficulty(log.getBeforeDifficulty())
                        .afterDifficulty(log.getAfterDifficulty())
                        .adjustParams(log.getAdjustParams())
                        .triggerRule(log.getTriggerRule())
                        .logTime(log.getLogTime())
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("logs", dtos);
        result.put("total", logPage.getTotalElements());
        return ResponseEntity.ok(ResultMessage.success(result));
    }

    @GetMapping("/user-model-adapt-logs/trend")
    @Operation(summary = "Dₜ 趋势聚合（按日）")
    public ResponseEntity<ResultMessage<List<Map<String, Object>>>> getAdaptLogsTrend(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "gameMode", required = false) String gameMode,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        List<UserModelAdaptLog> logs;
        if (gameMode != null && !gameMode.trim().isEmpty()) {
            logs = userModelAdaptLogRepository.findByUser_UserIdAndGameRecord_GameModeAndLogTimeBetweenOrderByLogTimeDesc(userId, gameMode.trim(), start, end);
        } else {
            logs = userModelAdaptLogRepository.findByUser_UserIdAndLogTimeBetweenOrderByLogTimeDesc(userId, start, end);
        }
        Map<LocalDate, List<UserModelAdaptLog>> byDate = logs.stream().collect(Collectors.groupingBy(l -> l.getLogTime().toLocalDate()));
        List<Map<String, Object>> trend = new ArrayList<>();
        byDate.forEach((date, list) -> {
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.toString());
            OptionalDouble avg = list.stream().filter(l -> l.getAfterDifficulty() != null).mapToDouble(l -> l.getAfterDifficulty().doubleValue()).average();
            point.put("avgAfterDifficulty", avg.isPresent() ? BigDecimal.valueOf(avg.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : null);
            point.put("count", list.size());
            point.put("maxRound", list.stream().mapToInt(UserModelAdaptLog::getRoundNum).max().orElse(0));
            trend.add(point);
        });
        trend.sort(Comparator.comparing(m -> (String) m.get("date")));
        return ResponseEntity.ok(ResultMessage.success(trend));
    }

    // ==================== 游戏模式配置（趣味模式扩展点） ====================

    @GetMapping("/game-mode-configs")
    @Operation(summary = "获取游戏模式配置列表")
    public ResponseEntity<ResultMessage<List<SystemGameModeConfig>>> getGameModeConfigs() {
        return ResponseEntity.ok(ResultMessage.success(gameModeConfigRepository.findAll()));
    }

    @PatchMapping("/game-mode-configs/{id}")
    @Operation(summary = "更新游戏模式配置（mode_k0、monster_init_num、monster_type_pro 等）")
    public ResponseEntity<ResultMessage<SystemGameModeConfig>> updateGameModeConfig(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> updates) {
        return gameModeConfigRepository.findById(id)
                .map(c -> {
                    if (updates.containsKey("mode_k0") && updates.get("mode_k0") != null)
                        c.setMode_k0(new BigDecimal(updates.get("mode_k0").toString()));
                    if (updates.containsKey("monster_init_num") && updates.get("monster_init_num") != null)
                        c.setMonster_init_num(((Number) updates.get("monster_init_num")).intValue());
                    if (updates.containsKey("monster_type_pro") && updates.get("monster_type_pro") != null)
                        c.setMonster_type_pro((String) updates.get("monster_type_pro"));
                    if (updates.containsKey("description") && updates.get("description") != null)
                        c.setDescription((String) updates.get("description"));
                    c.setUpdateTime(new Date());
                    return ResponseEntity.ok(ResultMessage.success(gameModeConfigRepository.save(c)));
                })
                .orElse(ResponseEntity.badRequest().body(ResultMessage.error("配置不存在")));
    }
}