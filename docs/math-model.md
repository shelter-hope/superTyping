# 打字训练游戏动态难度调节数学模型

生效实现是 `DifficultyEngine`（v2，对数几率调节器）。难度系数 \(D\) 跟踪准确率相对目标准确率的偏差，但两条性质是 v1 没有的：**目标附近有死区**，**调整幅度随误差按对数增长**。速度、峰值速度是可选的副信号，轮次和易错字符不再挤进同一个设定点。

v1（线性有界积分器）的问题是均衡难度不唯一：只要 \(A=A^\star\)，任何 \(D\) 都是不动点，玩家就停在「到达该准确率之前」的那个难度上，长期在目标附近来回抖。v2 把误差换算到对数几率空间，再按 \(\lambda\) 朝目标走一步，因此在目标附近有唯一指向。

## 1. 状态与一次更新

记本轮准确率 \(A\in[0,1]\)（大于 1 时先除以 100），当前速度 \(v\)，历史峰值 \(v_h\)，上一轮难度 \(D_{t-1}\)。默认参数：

| 符号 | 默认 | 含义 |
| :--- | :--- | :--- |
| \(A^\star\) | 0.85 | 目标准确率 `targetAccuracy` |
| \(k\) | 6.0 | logistic 斜率 `logitSlope` |
| \(\epsilon\) | 0.03 | 死区半宽 `deadband` |
| \(\lambda_{\text{up}}\) | 0.25 | 升难度步长系数 `lambdaUp` |
| \(\lambda_{\text{down}}\) | 0.5 | 降难度步长系数 `lambdaDown` |
| \(\delta\) | 0.05 | 单步上限 `step` |
| \(D\) | \([0.15,0.95]\)，初始 0.30 | 难度系数 |
| \(v_{\max}\) | 150 字符/分钟 | 速度归一化上界 |
| \(v_{\mathrm{ref}}\) | 120 字符/分钟 | 速度设定点的参考速度 |
| \(s\) | 2.0 | 速度误差折算系数 `speedErrorScale` |
| \(w_A,w_S,w_H\) | 1, 0, 0 | 调节器权重，三者之和必须为 1 |

对数几率

\[
\operatorname{logit}(p) = \ln\frac{p}{1-p},\qquad p\in(10^{-4},\,1-10^{-4}).
\]

观测准确率先夹到开区间 \((\eta,1-\eta)\)，\(\eta=10^{-4}\)，再算 logit，避免 \(p=0\) 或 \(p=1\) 时发散。

误差。准确率项带死区：偏差不超过 \(\epsilon\) 时记 0，单轮噪声不再推动难度。

\[
e_A =
\begin{cases}
w_A\big(\operatorname{logit}(A)-\operatorname{logit}(A^\star)\big), & |A-A^\star|>\epsilon\ \text{且}\ w_A\neq 0,\\[4pt]
0, & \text{否则}.
\end{cases}
\]

速度项把 \((v/v_{\max}-v_{\mathrm{ref}}/v_{\max})\) 乘以 \(s\) 折算到同一量级。默认 \(w_S=w_H=0\)，两项完全不参与：

\[
e_S = w_S\, s\, (V-V^\star),\qquad e_H = w_H\, s\, (H-V^\star),\qquad V=\min(1,\tfrac{v}{v_{\max}}),\ V^\star=\tfrac{v_{\mathrm{ref}}}{v_{\max}}=0.8 .
\]

缺失观测（\(A\)、\(v\)、\(v_h\) 为 null）一律记 0，不用 0 字符/分钟冒充观测。

更新

\[
e = e_A + e_S + e_H,
\]

\[
\lambda =
\begin{cases}
\lambda_{\text{up}}, & e>0,\\
\lambda_{\text{down}}, & e<0,
\end{cases}
\qquad
D_t = \Pi_{[D_{\min},D_{\max}]}\Big(D_{t-1} + \Pi_{[-\delta,\delta]}\big(\lambda\, e/k\big)\Big).
\]

- \(e=0\)（含死区内）时 \(D_t = \Pi(D_{t-1})\)，难度原地不动。
- \(A=0.90\)：\(\operatorname{logit}(0.90)-\operatorname{logit}(0.85)=0.2197\)，\(\Delta D=0.5\times0.2197/6=0.0183\)。
- \(A=0.95\)：差 \(1.2094\)，\(\Delta D=0.1008\)，被步长截成 \(+0.05\)。
- \(A=0.75\)：差 \(-0.636\)，\(\Delta D=-0.053\)，截成 \(-0.05\)。
- \(A=0.88\) 与 \(A=0.82\) 都落在 \(\pm0.03\) 死区里，\(\Delta D=0\)。
- 永远有 \(|D_t-D_{t-1}|\le\delta\)，且 \(D\) 留在 \([D_{\min},D_{\max}]\)。

