# 遗传进化算法文档

> 本文只写**当前代码里真实存在**的遗传逻辑。旧的 `EvolutionServiceImpl` 单体早已拆掉，
> 若你在别处看到 `selectParents` / `recombineAndMutate` 之类的描述，那些是历史文档，不要照着改代码。

## 一、它解决什么问题

打字训练要「以战代练」：让下一局出现的目标，刚好卡在玩家**打过、但打错过**的那条线上。
难度模型决定这一局有多难（速度、数量、长度），遗传算法决定**具体是哪些字符**——
把历史里打得最吃力（适应度最高）的字符串当父本，交叉、变异，生成下一批目标。

## 二、代码结构

| 位置 | 职责 |
| :--- | :--- |
| `typingGame/game-core-svc/src/main/java/com/typinggame/game/rule/GeneticEngine.java` | 纯函数引擎：交叉、变异、锦标赛选择、精英保留、长度归一 |
| `.../rule/GeneticRule.java` | 可配置参数：父本上限、变异率、交叉方式、精英比例、锦标赛规模、适应度权重 |
| `.../rule/RuleValidator.java` | 参数校验（变异率 ∈ [0,1]、精英比例 ∈ [0,0.5] 等） |
| `.../rule/RuleConfigProvider.java` | 从 `game_rule_config`（active 行）读取遗传规则，缺字段取 Java 默认值 |
| `.../service/impl/GameConfigServiceImpl.java#buildGeneticMonsters` | **局内**进化：用本轮怪物的 `monster_attempt` 做父本，生成下一轮的怪物 |
| `.../service/word/EndlessAdaptiveWordSourceStrategy.java` | **跨局**进化：用 `string_parents` 里的历史字符串做父本，生成新词 |

数据流：

```
本局结算 ──> monster_attempt(fitness, target_str) ──> 局内进化 ──> 下一轮怪物的 target_str
                         │                                        （generation / parent_attempt_id / mutation_positions 记进 monster_data）
                         └──> string_parents（无尽模式）──> 跨局进化 ──> 新对局的词
```

## 三、算法

### 3.1 父本

- **局内**：`monster_attempt` 按 `fitness` 降序，取前 `inRoundParentLimit` 个；
  父本再按锦标赛抽出（`tournamentSize` 个候选取 fitness 最高），因此「打得好」的字符串更容易留下后代。
- **跨局**：`string_parents.strParents`（`|` 分隔、整局覆盖式保存）按 `crossRoundParentLimit` 截断。
  这一层只有顺序、没有局内适应度，所以锦标赛规模为 1、精英比例为 0。

### 3.2 交叉

`GeneticEngine.crossover(first, second, length, mode, random)`，三种模式由 `GeneticRule.crossover` 决定：

| 模式 | 行为 |
| :--- | :--- |
| `SINGLE_POINT` | 在 `[0, length]` 随机切一刀，前半取父本 A、后半取父本 B |
| `TWO_POINT` | 随机切两刀，`[0,a)` 取 A、`[a,b)` 取 B、`[b,length)` 取 A（默认） |
| `UNIFORM` | 每个位置独立掷硬币决定取 A 还是 B |

字符串按**位置**取字符，越界回绕（`charAt(index % length)`），因此无论父本长短，
子代长度恒等于目标 `length`。v1 的「把自己第 0 位反复追加来补齐」已删除——它会把同一个错字放大成整段。

### 3.3 变异

`GeneticEngine.mutate(...)` 在 `mutationRate` 概率下改**一个**位置，替换字符的优先级：

1. **混淆对**（`Map<String, List<String>> confusion`）：如果这个字符有一对一本正经的「打错成什么」的记录，就朝那个方向变异；
2. **弱项字符**（`character_stats` 里错误次数多的字符，经 `WordSourceContext.focusChars` 传入）；
3. **字母表兜底**：`a-z0-9` 里随机换一个，绝不等于原字符。

> 现状：混淆对已经接通。前端在错键时记录「期待字符 → 实际字符」，结算时以
> `confusions: [{ expectedChar, typedChar, count }]` 随游戏记录上报；服务端写入
> `key_confusion` 表（`013-key-confusion.sql`），下一局生成时由
> `KeyConfusionService.getConfusionMap(userId, 20, 3)` 取该用户最强的 20 组混淆、
> 每组最多 3 个替代字符，注入 `GeneticEngine.Request.confusion`。局内遗传
> （`GameConfigServiceImpl.buildGeneticMonsters`）和跨局遗传
> （`EndlessAdaptiveWordSourceStrategy`）都走同一份映射。
>
> 回看：`GET /api/v1/stats/key-confusion?userId=&limit=` 按累计次数返回混淆对，
> 个人档案页的「最常打错的键」直接读它。

### 3.4 选择与精英

```
eliteCount = round(eliteRatio × count)          // 默认 0.1 × 数量
子代循环  ：tournament(父本池, tournamentSize) 选两个 → 交叉 → 变异 → 去重
循环上限  ：max(16, count × 12) 次，凑不满由调用方用字符池补齐
```

- 精英按适应度原样保留（截断到目标长度），避免每代都把最优解拆掉；
- 子代用 `LinkedHashSet` 去重，同一轮里不会出现两个一模一样的目标。

