# 设计模式应用方案文档

## 一、概述

本项目采用多种经典设计模式，旨在提高代码的可维护性、扩展性和可读性。以下详细介绍各设计模式的应用场景、实现方式和代码示例。

> **2026-10 现状校正**：文中示例里的 `EvolutionServiceImpl`、`EvolutionStrategy` 等类属于拆分前的单体代码，已不在仓库中。当前实现用「无状态静态引擎 + 规则对象」表达同样的策略切换：遗传侧见 `typingGame/game-core-svc/.../game/rule/GeneticEngine.java`（`crossoverMode` / `eliteRatio` / `tournamentSize` 驱动策略），难度侧见同目录 `DifficultyEngine.java`；前端接口层在 `app-64/src/services/*`。文中的模式意图仍然成立，只是落点换了文件。

---

## 二、策略模式 (Strategy Pattern)

### 2.1 应用场景

**问题背景**: 遗传进化算法需要支持多种进化策略，如不同的父本选择策略、基因重组策略和变异策略。如果使用传统的条件判断方式，代码会变得冗长且难以扩展。

**解决方案**: 使用策略模式将不同的进化策略封装为独立的策略类，通过上下文类动态切换策略。

### 2.2 类图设计

```
┌─────────────────────────────┐
│   EvolutionStrategy        │  ← 抽象策略接口
│  ──────────────────────────│
│ + selectParents(...)       │
│ + recombine(...)           │
│ + mutate(...)              │
└─────────────────────────────┘
        ▲         ▲         ▲
        │         │         │
┌───────┴───────┐ ┌────────┴────────┐ ┌─────────────┴─────────────┐
│ RouletteStrategy│ │ TournamentStrategy│ │ EliteSelectionStrategy │
│  ────────────── │ │ ────────────── │ │ ─────────────────────── │
│ + selectParents │ │ + selectParents │ │ + selectParents         │
│ + recombine     │ │ + recombine     │ │ + recombine             │
│ + mutate        │ │ + mutate        │ │ + mutate                │
└─────────────────┘ └─────────────────┘ └─────────────────────────┘
```

### 2.3 核心代码实现

```java
// EvolutionStrategy.java - 抽象策略接口
public interface EvolutionStrategy {
    List<MonsterRecord> selectParents(List<MonsterRecord> monsters, int parentCount);
    String recombine(String parent1, String parent2);
    String mutate(String target, double mutationRate, Set<Character> errorChars);
}

// RouletteStrategy.java - 轮盘赌选择策略
public class RouletteStrategy implements EvolutionStrategy {
    @Override
    public List<MonsterRecord> selectParents(List<MonsterRecord> monsters, int parentCount) {
        double totalScore = monsters.stream()
            .mapToDouble(m -> calculateScore(m))
            .sum();
        
        List<MonsterRecord> parents = new ArrayList<>();
        for (int i = 0; i < parentCount; i++) {
            double random = Math.random() * totalScore;
            double cumulative = 0;
            for (MonsterRecord monster : monsters) {
                cumulative += calculateScore(monster);
                if (cumulative >= random) {
                    parents.add(monster);
                    break;
                }
            }
        }
        return parents;
    }
    
    private double calculateScore(MonsterRecord monster) {
        return monster.getInputErrorCount() * 1.5 + monster.getSurvivalTime() * 0.5;
    }
    
    @Override
    public String recombine(String parent1, String parent2) {
        int crossoverPoint = (int) (Math.random() * Math.min(parent1.length(), parent2.length()));
        return parent1.substring(0, crossoverPoint) + parent2.substring(crossoverPoint);
    }
    
    @Override
    public String mutate(String target, double mutationRate, Set<Character> errorChars) {
        StringBuilder sb = new StringBuilder(target);
        for (int i = 0; i < sb.length(); i++) {
            if (Math.random() < mutationRate) {
                char mutatedChar = getRandomChar(errorChars);
                sb.setCharAt(i, mutatedChar);
            }
        }
        return sb.toString();
    }
}

// EvolutionContext.java - 策略上下文
public class EvolutionContext {
    private EvolutionStrategy strategy;
    
    public EvolutionContext(EvolutionStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void setStrategy(EvolutionStrategy strategy) {
        this.strategy = strategy;
    }
    
    public List<MonsterRecord> selectParents(List<MonsterRecord> monsters, int parentCount) {
        return strategy.selectParents(monsters, parentCount);
    }
    
    public String recombine(String parent1, String parent2) {
        return strategy.recombine(parent1, parent2);
    }
    
    public String mutate(String target, double mutationRate, Set<Character> errorChars) {
        return strategy.mutate(target, mutationRate, errorChars);
    }
}
```

