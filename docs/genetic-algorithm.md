# 遗传进化算法文档

## 一、概述

本系统采用遗传进化算法生成适合用户当前水平的打字训练内容。通过模拟自然选择和遗传机制，系统能够根据用户的表现动态生成个性化的训练字符串，实现"以战代练"的训练目标。

---

## 二、核心概念

遗传算法是一种模拟自然选择和遗传机制的优化算法，在本系统中用于生成适合用户当前水平的打字内容。主要包括以下步骤：

| 步骤 | 名称 | 说明 |
| :--- | :--- | :--- |
| 1 | 父本选择 | 从用户的历史游戏数据中选择表现较好的怪物作为父本 |
| 2 | 基因重组 | 将父本的目标字符串进行交叉，生成新的字符串 |
| 3 | 变异 | 对生成的字符串进行随机修改，增加多样性 |
| 4 | 适应度评估 | 根据用户的表现评估生成内容的难度和适应性 |

---

## 三、算法流程

### 3.1 整体流程图

```
┌─────────────────┐
│ 游戏开始       │
└────────┬────────┘
         │
┌────────▼────────┐
│ 拉取首轮参数   │
└────────┬────────┘
         │
┌────────▼────────┐
│ 生成首轮怪物   │
└────────┬────────┘
         │
┌────────▼────────┐
│ 游戏进行中     │
└────────┬────────┘
         │
┌────────▼────────┐
│ 每轮结束       │
└────────┬────────┘
         │
┌────────▼────────┐
│ 选择父本怪物   │
└────────┬────────┘
         │
┌────────▼────────┐
│ 基因重组与变异 │
└────────┬────────┘
         │
┌────────▼────────┐
│ 生成下一轮单词 │
└────────┬────────┘
         │
┌────────▼────────┐
│ 构建下一轮怪物 │
└────────┬────────┘
         │
┌────────▼────────┐
│ 应用新的难度参数 │
└────────┬────────┘
         │
┌────────▼────────┐
│ 继续游戏或结束  │
└─────────────────┘
```

### 3.2 父本选择

**文件位置**：`typingGame/src/main/java/com/typinggame/service/Impl/EvolutionServiceImpl.java`

**选择逻辑**：

```java
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

@Override
public double calculateParentScore(MonsterData monster) {
    if (monster == null) return 0;
    int errors = monster.getInputErrorCount() != null ? monster.getInputErrorCount() : 0;
    int len = monster.getTargetStr() != null ? monster.getTargetStr().length() : 0;
    return errors * 2.0 + len * 0.5;
}
```

**评分逻辑**：

| 指标 | 权重 | 说明 |
| :--- | :--- | :--- |
| 输入错误次数 | 2.0 | 错误越多，说明该字符串对用户挑战性越大，适合作为父本 |
| 字符串长度 | 0.5 | 长度越长，复杂度越高 |

### 3.3 基因重组与变异

**文件位置**：`typingGame/src/main/java/com/typinggame/service/Impl/EvolutionServiceImpl.java`

**重组逻辑**：

```java
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
```

**变异池构建**：

1. 包含父本字符串的所有字符；
2. 添加用户历史易错字符，以增加针对性练习；
3. 若变异池为空，使用默认字符池（a-z、0-9等）。

**变异概率**：`MUTATION_PROB`，默认值为0.1-0.15，控制变异发生的频率。

### 3.4 生成下一轮单词

**文件位置**：`typingGame/src/main/java/com/typinggame/service/Impl/EvolutionServiceImpl.java`

**生成逻辑**：

```java
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
```

### 3.5 生成下一轮怪物

**文件位置**：`typingGame/src/main/java/com/typinggame/service/Impl/EvolutionServiceImpl.java`

**生成流程**：

```java
@Override
public List<MonsterData> generateNextRoundMonsters(Integer userId, int roundNum, String gameMode, GameAdjustParam adjustParam, List<String> externalWordPool) {
    if (adjustParam == null) {
        return new ArrayList<>();
    }
    
    // 获取怪物数量，限制在3-25之间
    int n = adjustParam.getMonsterCount() != null 
            ? Math.max(3, Math.min(25, adjustParam.getMonsterCount())) : 8;
    // 获取平均字符串长度，限制在1-15之间
    int avgLen = adjustParam.getAvgStringLength() != null 
            ? Math.max(1, Math.min(15, adjustParam.getAvgStringLength())) : 3;
    // 获取基础速度
    BigDecimal baseSpeed = adjustParam.getMonsterSpeed() != null 
            ? adjustParam.getMonsterSpeed() : new BigDecimal("1.0");

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
```

**怪物生成步骤**：

