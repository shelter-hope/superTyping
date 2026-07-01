package com.typinggame.service.impl;

import com.typinggame.dto.game.GameAdjustParam;
import com.typinggame.dto.game.GamePlayerPerformanceDTO;
import com.typinggame.entity.config.GameCharTypeConfig;
import com.typinggame.entity.config.SystemGameModeConfig;
import com.typinggame.entity.config.SystemMonsterCommoConfig;
import com.typinggame.entity.game.MonsterData;
import com.typinggame.entity.game.StringParent;
import com.typinggame.entity.game.UserMonsterConfig;
import com.typinggame.entity.model.ModelConstant;
import com.typinggame.entity.model.UserModelParams;
import com.typinggame.enums.GameMode;
import com.typinggame.enums.MonsterType;
import com.typinggame.repository.CharacterStatsRepository;
import com.typinggame.repository.GameCharTypeConfigRepository;
import com.typinggame.repository.GameModeConfigRepository;
import com.typinggame.repository.StringParentRepository;
import com.typinggame.repository.SystemMonsterCommoConfigRepository;
import com.typinggame.repository.UserModelParamsRepository;
import com.typinggame.repository.UserMonsterConfigRepository;
import com.typinggame.service.DifficultyModelService;
import com.typinggame.service.EvolutionService;
import com.typinggame.service.MathModelService;
import com.typinggame.util.NormalDistributionLengthGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 遗传进化服务实现：父代选择、重组变异、下一轮词生成、父本持久化
 */
@Slf4j
@Service
public class EvolutionServiceImpl implements EvolutionService {

    // 遗传算法参数
    private static final int DEFAULT_PARENT_TOP_N = 10; // 选择父本的数量
    private static final int WEAK_CHARS_LIMIT = 20;     // 易错字符限制
    private static final int DEFAULT_WORD_LEN_MIN = 2;  // 默认单词最小长度
    private static final int DEFAULT_WORD_LEN_MAX = 5;  // 默认单词最大长度
    private static final double MUTATION_PROB = 0.3;    // 变异概率
    private static final List<Character> DEFAULT_CHAR_POOL = new ArrayList<>();
    private static final String DEFAULT_CHAR_TYPES = "lower,upper,number,punct,operator,chinese,special";

    static {
        for (char c = 'a'; c <= 'z'; c++) DEFAULT_CHAR_POOL.add(c);
        for (char c = '0'; c <= '9'; c++) DEFAULT_CHAR_POOL.add(c);
    }

    @Autowired
    private CharacterStatsRepository characterStatsRepository;
    @Autowired
    private StringParentRepository stringParentRepository;
    @Autowired
    private UserModelParamsRepository userModelParamsRepository;
    @Autowired
    private DifficultyModelService difficultyModelService;
    @Autowired
    private MathModelService mathModelService;
    @Autowired
    private SystemMonsterCommoConfigRepository systemMonsterCommoConfigRepository;
    @Autowired
    private GameModeConfigRepository gameModeConfigRepository;
    @Autowired
    private GameCharTypeConfigRepository gameCharTypeConfigRepository;
    @Autowired
    private UserMonsterConfigRepository userMonsterConfigRepository;

    private static final BigDecimal DEFAULT_MIN_SPEED = new BigDecimal("0.8");
    private static final BigDecimal DEFAULT_MAX_SPEED = new BigDecimal("3.0");
    private static final int DEFAULT_MIN_STR_LEN = 1;
    private static final int DEFAULT_MAX_STR_LEN = 15;

    private String evolutionStrategy = "default";

    /**
     * 选择父本：从怪物列表中选择表现较好的作为父本
     * @param userId 用户ID
     * @param monsters 怪物列表
     * @return 选择的父本列表
     */
    @Override
    public List<MonsterData> selectParents(Integer userId, List<MonsterData> monsters) {
        if (monsters == null || monsters.isEmpty()) return new ArrayList<>();
        List<MonsterData> scored = new ArrayList<>(monsters);
        // 按父本分数降序排序
        scored.sort((a, b) -> Double.compare(calculateParentScore(b), calculateParentScore(a)));
        // 选择前N个作为父本，N为默认值或怪物数量的一半
        int n = Math.min(DEFAULT_PARENT_TOP_N, (scored.size() + 1) / 2);
        return scored.subList(0, n);
    }