### 2.4 使用示例

```java
// EvolutionServiceImpl.java
@Service
public class EvolutionServiceImpl implements EvolutionService {
    
    @Override
    public List<MonsterRecord> selectParents(Long recordId) {
        List<MonsterRecord> monsters = monsterRecordRepository.findByRecordId(recordId);
        
        EvolutionStrategy strategy = new RouletteStrategy();
        EvolutionContext context = new EvolutionContext(strategy);
        
        return context.selectParents(monsters, 2);
    }
    
    @Override
    public List<String> recombineAndMutate(List<MonsterRecord> parents, AdjustParam param) {
        EvolutionStrategy strategy = new RouletteStrategy();
        EvolutionContext context = new EvolutionContext(strategy);
        
        List<String> nextRoundTargets = new ArrayList<>();
        for (int i = 0; i < parents.size() - 1; i += 2) {
            String combined = context.recombine(
                parents.get(i).getTargetStr(), 
                parents.get(i + 1).getTargetStr()
            );
            String mutated = context.mutate(combined, 0.1, getErrorChars());
            nextRoundTargets.add(mutated);
        }
        
        return nextRoundTargets;
    }
}
```

---

## 三、工厂模式 (Factory Pattern)

### 3.1 应用场景

**问题背景**: 游戏中有多种类型的怪物（普通怪物、攻击型怪物、闪避型怪物、智能怪物），每种怪物有不同的行为和属性。如果直接在代码中实例化不同类型的怪物，会导致代码耦合度高，难以扩展。

**解决方案**: 使用工厂模式封装怪物的创建逻辑，通过工厂类统一创建不同类型的怪物。

### 3.2 类图设计

```
┌─────────────────────────────┐
│        Monster              │  ← 抽象产品接口
│  ──────────────────────────│
│ + getId()                   │
│ + getTargetStr()            │
│ + getSpeed()                │
│ + move()                    │
│ + update()                  │
└─────────────────────────────┘
        ▲         ▲         ▲         ▲
        │         │         │         │
┌───────┴───────┐ ┌────────┴────────┐ ┌─────────────┴─────────────┐ ┌─────────────┴─────────────┐
│  NormalMonster │ │  AttackerMonster│ │    EvasiveMonster         │ │     SmartMonster         │
│  ───────────── │ │ ───────────────│ │ ───────────────────────── │ │ ───────────────────────── │
│ + move()       │ │ + move()        │ │ + move()                  │ │ + move()                  │
│ + update()     │ │ + update()      │ │ + update()                │ │ + update()                │
└────────────────┘ └─────────────────┘ └──────────────────────────┘ └──────────────────────────┘

┌─────────────────────────────┐
│      MonsterFactory         │  ← 工厂类
│  ──────────────────────────│
│ + createMonster(type, ...)  │
└─────────────────────────────┘
```

### 3.3 核心代码实现