1. **参数获取**：从 `adjustParam` 获取怪物数量、平均字符串长度、基础速度；
2. **系统配置**：读取系统配置的速度和长度限制；
3. **单词生成**：根据是否有外部词源选择不同的生成策略；
4. **类型分配**：按类型占比生成怪物类型槽位；
5. **速度确定**：根据类型和用户配置确定每只怪物的速度；
6. **怪物组装**：组装 MonsterData 对象并返回。

### 3.6 保存父本字符串

**文件位置**：`typingGame/src/main/java/com/typinggame/service/impl/EvolutionServiceImpl.java`

**保存逻辑**：

```java
@Override
public void saveParentStrings(Integer userId, List<MonsterData> parents) {
    if (userId == null || parents == null || parents.isEmpty()) return;
    String joined = parents.stream()
            .map(MonsterData::getTargetStr)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("|"));
    if (joined.length() > 255) joined = joined.substring(0, 255);
    List<StringParent> existingList = stringParentRepository.findByUserIdOrderByUpdateTimeDesc(
            userId, PageRequest.of(0, 1));
    StringParent sp = existingList.isEmpty() ? new StringParent() : existingList.get(0);
    sp.setUserId(userId);
    sp.setStrParents(joined);
    sp.setUpdateTime(LocalDateTime.now());
    stringParentRepository.save(sp);
}
```

**存储方式**：

- 使用 `|` 分隔多个父本字符串；
- 每个用户只保留一条父本记录（覆盖式保存）；
- 记录限制在255字符以内。

**调用时机**：仅在游戏结束、且模式为无尽适应（ENDLESS_ADAPT）时执行。

---

## 四、字符池构建

### 4.1 用户字符池

**文件位置**：`typingGame/src/main/java/com/typinggame/service/Impl/EvolutionServiceImpl.java`

**构建逻辑**：

```java
private List<Character> buildCharPoolForUser(Integer userId, GameMode modeEnum) {
    // 1. 构建基础字符池
    List<Character> pool = buildBasePool(userId, modeEnum);
    
    // 2. 从character_stats表获取错误次数最多的前20个字符
    List<String> weakChars = getWeakCharacters(userId, WEAK_CHARS_LIMIT);
    
    // 3. 每个易错字符追加3次到池中，实现加权
    for (String c : weakChars) {
        if (c != null && !c.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                pool.add(c.charAt(0));
            }
        }
    }
    
    // 4. 如果池为空，使用默认字符
    if (pool.isEmpty()) {
        pool.addAll(DEFAULT_CHAR_POOL);
    }
    
    return pool;
}
```

### 4.2 基础字符池

**构建逻辑**：

```java
private List<Character> buildBasePool(Integer userId, GameMode modeEnum) {
    List<Character> pool = new ArrayList<>();
    
    // 根据用户在该模式下的字符类型配置生成基础字符集合
    GameCharTypeConfig charTypeConfig = gameCharTypeConfigRepository
            .findByUserIdAndGameMode(userId, modeEnum.name())
            .orElse(null);
    
    if (charTypeConfig != null) {
        if (charTypeConfig.getEnableLowercase() != null && charTypeConfig.getEnableLowercase()) {
            for (char c = 'a'; c <= 'z'; c++) pool.add(c);
        }
        if (charTypeConfig.getEnableUppercase() != null && charTypeConfig.getEnableUppercase()) {
            for (char c = 'A'; c <= 'Z'; c++) pool.add(c);
        }
        if (charTypeConfig.getEnableNumber() != null && charTypeConfig.getEnableNumber()) {
            for (char c = '0'; c <= '9'; c++) pool.add(c);
        }
        // ... 其他字符类型
    }
    
    return pool;
}
```

---

## 五、各模式差异

### 5.1 三种模式对比

| 项目 | 无尽适应（endless） | 词源界定（dictionary） | 限时冲刺（limit） |
| :--- | :--- | :--- | :--- |
| 词源 | 后端生成（父本+池） | 外部词库 | 可同无尽或同词源 |
| 父本使用 | 使用 getLatestParentStrings + recombineTwo | 不使用父本 | 同无尽或按实现 |
| 父本写入 | 本局结束 saveRecord 时写入 | 不写父本 | 当前仅ENDLESS写父本 |
| 易错字加权 | 是 | 是（补词时） | 同无尽或同词源 |
| 字符串长度分布 | 正态分布 | 词库原始长度 | 同无尽或同词源 |

### 5.2 无尽适应模式

**特点**：

- 无外部词源；
- 词由 `generateWordsWithLengths` 生成，约50%概率用 `recombineTwo` 生成词，否则用 `buildWordFromPool`；
- 字符池包含易错字加权；
- 父本在本局结束后更新，下一局/下一轮使用。