    /**
     * 基因重组与变异：将父本字符串进行交叉和变异，生成新的字符串
     * @param parentStrings 父本字符串列表
     * @param errorChars 错误字符列表
     * @return 生成的新字符串列表
     */
    @Override
    public List<String> recombineAndMutate(List<String> parentStrings, List<String> errorChars) {
        if (parentStrings == null || parentStrings.isEmpty()) return new ArrayList<>();
        Random rnd = new Random();
        List<String> result = new ArrayList<>();
        List<Character> mutationPool = new ArrayList<>();
        
        // 构建变异池：包含父本字符串的所有字符
        for (String s : parentStrings) {
            for (int i = 0; i < s.length(); i++) mutationPool.add(s.charAt(i));
        }
        // 添加错误字符到变异池，以增加针对性练习
        for (String c : errorChars) {
            if (c != null && !c.isEmpty()) mutationPool.add(c.charAt(0));
        }
        // 如果变异池为空，使用默认字符池
        if (mutationPool.isEmpty()) mutationPool.addAll(DEFAULT_CHAR_POOL);

        // 对每个父本进行重组和变异
        for (int i = 0; i < parentStrings.size(); i++) {
            // 随机选择两个父本
            String p1 = parentStrings.get(rnd.nextInt(parentStrings.size()));
            String p2 = parentStrings.get(rnd.nextInt(parentStrings.size()));
            int len = Math.min(p1.length(), p2.length());
            
            // 如果长度小于2，直接使用父本
            if (len < 2) {
                result.add(p1);
                continue;
            }
            
            // 基因重组：随机选择交叉点
            int cut = 1 + rnd.nextInt(len - 1);
            String child = p1.substring(0, cut) + p2.substring(cut);
            
            // 基因变异：以一定概率随机替换字符
            if (rnd.nextDouble() < MUTATION_PROB && child.length() > 0 && !mutationPool.isEmpty()) {
                int pos = rnd.nextInt(child.length());
                char newChar = mutationPool.get(rnd.nextInt(mutationPool.size()));
                child = child.substring(0, pos) + newChar + child.substring(pos + 1);
            }
            result.add(child);
        }
        return result;
    }

    @Override
    public List<String> generateNextRoundWords(Integer userId, int roundNum, int wordCount) {
        return generateNextRoundWords(userId, roundNum, wordCount, (String) null, null);
    }

    @Override
    public List<String> generateNextRoundWords(Integer userId, int roundNum, int wordCount, List<String> externalWordPool) {
        return generateNextRoundWords(userId, roundNum, wordCount, null, externalWordPool);
    }

    @Override
    public List<String> generateNextRoundWords(Integer userId, int roundNum, int wordCount, String gameMode) {
        return generateNextRoundWords(userId, roundNum, wordCount, gameMode, null);
    }

    /**
     * 生成下一轮单词：根据游戏模式和外部词源生成适合用户的单词
     * @param userId 用户ID
     * @param roundNum 轮次
     * @param wordCount 单词数量
     * @param gameMode 游戏模式
     * @param externalWordPool 外部词源池
     * @return 生成的单词列表
     */
    @Override
    public List<String> generateNextRoundWords(Integer userId, int roundNum, int wordCount, String gameMode, List<String> externalWordPool) {
        List<String> result = new ArrayList<>();
        
        // 如果有外部词源，优先使用外部词源
        if (externalWordPool != null && !externalWordPool.isEmpty()) {
            List<String> copy = new ArrayList<>(externalWordPool);
            Collections.shuffle(copy);
            for (int i = 0; i < Math.min(wordCount, copy.size()); i++) {
                String w = copy.get(i);
                if (w != null && !w.trim().isEmpty()) result.add(w.trim());
            }
        }
        
        // 如果外部词源足够，直接返回
        if (result.size() >= wordCount) {
            return result.subList(0, wordCount);
        }
        
        // 确定游戏模式，默认为无尽适应模式
        GameMode modeEnum = (gameMode != null && !gameMode.trim().isEmpty())
                ? GameMode.fromClientMode(gameMode) : GameMode.ENDLESS_ADAPT;
        
        // 构建用户的字符池，包含易错字符
        List<Character> pool = buildCharPoolForUser(userId, modeEnum);
        Random rnd = new Random();
        
        // 生成剩余需要的单词
        while (result.size() < wordCount) {
            // 随机生成单词长度
            int len = DEFAULT_WORD_LEN_MIN + rnd.nextInt(DEFAULT_WORD_LEN_MAX - DEFAULT_WORD_LEN_MIN + 1);
            // 从字符池构建单词
            result.add(buildWordFromPool(pool, len, rnd));
        }
        return result;
    }