```java
// Monster.java - 抽象产品接口
public interface Monster {
    Long getId();
    String getTargetStr();
    double getSpeed();
    void move(double deltaTime);
    void update(double deltaTime, double playerInputSpeed);
    MonsterType getMonsterType();
}

// NormalMonster.java - 具体产品
public class NormalMonster implements Monster {
    private Long id;
    private String targetStr;
    private double speed;
    private double position;
    
    public NormalMonster(Long id, String targetStr, double speed) {
        this.id = id;
        this.targetStr = targetStr;
        this.speed = speed;
        this.position = 0;
    }
    
    @Override
    public void move(double deltaTime) {
        position += speed * deltaTime;
    }
    
    @Override
    public void update(double deltaTime, double playerInputSpeed) {
        move(deltaTime);
    }
    
    @Override
    public MonsterType getMonsterType() {
        return MonsterType.NORMAL;
    }
    
    // Getters and setters
}

// AttackerMonster.java - 具体产品
public class AttackerMonster implements Monster {
    private Long id;
    private String targetStr;
    private double speed;
    private double position;
    private static final double SPEED_BOOST = 1.3;
    
    public AttackerMonster(Long id, String targetStr, double speed) {
        this.id = id;
        this.targetStr = targetStr;
        this.speed = speed * SPEED_BOOST;
        this.position = 0;
    }
    
    @Override
    public void move(double deltaTime) {
        position += speed * deltaTime;
    }
    
    @Override
    public void update(double deltaTime, double playerInputSpeed) {
        move(deltaTime);
    }
    
    @Override
    public MonsterType getMonsterType() {
        return MonsterType.ATTACKER;
    }
}

// MonsterFactory.java - 工厂类
public class MonsterFactory {
    public static Monster createMonster(Long id, String targetStr, double speed, MonsterType type) {
        return switch (type) {
            case NORMAL -> new NormalMonster(id, targetStr, speed);
            case ATTACKER -> new AttackerMonster(id, targetStr, speed);
            case EVASIVE -> new EvasiveMonster(id, targetStr, speed);
            case SMART -> new SmartMonster(id, targetStr, speed);
        };
    }
}
```

### 3.4 使用示例

```java
// EvolutionServiceImpl.java
@Service
public class EvolutionServiceImpl implements EvolutionService {
    
    @Override
    public List<MonsterDTO> generateNextRoundMonsters(Long userId, AdjustParam param, String gameMode) {
        List<String> targetStrings = generateTargetStrings(userId, param, gameMode);
        List<MonsterDTO> monsters = new ArrayList<>();
        
        for (int i = 0; i < targetStrings.size(); i++) {
            MonsterType type = determineMonsterType(param.getCurrentDifficulty(), i);
            Monster monster = MonsterFactory.createMonster(
                (long) i,
                targetStrings.get(i),
                param.getMonsterSpeed(),
                type
            );
            
            monsters.add(convertToDTO(monster));
        }
        
        return monsters;
    }
    
    private MonsterType determineMonsterType(double difficulty, int index) {
        double random = Math.random();
        if (difficulty < 4) {
            if (random < 0.7) return MonsterType.NORMAL;
            if (random < 0.85) return MonsterType.ATTACKER;
            if (random < 0.95) return MonsterType.EVASIVE;
            return MonsterType.SMART;
        } else if (difficulty < 7) {
            if (random < 0.5) return MonsterType.NORMAL;
            if (random < 0.75) return MonsterType.ATTACKER;
            if (random < 0.9) return MonsterType.EVASIVE;
            return MonsterType.SMART;
        } else {
            if (random < 0.3) return MonsterType.NORMAL;
            if (random < 0.6) return MonsterType.ATTACKER;
            if (random < 0.85) return MonsterType.EVASIVE;
            return MonsterType.SMART;
        }
    }
}
```

---

## 四、观察者模式 (Observer Pattern)

### 4.1 应用场景

**问题背景**: 游戏中有多个组件需要响应游戏状态的变化，如UI界面、统计模块、难度调节模块等。如果直接在游戏核心逻辑中调用这些组件，会导致代码耦合度高，难以维护。

**解决方案**: 使用观察者模式实现游戏状态的发布-订阅机制，当游戏状态变化时，自动通知所有订阅者。

### 4.2 类图设计

