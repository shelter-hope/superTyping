# 打字训练游戏动态难度调节数学模型

生效实现是 `DifficultyEngine`。难度系数 \(D\) 是一个有界饱和积分器，默认只跟踪准确率相对 \(0.85\) 的偏差。速度、轮次和易错字符不再挤进同一个设定点。

## 1. 状态与一次更新

记本轮准确率 \(A\in[0,1]\)（大于 1 时先除以 100），当前速度 \(v\)，历史峰值 \(v_h\)，上一轮难度 \(D_{t-1}\)。默认参数：

| 符号 | 默认 | 含义 |
| :--- | :--- | :--- |
| \(A^\star\) | 0.85 | 目标准确率 `targetAccuracy` |
| \(g\) | 0.35 | 增益 `gain` |
| \(\delta\) | 0.05 | 单步上限 `step` |
| \(D\) | \([0.15,0.95]\)，初始 0.30 | 难度系数 |
| \(v_{\max}\) | 150 字符/分钟 | 速度归一化上界 |
| \(v_{\mathrm{ref}}\) | 120 字符/分钟 | 速度设定点所用的参考速度 |
| \(w_A,w_S,w_H\) | 1, 0, 0 | 调节器权重，三者之和必须为 1 |

速度设定点

\[
S^\star = \min\left(1,\ \frac{v_{\mathrm{ref}}}{v_{\max}}\right) = 0.8 .
\]

观测

\[
S = \min\left(1,\ \frac{v}{v_{\max}}\right),\qquad
H = \min\left(1,\ \frac{v_h}{v_{\max}}\right).
\]

\(v\) 缺失时速度偏差记 0，不用 0 字符/分钟冒充观测。\(v_h\) 缺失时峰值偏差记 0，不再把本轮速度再归一化一次。

误差

\[
e = w_A(A-A^\star) + w_S(S-S^\star) + w_H(H-S^\star).
\]

更新

\[
D_t =
\begin{cases}
\Pi_{[D_{\min},D_{\max}]}(D_{t-1}) & A \text{ 缺失}\\[4pt]
\Pi_{[D_{\min},D_{\max}]}\Big(D_{t-1}+\Pi_{[-\delta,\delta]}(g e)\Big) & A \text{ 存在}
\end{cases}
\]

默认权重下 \(e=A-0.85\)。因此：

- \(A=0.85\) 时 \(\Delta D=0\)，难度停在到达该准确率之前的值上。均衡难度不唯一，这是积分器的性质。
- \(A=0.95\) 时 \(\Delta D=0.35\times 0.10=0.035\)。
- \(A=1\) 时 \(0.35\times 0.15=0.0525\)，被步长截成 \(+0.05\)。
- \(A\le 0.85-0.05/0.35\approx 0.707\) 时步长饱和为 \(-0.05\)。
- \(|D_t-D_{t-1}|\le 0.05\)，且 \(D\) 始终留在 \([0.15,0.95]\)。

若把 \(w_S\) 或 \(w_H\) 调高，均衡条件变成 \(e=0\)，准确率要补偿速度偏差：

\[
A = A^\star - \frac{w_S}{w_A}(S-S^\star) - \frac{w_H}{w_A}(H-S^\star).
\]

\(|S-S^\star|\le 1\)，所以速度通道把目标准确率挪开的幅度不超过 \(w_S/w_A\)。默认这两项权重为 0，挪开幅度为 0。参考速度 120 字符/分钟恰好使 \(S-S^\star=0\)。

轮次 \(K\) 和易错字符数不进入 \(e\)。`weightRound` 只作为保留字段，引擎不读取。85% 和 50% 两个阈值只给结算日志打「高准确率推进 / 低准确率回退」标签，不改变 \(e\)。

## 2. 输出映射

先把难度归一化到速度和词长的配置区间：

\[
u = \Pi_{[0,1]}\left(\frac{D-D_{\min}}{D_{\max}-D_{\min}}\right).
\]

怪物速度和平均词长铺满配置边界。`speedSpan` 大于 1 时按满覆盖处理，默认 1。

\[
\begin{aligned}
V &= v_{\min} + \min(1,\texttt{speedSpan})\, u\, (v_{\max}-v_{\min}),\\
L &= \mathrm{round}\big(L_{\min} + u(L_{\max}-L_{\min})\big).
\end{aligned}
\]

默认边界 \([0.8,3.0]\) 与 \([1,15]\) 时，\(D=0.15\) 给出 \(V=0.8,\ L=1\)，\(D=0.95\) 给出 \(V=3.0,\ L=15\)，\(D=0.30\) 给出 \(u=0.1875\)，\(V\approx 1.21\)，\(L=4\)。

怪物数量把轮次留在积分器外面，并且乘上 \(u\)。贴在难度下界的玩家不会因为轮次变多而增加怪物：

\[
R(K)=\min\left(1,\ \frac{\log(K+1)}{\log 32}\right),
\]

\[
N=\Pi_{[N_{\min},N_{\max}]}\Big(\mathrm{round}(3 + 16u + 6u\, R(K))\Big).
\]

| \(D\) | \(K\) | \(N\) |
| ---: | ---: | ---: |
| 0.15 | 任意 | 3 |
| 0.30 | 1 | 6 |
| 0.95 | 31 | 25 |

类型占比用 \(100u\) 走完配置斜率，四舍五入，和为 100。默认从 \(u=0\) 的普通 80 / 攻击 5 / 回避 8 / 智慧 7，到 \(u=1\) 的 35 / 20 / 20 / 25。

## 3. 易错字符

易错字符只提高出字权重，不降低难度。弱项比例 \(\rho=\min(1,w/10)\)，强化系数 \(\kappa\) 默认 1：

\[
m = 1 + \kappa\rho.
\]

无易错字符时 \(m=1\)；10 个易错字符时 \(m=2\)。词池按这个倍率抽样，词库词条权重乘 \(m\) 后四舍五入。无尽模式的遗传路径另外把易错字符写入定向变异。

## 4. 一轮只积分一次

1. 首轮出牌读取玩家上次难度或初始值 0.30，不再用历史准确率预推一步。
2. 本轮结算调用一次 `nextDifficulty`，写入 `difficulty_after`。
3. 下一轮出牌直接投影这个 `difficulty_after`，不再积分。
4. 整局结束时，若最后一轮已有 `difficulty_after`，把它记为玩家上次难度。没有轮次结算的旧上传才按整局准确率积分一次，速度缺失按“无观测”处理。

因此玩家看到的难度每轮最多变化 \(0.05\)。

## 5. 代码

- `com.typinggame.game.rule.DifficultyEngine`
- `com.typinggame.game.rule.DifficultyRule`：缺省参数
- `GameRoundServiceImpl.completeRound`：唯一的轮内积分
- `GameConfigServiceImpl.getNextRound`：只投影已结算难度
- `GameRecordPostProcessServiceImpl`：优先沿用最后一轮的 `difficulty_after`
- 已有库执行 `typingGame/DB/009-difficulty-regulator.sql` 改写 active 难度规则。新库的种子在 `DB/005-game-rule-config.sql`

管理端「准确率、速度偏差、峰值偏差」三项之和必须为 1。易错强化系数单独取值，不参与这个和。