### 3.5 可复现

引擎的随机源由调用方注入，**所有调用点都用确定性种子**：

| 调用点 | 种子 |
| :--- | :--- |
| 局内 | `Objects.hash("in-round-genetic", roundNum, 父本 attemptId 列表)` |
| 跨局 | `Objects.hash(userId, 父本字符串列表, count, length)` |

同一份历史 + 同一份规则配置 → 同一批目标字符。出问题时能把一局完整重放出来，
这也是 `monster_data` 上 `generation / parent_attempt_a_id / parent_attempt_b_id / crossover_point /
mutation_rate / mutation_positions / mutation_chars` 这些审计字段的意义。

## 四、配置项（`GeneticRule`，存于 `game_rule_config.rule_key='genetic'`）

| 字段 | 默认 | 说明 |
| :--- | :--- | :--- |
| `inRoundParentLimit` | 10 | 局内父本上限 |
| `crossRoundParentLimit` | 8 | 跨局父本上限 |
| `inRoundMutationRate` | 0.15 | 局内变异概率 |
| `crossRoundMutationRate` | 0.25 | 跨局变异概率 |
| `crossover` | `TWO_POINT` | `SINGLE_POINT` / `TWO_POINT` / `UNIFORM` |
| `eliteRatio` | 0.1 | 精英保留比例，上限 0.5 |
| `tournamentSize` | 3 | 锦标赛规模，上限 10 |
| `preferWeakChars` | true | 变异是否优先用弱项字符 |
| `fitnessScoreWeight` 等 | 见类 | 适应度权重（评分口径，见下） |

管理台「遗传规则」表单可直接改这些值；改完先存草稿（`draft`），发布（`publish`）后新的 active 版本才生效。
`typingGame/DB/012-genetic-v2.sql` 把 v2 的三个新字段写进 active 行，保证新旧环境口径一致。

## 五、适应度口径

`monster_attempt.fitness` 是本轮怪物的表现评分，局内父本排序与锦标赛都用它：

```
fitness ≈ fitnessScoreWeight × 得分 + fitnessAccuracyWeight × 准确率 / 100 − fitnessErrorPenalty × 错误次数
```

具体公式在结算侧（`GameRecordPostProcessServiceImpl` / `MonsterAttempt` 写入处）维护；
遗传引擎只消费它，不重新定义。

## 六、各模式差异

| 项目 | 无尽适应 `ENDLESS_ADAPT` | 词源界定 `DICTIONARY` | 限时冲刺 `LIMIT_TIME` |
| :--- | :--- | :--- | :--- |
| 词源 | 父本 + 字符池进化 | 外部词库（`WordSource`） | 无词库时同无尽，有词库时同词源 |
| 跨局父本 | 使用并写回 | 不使用 | 视是否传词库而定 |
| 局内进化 | 使用 | 使用 | 使用 |
| 弱项加权 | 是 | 是（补词时） | 同无尽 |

## 七、与难度模型的分工

| 阶段 | 难度模型（见 `docs/math-model.md`） | 遗传算法 |
| :--- | :--- | :--- |
| 开局 | 给出 D₀ → 速度/数量/长度/类型占比 | 用历史父本生成首轮词 |
| 轮中 | 不影响 | 不影响 |
| 回合结算 | 更新 Dₜ | — |
| 下一轮参数 | Dₜ → 新的速度/数量/长度 | 用本轮的 `monster_attempt` 进化出下一轮目标字符 |
| 本局结束 | 更新用户模型参数 | 无尽模式写 `string_parents` |

一句话：**难度决定「多难」，遗传决定「练什么」**，两者在 `GameConfigServiceImpl` 里汇合。

## 八、测试

`typingGame/game-core-svc/src/test/java/com/typinggame/game/rule/GeneticEngineTest.java` 覆盖：

- 三种交叉模式在任意目标长度下都返回严格等长的子代；
- 双点交叉确实同时用到两个父本；
- 给定混淆对时只替换成混淆字符；没有混淆对时替换结果分散（不是固定「下一个字符」）；
- 同种子两次进化结果完全一致；
- 精英保留生效、子代去重；
- 变异率为 0 时文本不变；v1 兼容入口仍可用。

跑法：

```bash
cd typingGame
mvn -pl game-core-svc -am test -Dtest='GeneticEngineTest+RuleEngineTest' -Dsurefire.failIfNoSpecifiedTests=false
```

## 九、已知缺口

1. **混淆对回看**——`GET /api/v1/stats/key-confusion?userId=&limit=` 已返回「该打 A 却打成 B × 次数」，
   个人档案页有「最常打错的键」面板；管理端在「按键混淆」页签按用户查
   `GET /api/v1/admin/key-confusion?userId=&limit=`（`ConfusionAdminService`，同时给出明细与按期望键聚合）；
2. **适应度只用于局内**——跨局父本只有顺序，没有分数；若要跨局也做锦标赛，得把 `string_parents` 升级成带 fitness 的表；
3. **精英只在单次调用内保留**——两次 `evolve` 调用之间没有代际记忆，代际目前只体现在 `monster_data.generation` 字段上。