```
┌─────────────────────────────┐
│      GameSubject            │  ← 抽象主题
│  ──────────────────────────│
│ + attach(observer)          │
│ + detach(observer)          │
│ + notifyObservers()         │
└─────────────────────────────┘
        ▲
        │
┌───────┴───────┐
│  GameEngine   │  ← 具体主题
│  ─────────────│
│ - observers   │
│ + updateState()│
└────────────────┘
        │
        ├──────────────┬──────────────┬──────────────┐
        ▼              ▼              ▼              ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ UIObserver  │ │ StatsObserver│ │ Difficulty  │ │ AudioObserver│
│ ─────────── │ │ ─────────── │ │ Observer    │ │ ─────────── │
│ + update()  │ │ + update()  │ │ + update()  │ │ + update()  │
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
```

### 4.3 核心代码实现

```java
// GameObserver.java - 抽象观察者接口
public interface GameObserver {
    void update(GameState state);
}

// GameState.java - 游戏状态
public class GameState {
    private int score;
    private int round;
    private double difficulty;
    private int monstersDestroyed;
    private int monstersEscaped;
    
    // Getters and setters
}

// GameSubject.java - 抽象主题
public interface GameSubject {
    void attach(GameObserver observer);
    void detach(GameObserver observer);
    void notifyObservers();
}

// GameEngine.java - 具体主题
public class GameEngine implements GameSubject {
    private List<GameObserver> observers = new ArrayList<>();
    private GameState state = new GameState();
    
    @Override
    public void attach(GameObserver observer) {
        observers.add(observer);
    }
    
    @Override
    public void detach(GameObserver observer) {
        observers.remove(observer);
    }
    
    @Override
    public void notifyObservers() {
        for (GameObserver observer : observers) {
            observer.update(state);
        }
    }
    
    public void updateScore(int points) {
        state.setScore(state.getScore() + points);
        notifyObservers();
    }
    
    public void nextRound() {
        state.setRound(state.getRound() + 1);
        notifyObservers();
    }
    
    public void updateDifficulty(double newDifficulty) {
        state.setDifficulty(newDifficulty);
        notifyObservers();
    }
}

// StatsObserver.java - 具体观察者
public class StatsObserver implements GameObserver {
    private CharacterStatsService statsService;
    private Long userId;
    
    public StatsObserver(CharacterStatsService statsService, Long userId) {
        this.statsService = statsService;
        this.userId = userId;
    }
    
    @Override
    public void update(GameState state) {
        statsService.updateGameStats(userId, state);
    }
}

// DifficultyObserver.java - 具体观察者
public class DifficultyObserver implements GameObserver {
    private DifficultyCalculationUtil difficultyUtil;
    
    public DifficultyObserver(DifficultyCalculationUtil difficultyUtil) {
        this.difficultyUtil = difficultyUtil;
    }
    
    @Override
    public void update(GameState state) {
        double newDifficulty = difficultyUtil.calculate(state);
        state.setDifficulty(newDifficulty);
    }
}
```

### 4.4 使用示例

```java
// GameConfigServiceImpl.java
@Service
public class GameConfigServiceImpl implements GameConfigService {
    
    @Override
    public NextRoundResponseDTO getNextRoundConfig(Long userId, Integer roundNum, String gameMode) {
        GameEngine engine = new GameEngine();
        
        StatsObserver statsObserver = new StatsObserver(characterStatsService, userId);
        DifficultyObserver difficultyObserver = new DifficultyObserver(difficultyCalculationUtil);
        
        engine.attach(statsObserver);
        engine.attach(difficultyObserver);
        
        engine.nextRound();
        engine.updateScore(0);
        
        return buildResponse(engine.getState());
    }
}
```

---

## 五、模板方法模式 (Template Method Pattern)

### 5.1 应用场景

**问题背景**: 游戏中有多种模式（无尽适应模式、词源界定模式、限时冲刺模式），每种模式的流程相似但有不同的细节。如果为每种模式编写独立的流程代码，会导致大量重复代码。

**解决方案**: 使用模板方法模式定义游戏流程的骨架，将具体步骤延迟到子类实现。

### 5.2 类图设计