### 5.3 词源界定模式

**特点**：

- 有外部词源（WordSourceService.getCandidateWords）；
- 词优先来自词库，不足部分用池补；
- 不读父本、不写父本；
- 类型与速度仍由 adjustParam 决定。

### 5.4 限时冲刺模式

**特点**：

- 若未传词库ID，同无尽模式；
- 若传了词库ID，同词源模式；
- 父本写入取决于后端实现（当前仅ENDLESS_ADAPT写入）。

---

## 六、与数学模型的协同

### 6.1 遗传算法与难度模型的关系

```
┌─────────────────────────────────────────────────────────────┐
│                    游戏流程                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  游戏开始                                                    │
│      │                                                       │
│      ▼                                                       │
│  获取首轮参数 ──────┬───────────────────────────────────────│
│      │             │                                         │
│      │             ▼                                         │
│      │    数学模型计算 Dₜ → 速度/数量/长度/类型占比           │
│      │             │                                         │
│      ▼             │                                         │
│  生成首轮怪物 ←────┘                                         │
│      │                                                       │
│      │        ┌─────────────────────────┐                    │
│      │        │   遗传算法              │                    │
│      │        │  父本选择+重组+变异      │                    │
│      ▼        └─────────────────────────┘                    │
│  游戏进行中                                                  │
│      │                                                       │
│      ▼                                                       │
│  轮次完成                                                    │
│      │                                                       │
│      ▼                                                       │
│  获取下一轮参数 ────┬───────────────────────────────────────│
│      │             │                                         │
│      │             ▼                                         │
│      │    数学模型更新 Dₜ → 新的难度参数                     │
│      │             │                                         │
│      ▼             │                                         │
│  生成下一轮怪物 ←───┘                                         │
│      │                                                       │
│      ▼                                                       │
│  游戏结束                                                    │
│      │                                                       │
│      ▼                                                       │
│  保存游戏记录 + 更新父本（无尽模式）+ 更新用户模型参数         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 协同机制

| 阶段 | 数学模型作用 | 遗传算法作用 |
| :--- | :--- | :--- |
| 游戏开始 | 计算初始难度系数D₀ | 生成首轮怪物（父本+池） |
| 游戏进行中 | 实时计算难度参数 | 根据难度参数调整生成策略 |
| 轮次完成 | 更新难度系数Dt | 使用新参数生成下一轮怪物 |
| 游戏结束 | 更新用户模型参数 | 选择父本并保存（无尽模式） |

---

## 七、代码实现要点

### 7.1 核心服务

**EvolutionService**：处理遗传进化算法相关逻辑

| 方法 | 功能 |
| :--- | :--- |
| `selectParents()` | 选择父本 |
| `recombineAndMutate()` | 基因重组与变异 |
| `generateNextRoundWords()` | 生成下一轮单词 |
| `generateNextRoundMonsters()` | 生成下一轮怪物 |
| `saveParentStrings()` | 保存父本字符串 |
| `buildCharPoolForUser()` | 构建用户字符池 |
| `calculateParentScore()` | 计算父本分数 |

### 7.2 工具类

| 类名 | 功能 |
| :--- | :--- |
| `GeneticEvolutionUtil` | 遗传进化算法工具 |
| `NormalDistributionLengthGenerator` | 正态分布长度生成器 |
| `DifficultyCalculationUtil` | 难度计算工具 |
| `GameCoreUtil` | 游戏核心工具 |

### 7.3 数据库表

| 表名 | 用途 |
| :--- | :--- |
| `string_parents` | 存储用户父本字符串 |
| `monster_data` | 存储怪物数据 |
| `character_stats` | 存储字符统计（易错字符来源） |
| `user_model_params` | 存储用户模型参数 |

---

## 八、性能优化

### 8.1 字符池缓存

- 将构建好的字符池缓存到Redis，避免重复计算；
- 设置合理的过期时间，用户字符类型配置变更时清除缓存。

### 8.2 父本选择优化

- 限制父本选择数量（默认取前5个）；
- 使用高效的排序算法（Java内置TimSort）。

### 8.3 正态分布生成

- 使用预计算的正态分布表，避免重复计算；
- 使用Box-Muller变换生成正态分布随机数。

---

## 九、总结

遗传进化算法是本系统的核心特色之一，通过模拟自然选择和遗传机制，系统能够根据用户的表现动态生成个性化的训练内容。与数学调节模型的协同，实现了难度与内容的双重自适应，既保证了训练的针对性，又确保了游戏的趣味性和挑战性。