上调慢下调快（\(\lambda_{\text{up}}<\lambda_{\text{down}}\)）是有意的：对玩家来说，被逼太紧比练太松更难受，所以回退要快、推进要稳。

## 2. 目标难度与能力估计

把一轮观测代入 \(e\) 的根，得到「若玩家稳定在当前准确率，难度应收敛到哪」：

\[
D^\star = D + \frac{\operatorname{logit}(A)-\operatorname{logit}(A^\star)}{k}.
\]

它只用于观测：写入轮次快照的 `targetDifficulty`、以及管理台模拟器，不参与积分。积分器一次只朝 \(D^\star\) 走 \(\lambda\)，所以不会因为单轮爆冷而跳变。

由 logistic 响应模型 \(A=\sigma\big(k(\theta-D)\big)\) 反推玩家能力：

\[
\theta = D + \frac{\operatorname{logit}(A^\star)}{k}.
\]

这是「在该难度下刚好打出目标准确率所需的能力」。给定 \(\theta\)，模型预测任意难度下的准确率 \(\hat A=\sigma\big(k(\theta-D)\big)\)，管理台用它模拟调参效果。

## 3. 输出映射

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

## 4. 易错字符

易错字符只提高出字权重，不降低难度。弱项比例与强化系数：

\[
\rho = \Pi_{[0.1,1]}\Big(\frac{w}{10}\Big),\qquad m = 1+\kappa\rho,\qquad \kappa=\texttt{weakExposureGain}\ (\text{默认 }1).
\]

比例下限 0.1 保证「只有一两个易错字符」也能被明显加权；上限 1 保证 10 个以上不再继续放大。无易错字符时 \(m=1\)，满比例时 \(m=2\)。词池按这个倍率抽样，词库词条权重乘 \(m\) 后四舍五入。无尽模式的遗传路径另外把易错字符写入定向变异。

v1 用 `weightComposure` 兼任「易错字符增益」，语义被复用；v2 单独取出 `weakExposureGain`，`weightComposure` 回到保留字段。

## 5. 一轮只积分一次

1. 首轮出牌读取玩家上次难度或初始值 0.30，不再用历史准确率预推一步。
2. 本轮结算调用一次 `nextDifficulty`，写入 `difficulty_after`。
3. 下一轮出牌直接投影这个 `difficulty_after`，不再积分。
4. 整局结束时，若最后一轮已有 `difficulty_after`，把它记为玩家上次难度。没有轮次结算的旧上传才按整局准确率积分一次，速度缺失按「无观测」处理。

因此玩家看到的难度每轮最多变化 \(0.05\)。

## 6. 代码

- `com.typinggame.game.rule.DifficultyEngine`：`nextDifficulty`（唯一的难度更新）、`targetDifficulty`、`thetaAccuracy`、`predictedAccuracy`
- `com.typinggame.game.rule.DifficultyRule`：缺省参数与配置字段
- `com.typinggame.game.rule.RuleValidator`：校验 `logitSlope∈[1,20]`、`deadband∈[0,0.2]`、`lambdaUp/lambdaDown∈[0,1]`、`step∈(0,0.5]`、`speedErrorScale∈[0.1,10]`
- `com.typinggame.game.service.rule.RuleConfigService`：把新字段写进 `rule_config.config_json`
- `GameRoundServiceImpl.completeRound`：唯一的轮内积分，管理端「重新调整」会走同一条 `nextDifficulty`
- `GameConfigServiceImpl.getNextRound`：只投影已结算难度
- `GameRecordPostProcessServiceImpl`：优先沿用最后一轮的 `difficulty_after`
- 已有库执行 `typingGame/DB/011-difficulty-v2.sql` 把 v2 字段写入当前 active 难度规则（可重复执行）；新库的种子在 `DB/005-game-rule-config.sql`，缺字段时引擎用 `DifficultyRule` 的 Java 默认值
- 管理端「准确率、速度偏差、峰值偏差」三项之和必须为 1。易错强化系数 `weakExposureGain` 单独取值，不参与这个和。