```
┌─────────────────────────────┐
│     GameModeTemplate        │  ← 抽象模板类
│  ──────────────────────────│
│ + startGame()               │  ← 模板方法
│ # initialize()              │  ← 抽象方法
│ # generateContent()         │  ← 抽象方法
│ # checkEndCondition()       │  ← 抽象方法
│ # calculateScore()          │  ← 抽象方法
│ # finalizeGame()            │  ← 抽象方法
└─────────────────────────────┘
        ▲         ▲         ▲
        │         │         │
┌───────┴───────┐ ┌────────┴────────┐ ┌─────────────┴─────────────┐
│ EndlessMode   │ │ DictionaryMode  │ │    LimitMode              │
│  ─────────────│ │ ────────────────│ │ ──────────────────────────│
│ # initialize()│ │ # initialize()  │ │ # initialize()            │
│ # generate... │ │ # generate...   │ │ # generate...             │
│ # checkEnd... │ │ # checkEnd...   │ │ # checkEnd...             │
│ # calculate.. │ │ # calculate..   │ │ # calculate..             │
│ # finalize... │ │ # finalize...   │ │ # finalize...             │
└────────────────┘ └─────────────────┘ └──────────────────────────┘
```

### 5.3 核心代码实现

```java
// GameModeTemplate.java - 抽象模板类
public abstract class GameModeTemplate {
    
    public final GameResult startGame(Long userId, GameSettings settings) {
        initialize(userId, settings);
        
        GameState state = new GameState();
        while (!checkEndCondition(state)) {
            List<MonsterDTO> monsters = generateContent(state);
            processRound(state, monsters);
        }
        
        ScoreResult score = calculateScore(state);
        return finalizeGame(state, score);
    }
    
    protected abstract void initialize(Long userId, GameSettings settings);
    
    protected abstract List<MonsterDTO> generateContent(GameState state);
    
    protected abstract boolean checkEndCondition(GameState state);
    
    protected abstract ScoreResult calculateScore(GameState state);
    
    protected abstract GameResult finalizeGame(GameState state, ScoreResult score);
    
    protected void processRound(GameState state, List<MonsterDTO> monsters) {
        for (MonsterDTO monster : monsters) {
            state.incrementTotalChars(monster.getTargetStr().length());
        }
        state.incrementRound();
    }
}

// EndlessMode.java - 具体模板实现
public class EndlessMode extends GameModeTemplate {
    private EvolutionService evolutionService;
    private AdjustParam currentParam;
    
    @Override
    protected void initialize(Long userId, GameSettings settings) {
        currentParam = new AdjustParam();
        currentParam.setCurrentDifficulty(1.0);
        currentParam.setMonsterCount(3);
        currentParam.setMonsterSpeed(1.0);
    }
    
    @Override
    protected List<MonsterDTO> generateContent(GameState state) {
        return evolutionService.generateNextRoundMonsters(
            state.getUserId(), 
            currentParam, 
            "endless"
        );
    }
    
    @Override
    protected boolean checkEndCondition(GameState state) {
        return state.getMonstersEscaped() >= 3 || state.isUserQuit();
    }
    
    @Override
    protected ScoreResult calculateScore(GameState state) {
        ScoreResult result = new ScoreResult();
        result.setTotalScore(state.getScore());
        result.setAvgSpeed(state.getTotalChars() / state.getGameDuration() * 60);
        result.setAvgAccuracy(state.getCorrectChars() * 100.0 / state.getTotalChars());
        return result;
    }
    
    @Override
    protected GameResult finalizeGame(GameState state, ScoreResult score) {
        GameResult result = new GameResult();
        result.setScore(score);
        result.setDifficulty(currentParam.getCurrentDifficulty());
        return result;
    }
}

// LimitMode.java - 具体模板实现
public class LimitMode extends GameModeTemplate {
    private int timeLimit;
    private long startTime;
    
    @Override
    protected void initialize(Long userId, GameSettings settings) {
        this.timeLimit = settings.getTimeLimit();
        this.startTime = System.currentTimeMillis();
    }
    
    @Override
    protected List<MonsterDTO> generateContent(GameState state) {
        AdjustParam param = new AdjustParam();
        param.setCurrentDifficulty(calculateDifficulty(state));
        return evolutionService.generateNextRoundMonsters(
            state.getUserId(), 
            param, 
            "limit"
        );
    }
    
    @Override
    protected boolean checkEndCondition(GameState state) {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        return elapsed >= timeLimit;
    }
    
    @Override
    protected ScoreResult calculateScore(GameState state) {
        ScoreResult result = new ScoreResult();
        result.setTotalScore(state.getScore());
        result.setAvgSpeed(state.getTotalChars() / timeLimit * 60);
        result.setAvgAccuracy(state.getCorrectChars() * 100.0 / state.getTotalChars());
        return result;
    }
    
    @Override
    protected GameResult finalizeGame(GameState state, ScoreResult score) {
        GameResult result = new GameResult();
        result.setScore(score);
        result.setTimeUsed(timeLimit);
        return result;
    }
}
```