    /**
     * 调整难度：根据用户表现计算并调整游戏难度
     * @param userId 用户ID
     * @param performance 游戏表现数据
     * @return 调整后的速度值
     */
    @Override
    public double adjustDifficulty(Integer userId, GamePlayerPerformanceDTO performance) {
        // 仅针对无尽适应模式（ENDLESS_ADAPT）计算难度，模式标识统一使用枚举名
        Optional<UserModelParams> opt = userModelParamsRepository.findByUserIdAndGameMode(
                userId, GameMode.ENDLESS_ADAPT.name());
        if (opt.isEmpty() || performance == null) return 0.5;
        UserModelParams params = opt.get();
        // 获取上一轮的难度系数，默认为0.3
        BigDecimal lastD = params.getLastDifficulty() != null ? params.getLastDifficulty() : new BigDecimal("0.3");

        // 使用 DifficultyModelService 聚合历史指标（A_h、S_hmax、i_t、R_e），并与实时表现合并
        GamePlayerPerformanceDTO aggregated = difficultyModelService.buildAggregatedPerformance(
                userId, GameMode.ENDLESS_ADAPT.name());
        GamePlayerPerformanceDTO perf = new GamePlayerPerformanceDTO();
        // 实时表现来自调用方（建议配置/当前局）
        perf.setRealAccuracy(performance.getRealAccuracy());
        perf.setRealSpeed(performance.getRealSpeed());
        perf.setCurrentRound(performance.getCurrentRound());
        // 历史与错误统计来自聚合结果
        if (aggregated != null) {
            perf.setHistoryAvgAccuracy(aggregated.getHistoryAvgAccuracy());
            perf.setHistoryPeakSpeed(aggregated.getHistoryPeakSpeed());
            perf.setAvgCharErrorRate(aggregated.getAvgCharErrorRate());
            perf.setErrorCharRatio(aggregated.getErrorCharRatio());
        }

        // 获取模型常量
        ModelConstant constant = mathModelService.getModelConstant();

        // 计算当前难度系数 Dt
        BigDecimal currentD = com.typinggame.util.DifficultyCalculationUtil.calculateCurrentDifficulty(
                lastD, perf, constant, params);

        // 仍复用 ParamAdjustUtil.adjustMonsterSpeed 基于简化 GameRecord 计算速度
        BigDecimal realAcc = perf.getRealAccuracy() != null ? perf.getRealAccuracy() : BigDecimal.ZERO;
        BigDecimal realSpeed = perf.getRealSpeed() != null ? perf.getRealSpeed() : new BigDecimal("40");
        BigDecimal peakSpeed = perf.getHistoryPeakSpeed() != null ? perf.getHistoryPeakSpeed() : realSpeed;
        int round = perf.getCurrentRound() != null ? perf.getCurrentRound() : 1;

        com.typinggame.entity.game.GameRecord synthetic = new com.typinggame.entity.game.GameRecord();
        synthetic.setAvgAccuracy(realAcc);
        synthetic.setAvgSpeed(realSpeed);
        synthetic.setPeakSpeed(peakSpeed);
        synthetic.setRoundNum(round);

        MonsterData lastParam = new MonsterData();
        lastParam.setSpeed(new BigDecimal("1.0"));
        BigDecimal newSpeed = com.typinggame.util.ParamAdjustUtil.adjustMonsterSpeed(
                lastParam.getSpeed(), synthetic, currentD, lastD, constant, params);
        return newSpeed.doubleValue();
    }

