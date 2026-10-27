# 视觉系统 v2 · 纸与墨

> 打字是排版的一种。所以整个产品按「铅字与校样」的语汇来设计：纸面、发丝线、衬线字、朱红批注；
> 进到对局，纸反过来变成墨——同一套字号与线宽，只换底色。

## 两个域

| 域 | 用在哪 | 底色 | 文字 | 点缀 |
| --- | --- | --- | --- | --- |
| 纸面（默认） | 首页、配置、档案、词库、实验室、排行、设置、结果页、管理台 | `--background` 纸白 | `--foreground` 墨黑 | `--accent` 朱红 |
| 墨色（`.dark` 作用域） | 对局页（`/game`） | `--background` 墨黑 | `--foreground` 米白 | `--accent` 提亮的朱红 |

切域只需要在容器上加 `className="dark"`（`GamePage` 就是这样做的），页头与页脚不需要跟着变——它们本来就落在纸面上。
聊天页虽然是沉浸式（无页脚），但仍留在纸面：连续阅读长文本时，纸比墨舒服。

## 色板

纸面（`src/index.css` 的 `:root`，HSL 三元组，供 Tailwind 的 `hsl(var(--x))` 使用）：

```
background 44 41% 95%     纸       foreground 43 19% 7%    墨
card       42 38% 91%     纸上更浅的纸 panel→card
muted      42 24% 89%     hover    muted-foreground 40 11% 37%  灰字（正文用这个，别用 /60）
accent     6 66% 49%      朱红     chart-3 168 32% 26%  胭脂（回避）
border     42 23% 81%     发丝线   chart-4 42 55% 40%  赭石（智慧）
chart-1    43 19% 7%      墨       chart-2 6 66% 49%    朱红
```

墨色（`.dark`）：`background 40 6% 6%`、`foreground 40 22% 90%`、`card 40 6% 9%`、
`muted-foreground 40 9% 58%`、`accent 6 72% 58%`、`border 40 7% 19%`。

## 字体

三族变量在 `@theme inline` 里注册，Tailwind 用 `font-serif / font-sans / font-mono`：

- **衬线**（`--font-serif`，默认标题/数字/怪字）：`Noto Serif SC`（@fontsource 子集，按 unicode-range 分片，首屏只拉需要的片）
- **无衬线**（正文、按钮、表单）：`Inter` + 系统中文字体
- **等宽**（英文标签、数据、键盘）：`JetBrains Mono`

三者都是本地依赖（`@fontsource/*`），离线可用，不依赖 Google Fonts CDN。

## 排版规则

1. 层级用**字号 + 衬线/无衬线对比**，不靠阴影、渐变、发光。
2. 英文小标一律 `.label-mono`（11px、字距 0.22em、大写）；它前面常配 `.status-light`（朱红小圆点）。
3. 分隔用 **1px 发丝线**（`border-border`、`divide-y divide-border`），不用卡片阴影。
4. 圆角几乎为零（`--radius: 0.125rem`）；卡片是「纸上的一块区域」，不是「浮起的玻璃」。
5. 数字统一 `.font-serif .tabular`，可带小号单位（`<span className="text-sm">cpm</span>`）。
6. 强调只有两种：**朱红**、**加粗**。不要第三种颜色来说同一件事。
7. 引文/说明用 `.pull-quote`（左侧 2px 朱红竖线）。

## 组件

- `@/components/common/PageHeader`：`{ eyebrow, title, description?, actions?, children? }`——所有页面第一屏都用它。
- `@/components/common/PageBody`：`container max-w-6xl py-10` 的统一页体容器。
- `Card`：1px 边框 + 零阴影；`CardHeader` 底部发丝线；`CardTitle` 自动衬线。
- `Button`：`default` 墨底米字实心；`outline` 发丝线；`ghost` 无底；`link` 带下划线。大写小标用 `<span className="label-mono">` 包一层。
- `Input/Textarea/Select`：直角、白底、聚焦时边框变墨色，不用发光环。
- `.keycap`：键帽（`.keycap` + 内含文字），用于操作说明。

## 图表

recharts 的 `stroke` / `fill` / `tick.fill` 是 **SVG 呈现属性，不解析 `hsl(var(--x))`**（会整条线不显示）。
图表里一律写具体色值：墨 `#17150f`、朱红 `#ce3a2a`、赭石 `#a8823c`、灰字 `#6e6659`、网格 `#e3dccd`、纸 `#f7f4ed`。
其余样式（网格虚线、细轴线、`isAnimationActive={false}`）在 `components/trends/*.tsx` 与 `components/stats/ErrorDistributionChart.tsx` 里有范例。

## 对局画布

`src/game/inkPalette.ts` 是画布唯一取色口（PIXI 需要用数字色值）。中文怪字用衬线，标签用等宽小标，
锁定怪用朱红、进度下划线用赭石；DNA 刻度、粒子、波纹全部只用墨/朱红/赭石三色。

## 已知遗留

- `src/pages/LabPage.tsx` 等 17 处既有 TypeScript 错误（改造前就存在，不在本次范围内）。
- 难度数学模型（θ 估计 + logistic 目标 + λ 阻尼 + 死区 + 下降快/上升慢）与遗传算法（基因库/代际/适应度/锦标赛选择/混淆矩阵变异）尚未落地，见 `docs/math-model.md`、`docs/genetic-algorithm.md` 的改造章节。