### 5.4 使用示例

```java
// GameService.java
@Service
public class GameService {
    
    public GameResult startGame(Long userId, String gameMode, GameSettings settings) {
        GameModeTemplate template = switch (gameMode) {
            case "endless" -> new EndlessMode();
            case "dictionary" -> new DictionaryMode();
            case "limit" -> new LimitMode();
            default -> throw new IllegalArgumentException("Unknown game mode: " + gameMode);
        };
        
        return template.startGame(userId, settings);
    }
}
```

---

## 六、代理模式 (Proxy Pattern)

### 6.1 应用场景

**问题背景**: 游戏中的某些操作需要进行权限验证、日志记录或性能监控，如用户认证、游戏记录保存等。如果直接在业务逻辑中添加这些功能，会导致代码臃肿且难以维护。

**解决方案**: 使用代理模式在不修改原始类的情况下，为其添加额外的功能。

### 6.2 类图设计

```
┌─────────────────────────────┐
│      GameRecordService      │  ← 抽象主题接口
│  ──────────────────────────│
│ + createRecord(record)      │
│ + getRecordById(id)         │
│ + deleteRecord(id)          │
└─────────────────────────────┘
        ▲         ▲
        │         │
┌───────┴───────┐ ┌───────────┴───────────┐
│  GameRecord   │ │  GameRecordService    │
│  ServiceImpl  │ │       Proxy           │
│  ─────────────│ │ ─────────────────────│
│ + createRecord│ │ + createRecord        │
│ + getRecord.. │ │ + getRecordById       │
│ + deleteRecord│ │ + deleteRecord        │
└───────────────┘ └───────────────────────┘
        │                              │
        └──────────────────────────────┘
                 委托调用
```

### 6.3 核心代码实现

```java
// GameRecordService.java - 抽象主题接口
public interface GameRecordService {
    GameRecordResponseDTO createRecord(GameRecordRequestDTO request);
    GameRecordResponseDTO getRecordById(Long recordId);
    void deleteRecord(Long recordId);
}

// GameRecordServiceImpl.java - 真实主题
@Service
public class GameRecordServiceImpl implements GameRecordService {
    
    @Autowired
    private GameRecordRepository gameRecordRepository;
    
    @Override
    public GameRecordResponseDTO createRecord(GameRecordRequestDTO request) {
        GameRecord record = convertToEntity(request);
        GameRecord saved = gameRecordRepository.save(record);
        return convertToDTO(saved);
    }
    
    @Override
    public GameRecordResponseDTO getRecordById(Long recordId) {
        GameRecord record = gameRecordRepository.findById(recordId)
            .orElseThrow(() -> new BusinessException("Record not found"));
        return convertToDTO(record);
    }
    
    @Override
    public void deleteRecord(Long recordId) {
        gameRecordRepository.deleteById(recordId);
    }
}

// GameRecordServiceProxy.java - 代理类
public class GameRecordServiceProxy implements GameRecordService {
    
    private GameRecordService target;
    private Logger logger = LoggerFactory.getLogger(GameRecordServiceProxy.class);
    
    public GameRecordServiceProxy(GameRecordService target) {
        this.target = target;
    }
    
    @Override
    public GameRecordResponseDTO createRecord(GameRecordRequestDTO request) {
        logger.info("Creating game record for user: {}", request.getUserId());
        
        long startTime = System.currentTimeMillis();
        try {
            GameRecordResponseDTO result = target.createRecord(request);
            logger.info("Game record created successfully, duration: {}ms", 
                System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            logger.error("Failed to create game record", e);
            throw e;
        }
    }
    
    @Override
    public GameRecordResponseDTO getRecordById(Long recordId) {
        logger.info("Getting game record by id: {}", recordId);
        return target.getRecordById(recordId);
    }
    
    @Override
    public void deleteRecord(Long recordId) {
        logger.info("Deleting game record: {}", recordId);
        
        if (!SecurityContextHolder.getContext().getAuthentication()
            .getAuthorities().contains("ROLE_ADMIN")) {
            throw new BusinessException("Permission denied");
        }
        
        target.deleteRecord(recordId);
        logger.info("Game record deleted successfully: {}", recordId);
    }
}
```