    @Override
    public void setEvolutionStrategy(String strategy) {
        this.evolutionStrategy = strategy != null ? strategy : "default";
    }

    @Override
    public MonsterData cloneMonster(MonsterData monster) {
        if (monster == null) return null;
        MonsterData c = new MonsterData();
        c.setTargetStr(monster.getTargetStr());
        c.setSpeed(monster.getSpeed());
        c.setSurvivalTime(monster.getSurvivalTime());
        c.setInputErrorCount(monster.getInputErrorCount());
        c.setMonsterType(monster.getMonsterType());
        c.setScore(monster.getScore());
        return c;
    }

    /**
     * 计算父本分数
     * @param monster 怪物数据
     * @return 父本分数
     */
    @Override
    public double calculateParentScore(MonsterData monster) {
        if (monster == null) return 0;
        int errors = monster.getInputErrorCount() != null ? monster.getInputErrorCount() : 0;
        int len = monster.getTargetStr() != null ? monster.getTargetStr().length() : 0;
        return errors * 2.0 + len * 0.5;
    }

    @Override
    public List<String> getEvolutionStrategies() {
        List<String> strategies = new ArrayList<>();
        strategies.add("default");
        strategies.add("aggressive");
        return strategies;
    }

    /**
     * 保存父本字符串：将选择的父本保存到数据库，供下一轮使用
     * @param userId 用户ID
     * @param parents 父本怪物列表
     */
    @Override
    public void saveParentStrings(Integer userId, List<MonsterData> parents) {
        if (userId == null || parents == null || parents.isEmpty()) return;
        String joined = parents.stream()
                .map(MonsterData::getTargetStr)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("|"));
        if (joined.length() > 255) joined = joined.substring(0, 255);
        // string_parents 表对 user_id 有唯一约束（uk_user_parents），同一用户只保留一条父本记录，
        // 因此这里需要更新最新一条记录而不是总是插入新行。
        List<StringParent> existingList = stringParentRepository.findByUserIdOrderByUpdateTimeDesc(
                userId, org.springframework.data.domain.PageRequest.of(0, 1));
        StringParent sp = existingList.isEmpty() ? new StringParent() : existingList.get(0);
        sp.setUserId(userId);
        sp.setStrParents(joined);
        if (sp.getCreateTime() == null) {
            sp.setCreateTime(LocalDateTime.now());
        }
        sp.setUpdateTime(LocalDateTime.now());
        stringParentRepository.save(sp);
    }

    @Override
    public List<MonsterData> generateNextRoundMonsters(Integer userId, int roundNum, String gameMode, GameAdjustParam adjustParam) {
        return generateNextRoundMonsters(userId, roundNum, gameMode, adjustParam, null);
    }

    /**
     * 生成下一轮怪物：根据调节参数生成适合用户的怪物集
     * @param userId 用户ID
     * @param roundNum 当前轮次
     * @param gameMode 游戏模式
     * @param adjustParam 调节参数（速度、数量、长度、类型占比）
     * @param externalWordPool 外部词源池
     * @return 生成的怪物列表
     */
    @Override
    public List<MonsterData> generateNextRoundMonsters(Integer userId, int roundNum, String gameMode, GameAdjustParam adjustParam, List<String> externalWordPool) {
        if (adjustParam == null) {
            return new ArrayList<>();
        }
        
        // 获取怪物数量，限制在3-25之间
        int n = adjustParam.getMonsterCount() != null ? Math.max(3, Math.min(25, adjustParam.getMonsterCount())) : 8;
        // 获取平均字符串长度，限制在1-15之间
        int avgLen = adjustParam.getAvgStringLength() != null ? Math.max(1, Math.min(15, adjustParam.getAvgStringLength())) : 3;
        // 获取基础速度
        BigDecimal baseSpeed = adjustParam.getMonsterSpeed() != null ? adjustParam.getMonsterSpeed() : new BigDecimal("1.0");

        // 获取系统配置的速度和长度限制
        BigDecimal minSpeed = DEFAULT_MIN_SPEED;
        BigDecimal maxSpeed = DEFAULT_MAX_SPEED;
        int minStrLen = DEFAULT_MIN_STR_LEN;
        int maxStrLen = DEFAULT_MAX_STR_LEN;
        List<SystemMonsterCommoConfig> configs = systemMonsterCommoConfigRepository.findAll();
        if (!configs.isEmpty()) {
            SystemMonsterCommoConfig c = configs.get(0);
            if (c.getMinSpeed() != null) minSpeed = c.getMinSpeed();
            if (c.getMaxSpeed() != null) maxSpeed = c.getMaxSpeed();
            if (c.getMinStrLength() != null) minStrLen = c.getMinStrLength();
            if (c.getMaxStrLength() != null) maxStrLen = c.getMaxStrLength();
        }
        // 确保速度在限制范围内
        baseSpeed = baseSpeed.max(minSpeed).min(maxSpeed);

        // 确定游戏模式
        GameMode modeEnum = GameMode.fromClientMode(gameMode);
        // 构建用户的字符池
        List<Character> pool = buildCharPoolForUser(userId, modeEnum);

        // 生成单词
        List<String> words;
        if (externalWordPool != null && !externalWordPool.isEmpty()) {
            // 使用外部词源
            words = new ArrayList<>();
            for (String w : externalWordPool) {
                if (w != null && !w.trim().isEmpty()) words.add(w.trim());
            }
            Collections.shuffle(words);
            if (words.size() > n) words = words.subList(0, n);
            // 不足部分生成新单词
            while (words.size() < n) {
                words.add(generateOneWord(pool, avgLen));
            }
        } else {
            // 生成符合正态分布的长度数组
            int[] lengths = NormalDistributionLengthGenerator.generateNormalDistLengths(
                    Math.max(minStrLen, Math.min(maxStrLen, avgLen)), n);
            // 确保长度在限制范围内
            for (int i = 0; i < lengths.length; i++) {
                lengths[i] = Math.max(minStrLen, Math.min(maxStrLen, lengths[i]));
            }
            // 根据长度生成单词
            words = generateWordsWithLengths(userId, pool, lengths);
            // 调整单词数量
            if (words.size() != n) {
                while (words.size() < n) {
                    words.add(generateOneWord(pool, avgLen));
                }
                words = words.subList(0, n);
            }
        }

        // 构建怪物类型槽位
        List<MonsterType> typeSlots = buildTypeSlots(
                adjustParam.getNormalRatio(),
                adjustParam.getAttackRatio(),
                adjustParam.getEvasiveRatio(),
                adjustParam.getSmartRatio(),
                n);
        Collections.shuffle(typeSlots);

        // 获取用户的怪物配置
        Map<String, UserMonsterConfig> userMonsterByType = new HashMap<>();
        try {
            List<UserMonsterConfig> userConfigs = userMonsterConfigRepository.findByUserId(userId);
            if (userConfigs != null) {
                for (UserMonsterConfig uc : userConfigs) {
                    if (uc.getType() != null && !uc.getType().isEmpty()) {
                        userMonsterByType.put(uc.getType(), uc);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 生成怪物
        List<MonsterData> monsters = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String typeName = typeSlots.get(i).name();
            // 根据类型确定速度
            BigDecimal speed = resolveSpeedForType(typeName, baseSpeed, userMonsterByType, minSpeed, maxSpeed);
            MonsterData md = new MonsterData();
            md.setTargetStr(words.get(i));
            md.setSpeed(speed);
            md.setMonsterType(typeName);
            md.setScore(typeSlots.get(i).score);
            md.setSpawnWave(roundNum);
            md.setSurvivalTime(0);
            md.setInputErrorCount(0);
            md.setIsParent(false);
            monsters.add(md);
        }
        return monsters;
    }

    /** 按类型取速度：有用户配置则用 init_speed（再与系统上下限 clamp），否则用 baseSpeed */
    private BigDecimal resolveSpeedForType(String typeName, BigDecimal baseSpeed,
                                           Map<String, UserMonsterConfig> userMonsterByType,
                                           BigDecimal minSpeed, BigDecimal maxSpeed) {
        UserMonsterConfig uc = userMonsterByType != null ? userMonsterByType.get(typeName) : null;
        BigDecimal speed = baseSpeed;
        if (uc != null && uc.getInitSpeed() != null) {
            speed = uc.getInitSpeed();
        }
        return speed.max(minSpeed).min(maxSpeed);
    }

    private List<Character> buildCharPoolForUser(Integer userId, GameMode modeEnum) {
        List<Character> basePool = buildBasePool(userId, modeEnum);
        List<Character> pool = new ArrayList<>(basePool);
        List<com.typinggame.entity.user.CharacterStats> weak =
                characterStatsRepository.findByUserIdOrderByErrorCountDesc(userId, PageRequest.of(0, WEAK_CHARS_LIMIT));
        for (com.typinggame.entity.user.CharacterStats cs : weak) {
            if (cs.getCharValue() != null && !cs.getCharValue().isEmpty()) {
                for (int i = 0; i < 3; i++) pool.add(cs.getCharValue().charAt(0));
            }
        }
        if (pool.isEmpty()) pool.addAll(DEFAULT_CHAR_POOL);
        return pool;
    }

    private List<String> generateWordsWithLengths(Integer userId, List<Character> pool, int[] lengths) {
        List<String> parentStrings = getLatestParentStrings(userId);
        Random rnd = new Random();
        List<String> result = new ArrayList<>();
        for (int len : lengths) {
            if (len <= 0) len = 1;
            String word;
            if (!parentStrings.isEmpty() && rnd.nextDouble() < 0.5) {
                String p = parentStrings.get(rnd.nextInt(parentStrings.size()));
                if (p != null && p.length() >= 2) {
                    word = recombineTwo(parentStrings, pool, rnd, len);
                } else {
                    word = buildWordFromPool(pool, len, rnd);
                }
            } else {
                word = buildWordFromPool(pool, len, rnd);
            }
            result.add(word);
        }
        return result;
    }

    private String recombineTwo(List<String> parentStrings, List<Character> pool, Random rnd, int targetLen) {
        String p1 = parentStrings.get(rnd.nextInt(parentStrings.size()));
        String p2 = parentStrings.get(rnd.nextInt(parentStrings.size()));
        if (p1 == null || p1.isEmpty()) return buildWordFromPool(pool, targetLen, rnd);
        if (p2 == null || p2.isEmpty()) p2 = p1;
        int len = Math.min(p1.length(), p2.length());
        if (len < 2) return buildWordFromPool(pool, targetLen, rnd);
        int cut = 1 + rnd.nextInt(len - 1);
        String child = p1.substring(0, cut) + p2.substring(cut);
        while (child.length() < targetLen && !pool.isEmpty()) {
            child += pool.get(rnd.nextInt(pool.size()));
        }
        if (child.length() > targetLen) child = child.substring(0, targetLen);
        return child;
    }

    private String buildWordFromPool(List<Character> pool, int len, Random rnd) {
        if (pool.isEmpty() || len <= 0) return "a";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(pool.get(rnd.nextInt(pool.size())));
        }
        return sb.toString();
    }

    private String generateOneWord(List<Character> pool, int len) {
        return buildWordFromPool(pool, Math.max(1, len), new Random());
    }

    private List<Character> buildBasePool(Integer userId, GameMode modeEnum) {
        String charType = DEFAULT_CHAR_TYPES;
        try {
            Optional<SystemGameModeConfig> sysMode = gameModeConfigRepository.findByModeName(modeEnum.name());
            if (sysMode.isPresent()) {
                Integer modeId = sysMode.get().getId();
                GameCharTypeConfig cfg = gameCharTypeConfigRepository
                        .findByUserIdAndGameModeId(userId, modeId)
                        .orElse(null);
                if (cfg != null && cfg.getCharType() != null && !cfg.getCharType().trim().isEmpty()) {
                    charType = cfg.getCharType();
                }
            }
        } catch (Exception ignored) {
        }
        Set<String> types = parseTypes(charType);
        if (types.isEmpty()) types = parseTypes(DEFAULT_CHAR_TYPES);

        List<Character> base = new ArrayList<>();
        if (types.contains("lower")) for (char c = 'a'; c <= 'z'; c++) base.add(c);
        if (types.contains("upper")) for (char c = 'A'; c <= 'Z'; c++) base.add(c);
        if (types.contains("number")) for (char c = '0'; c <= '9'; c++) base.add(c);
        if (types.contains("punct")) {
            for (char c : new char[]{',', '.', '!', '?', ';', ':', '\'', '"'}) base.add(c);
        }
        if (types.contains("operator")) {
            for (char c : new char[]{'+', '-', '*', '/', '=', '<', '>', '(', ')', '[', ']', '{', '}'}) base.add(c);
        }
        if (types.contains("special")) {
            for (char c : new char[]{'@', '#', '$', '%', '^', '&', '_', '~', '\\', '|'}) base.add(c);
        }
        if (types.contains("chinese")) {
            String common = "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能下过子说产种面而方后多定行学法所民得经十三之进着等部度家电力里如水化高自二理起小物现实加量都两体制机当使点从业本去把性好应开它合还因由其些然前外天政四日那社义事平形相全表间样与关各重新线内数正心反你明看原又么利比或但质气第向道命此变条只没结解问意建月公无系军很情者最立代想已通并提直题党程展五果料象员革位入常文总次品式活设及管特件长求老头基资边流路级少图山统接知较将组见计别她手角期根论运农指几九区强放决西被干做必战先回则任取据处队南给色光门即保治北造百规热领七海口东导器压志世金增争济阶油思术极交受联什认六共权收证改清己美再采转更单风切打白教速花带安场身车例真务具万每目至达走积示议声报斗完类八离华名确才科张信马节话米整空元况今集温传土许步群广石记需段研界拉林律叫且究观越织装影算低持音众书布复容儿须际商非验连断深难近矿千周委素技备半办青省列习响约支般史感劳便团往酸历市克何除消构府称太准精值号率族维划选标写存候毛亲快效斯院查江型眼王按格养易置派层片始却专状育厂京识适属圆包火住调满县局照参红细引听该铁价严龙飞";
            for (int i = 0; i < common.length(); i++) base.add(common.charAt(i));
        }
        if (base.isEmpty()) base.addAll(DEFAULT_CHAR_POOL);
        return base;
    }

    private Set<String> parseTypes(String charType) {
        if (charType == null) return Collections.emptySet();
        String raw = charType.trim();
        if (raw.isEmpty()) return Collections.emptySet();
        String[] parts = raw.split("[,，\\s]+");
        Set<String> allowed = new HashSet<>(Arrays.asList("lower", "upper", "number", "punct", "operator", "special", "chinese"));
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String p : parts) {
            if (p == null) continue;
            String s = p.trim().toLowerCase();
            if (s.equals("numbers")) s = "number";
            if (s.equals("punctuation")) s = "punct";
            if (s.equals("operators")) s = "operator";
            if (s.equals("specialchars") || s.equals("special_char") || s.equals("special-char")) s = "special";
            if (allowed.contains(s)) out.add(s);
        }
        return out;
    }

    private List<String> getLatestParentStrings(Integer userId) {
        List<StringParent> list = stringParentRepository.findByUserIdOrderByUpdateTimeDesc(
                userId, PageRequest.of(0, 1));
        if (list.isEmpty()) return Collections.emptyList();
        String joined = list.get(0).getStrParents();
        if (joined == null || joined.isEmpty()) return Collections.emptyList();
        return Arrays.stream(joined.split("\\|")).filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList());
    }

    private List<MonsterType> buildTypeSlots(Integer normal, Integer attack, Integer evasive, Integer smart, int total) {
        int n = normal != null ? normal : 60;
        int a = attack != null ? attack : 10;
        int e = evasive != null ? evasive : 15;
        int s = smart != null ? smart : 15;
        int sum = n + a + e + s;
        if (sum != 100) {
            n = 60; a = 10; e = 15; s = 15;
        }
        List<MonsterType> slots = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            int r = i % 100;
            if (r < n) slots.add(MonsterType.NORMAL);
            else if (r < n + a) slots.add(MonsterType.ATTACKER);
            else if (r < n + a + e) slots.add(MonsterType.EVASIVE);
            else slots.add(MonsterType.SMART);
        }
        return slots;
    }
}