### 6.4 使用示例

```java
// GameRecordController.java
@RestController
@RequestMapping("/api/game/records")
public class GameRecordController {
    
    private GameRecordService gameRecordService;
    
    @Autowired
    public GameRecordController(GameRecordServiceImpl gameRecordServiceImpl) {
        this.gameRecordService = new GameRecordServiceProxy(gameRecordServiceImpl);
    }
    
    @PostMapping
    public ApiResponse<GameRecordResponseDTO> createRecord(@RequestBody GameRecordRequestDTO request) {
        GameRecordResponseDTO result = gameRecordService.createRecord(request);
        return ApiResponse.success(result);
    }
}
```

---

## 七、装饰器模式 (Decorator Pattern)

### 7.1 应用场景

**问题背景**: 游戏中的怪物需要支持多种能力增强，如速度加成、护盾、攻击力提升等。如果为每种能力组合创建一个子类，会导致类爆炸问题。

**解决方案**: 使用装饰器模式动态地为怪物添加额外的能力，而不需要创建大量子类。

### 7.2 类图设计

```
┌─────────────────────────────┐
│        Monster              │  ← 抽象组件接口
│  ──────────────────────────│
│ + getId()                   │
│ + getSpeed()                │
│ + getTargetStr()            │
│ + move()                    │
└─────────────────────────────┘
        ▲         ▲
        │         │
┌───────┴───────┐ ┌───────────┴───────────┐
│  NormalMonster│ │   MonsterDecorator    │
│  ─────────────│ │ ─────────────────────│
│ + move()      │ │ - monster            │
│ + getSpeed()  │ │ + move()             │
│ + ...         │ │ + getSpeed()         │
└───────────────┘ └───────────────────────┘
                          ▲
         ┌────────────────┼────────────────┐
         ▼                ▼                ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│SpeedDecorator│    │ ShieldDecorator│    │ AttackDecorator│
│ ─────────── │    │ ─────────── │    │ ─────────── │
│ + getSpeed()│    │ + takeDamage│    │ + getDamage │
│ + move()    │    │ + move()    │    │ + move()    │
└─────────────┘    └─────────────┘    └─────────────┘
```

### 7.3 核心代码实现

```java
// Monster.java - 抽象组件接口
public interface Monster {
    Long getId();
    double getSpeed();
    String getTargetStr();
    void move(double deltaTime);
    double getHealth();
    void takeDamage(double damage);
}

// NormalMonster.java - 具体组件
public class NormalMonster implements Monster {
    private Long id;
    private double speed;
    private String targetStr;
    private double health = 100;
    
    @Override
    public double getSpeed() { return speed; }
    @Override
    public void move(double deltaTime) { /* ... */ }
    @Override
    public double getHealth() { return health; }
    @Override
    public void takeDamage(double damage) { health -= damage; }
}

// MonsterDecorator.java - 抽象装饰器
public abstract class MonsterDecorator implements Monster {
    protected Monster monster;
    
    public MonsterDecorator(Monster monster) {
        this.monster = monster;
    }
    
    @Override
    public Long getId() { return monster.getId(); }
    @Override
    public String getTargetStr() { return monster.getTargetStr(); }
    @Override
    public double getSpeed() { return monster.getSpeed(); }
    @Override
    public void move(double deltaTime) { monster.move(deltaTime); }
    @Override
    public double getHealth() { return monster.getHealth(); }
    @Override
    public void takeDamage(double damage) { monster.takeDamage(damage); }
}

// SpeedDecorator.java - 具体装饰器
public class SpeedDecorator extends MonsterDecorator {
    private double speedMultiplier;
    
    public SpeedDecorator(Monster monster, double speedMultiplier) {
        super(monster);
        this.speedMultiplier = speedMultiplier;
    }
    
    @Override
    public double getSpeed() {
        return monster.getSpeed() * speedMultiplier;
    }
}

// ShieldDecorator.java - 具体装饰器
public class ShieldDecorator extends MonsterDecorator {
    private double shieldAmount;
    
    public ShieldDecorator(Monster monster, double shieldAmount) {
        super(monster);
        this.shieldAmount = shieldAmount;
    }
    
    @Override
    public void takeDamage(double damage) {
        double actualDamage = Math.max(0, damage - shieldAmount);
        monster.takeDamage(actualDamage);
    }
}
```

### 7.4 使用示例

```java
// MonsterFactory.java (扩展)
public class MonsterFactory {
    public static Monster createMonster(Long id, String targetStr, double speed, 
                                         MonsterType type, List<Ability> abilities) {
        Monster monster = switch (type) {
            case NORMAL -> new NormalMonster(id, targetStr, speed);
            case ATTACKER -> new AttackerMonster(id, targetStr, speed);
            case EVASIVE -> new EvasiveMonster(id, targetStr, speed);
            case SMART -> new SmartMonster(id, targetStr, speed);
        };
        
        for (Ability ability : abilities) {
            monster = switch (ability.getType()) {
                case SPEED_BOOST -> new SpeedDecorator(monster, ability.getMultiplier());
                case SHIELD -> new ShieldDecorator(monster, ability.getAmount());
                case ATTACK_BOOST -> new AttackDecorator(monster, ability.getMultiplier());
                default -> monster;
            };
        }
        
        return monster;
    }
}
```

---

## 八、设计模式总结

| 设计模式 | 应用场景 | 核心价值 |
| :--- | :--- | :--- |
| **策略模式** | 遗传进化算法的多种策略 | 消除条件判断，支持策略扩展 |
| **工厂模式** | 怪物对象的创建 | 封装创建逻辑，解耦产品与调用方 |
| **观察者模式** | 游戏状态变化通知 | 实现状态解耦，支持多组件响应 |
| **模板方法模式** | 多种游戏模式的流程控制 | 提取公共流程，差异化实现细节 |
| **代理模式** | 权限验证、日志记录、性能监控 | 透明添加功能，符合开闭原则 |
| **装饰器模式** | 怪物能力增强 | 动态组合功能，避免类爆炸 |

### 设计模式选择原则

1. **开闭原则**: 对扩展开放，对修改关闭
2. **单一职责**: 每个类只负责一个功能
3. **依赖倒置**: 依赖抽象而非具体实现
4. **接口隔离**: 为客户端提供最小接口

### 未来扩展建议

| 扩展方向 | 推荐模式 | 说明 |
| :--- | :--- | :--- |
| 新增怪物类型 | 工厂模式 + 策略模式 | 通过工厂创建，策略定义行为 |
| 新增游戏模式 | 模板方法模式 | 复用流程骨架，实现差异化步骤 |
| 新增统计维度 | 观察者模式 | 添加新的观察者响应状态变化 |
| 新增能力效果 | 装饰器模式 | 动态组合多种能力 |