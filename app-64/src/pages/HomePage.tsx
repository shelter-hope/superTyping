// 首页：纸与墨（编辑式落地页）
// 叙事顺序：一句主张 → 一张字帖 → 一条机制数据带 → 墨色对局预览 → 机制/模式/怪物/操作
import { Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { ArrowRight } from 'lucide-react';

const MECHANICS = [
  {
    index: '01',
    title: '自适应难度',
    body: '每次结算只走一小步：难度沿误差方向移动，单步封顶，永远留在 [0.15, 0.95] 之间。它不会突然把你甩下车，也不会让你停在舒适区。',
    data: 'D_t = clip(D_t−1 + k·(A − A*), ±δ)',
  },
  {
    index: '02',
    title: '遗传进化',
    body: '你的错字就是基因库。挑出得分最差的字符做父本，交叉、变异，生成下一轮真正能命中你弱点的字符组合。',
    data: 'parent = argmax(error × 2 + len × 0.5)',
  },
  {
    index: '03',
    title: '逐键档案',
    body: '每个字符的准确率、平均停顿、混淆方向都被单独记账。你看到的不是感觉，是具体到某个键的原始记录。',
    data: 'per-key accuracy · latency · confusion',
  },
];

const MODES = [
  {
    index: 'A',
    title: '无尽适应',
    lead: '系统自动分析你的打字数据，生成针对性的字符组合。',
    items: ['自动识别易错字符', '动态调整难度', '无限轮次挑战'],
  },
  {
    index: 'B',
    title: '词源界定',
    lead: '上传自定义词库，在特定词汇范围内进行训练。',
    items: ['支持自定义词库', '专业术语训练', '编程语言练习'],
  },
];

const MONSTERS = [
  { name: '普通型', note: '匀速前进，最容易处理的目标', points: '1 分', color: 'hsl(var(--monster-normal))' },
  { name: '攻击型', note: '定期生成新怪物，一错就连锁', points: '3 分', color: 'hsl(var(--monster-attacker))' },
  { name: '回避型', note: '30% 概率规避你的攻击', points: '2 分', color: 'hsl(var(--monster-evasive))' },
  { name: '智慧型', note: '吸引周围怪物，扎堆而来', points: '3 分', color: 'hsl(var(--monster-smart))' },
];

const KEYS = [
  { keys: ['A–Z'], action: '敲击对应字符攻击怪物，首次输入自动锁定目标' },
  { keys: ['Tab'], action: '取消当前锁定，重新选择目标' },
  { keys: ['Shift'], action: '切换中英文输入模式' },
  { keys: ['Caps'], action: '切换大小写' },
];

export default function HomePage() {
  const { user } = useAuth();

  return (
    <div className="overflow-hidden">
      {/* ─── 主视觉 ─── */}
      <section className="container grid gap-14 pt-14 lg:grid-cols-12 lg:gap-8 lg:pt-24">
        <div className="lg:col-span-7">
          <p className="section-eyebrow">群体智能 · 打字训练</p>

          <h1 className="mt-8 display-serif text-[clamp(2.75rem,6.2vw,4.75rem)] leading-[1.02] tracking-[-0.01em]">
            每一局，
            <br />
            都为你
            <span className="text-accent">重新排版</span>
            <span className="text-accent">。</span>
          </h1>

          <p className="mt-7 max-w-xl text-[15px] leading-[2] text-muted-foreground">
            系统记录你的误触与速度，用遗传算法重排字符，用难度模型把你的准确率稳定在 85%
            —— 那个学得最快、也最不容易放弃的区间。
          </p>

          <div className="mt-9 flex flex-wrap items-center gap-x-6 gap-y-4">
            {user ? (
              <Button size="lg" asChild>
                <Link to="/prepare">
                  开始一局
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            ) : (
              <Button size="lg" asChild>
                <Link to="/register">
                  开始一局
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            )}
            <Link
              to={user ? '/trends' : '/login'}
              className="text-[13px] text-foreground underline decoration-accent decoration-2 underline-offset-[6px] transition-colors hover:text-accent"
            >
              先看我的弱点
            </Link>
          </div>

          <dl className="mt-11 flex flex-wrap items-center gap-x-6 gap-y-3">
            {[
              ['A*', '0.85 目标准确率'],
              ['D', '[0.15, 0.95] 难度区间'],
              ['δ', '0.05 单步上限'],
              ['N', '4 类字符怪物'],
            ].map(([sym, text]) => (
              <div key={sym} className="flex items-baseline gap-2">
                <dt className="label-mono text-[10px] text-accent">{sym}</dt>
                <dd className="mono-num text-[11px] text-muted-foreground">{text}</dd>
              </div>
            ))}
          </dl>
        </div>

        {/* 字帖：用排版本身演示机制 */}
        <div className="lg:col-span-5 lg:pt-6">
          <div className="relative border border-border bg-card">
            <div className="flex items-center justify-between border-b border-border px-6 py-4">
              <span className="label-mono flex items-center gap-2 text-[10px] text-foreground">
                <span className="h-[5px] w-[5px] rounded-full bg-accent" aria-hidden />
                字帖 · 样例
              </span>
              <span className="label-mono text-[10px]">平均 0.425s / 字</span>
            </div>
            <div className="divide-y divide-border">
              <div className="flex items-center justify-between px-6 py-5">
                <div className="flex items-baseline gap-4">
                  <span className="label-mono text-[10px]">01</span>
                  <span className="font-mono text-[19px] tracking-[0.02em]">rhythm</span>
                </div>
                <span className="mono-num text-[11px] text-success">✓ 0.425</span>
              </div>
              <div className="flex items-center justify-between px-6 py-5">
                <div className="flex items-baseline gap-4">
                  <span className="label-mono text-[10px]">02</span>
                  <span className="font-mono text-[19px] tracking-[0.02em]">
                    wa<span className="border-b-2 border-accent text-accent">i</span>†scoat
                    <span className="ml-1 inline-block h-[17px] w-[1px] translate-y-[3px] animate-caret bg-accent align-middle" />
                  </span>
                </div>
                <span className="mono-num text-[11px] text-accent">错 i→t</span>
              </div>
              <div className="flex items-center justify-between px-6 py-5">
                <div className="flex items-baseline gap-4">
                  <span className="label-mono text-[10px]">03</span>
                  <span className="font-mono text-[19px] tracking-[0.02em]">throughput</span>
                </div>
                <span className="mono-num text-[11px] text-success">✓ 0.38</span>
              </div>
            </div>
            <div className="border-t border-border px-6 py-5">
              <p className="pull-quote text-[12.5px] leading-[1.9] text-muted-foreground">
                下一轮会把 <span className="font-mono text-foreground">i / t</span>{' '}
                的混淆编进怪物序列——错过的字符，会回来找你。
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ─── 机制数据带 ─── */}
      <section className="container mt-16 border-y border-foreground/80">
        <dl className="grid grid-cols-1 divide-y divide-border sm:grid-cols-3 sm:divide-x sm:divide-y-0">
          {[
            ['85', '%', '目标准确率', 'TARGET ACCURACY'],
            ['0.05', '', '单步难度上限', 'MAX STEP'],
            ['0.15–0.95', '', '难度有界区间', 'BOUNDED'],
          ].map(([num, unit, cn, en]) => (
            <div key={en} className="px-2 py-7 sm:px-6">
              <div className="flex items-baseline gap-1">
                <span className="display-serif tabular text-[40px] leading-none">{num}</span>
                {unit ? <span className="mono-num text-[13px] text-muted-foreground">{unit}</span> : null}
              </div>
              <div className="mt-3 flex items-baseline gap-2">
                <span className="text-[12px] tracking-[0.04em]">{cn}</span>
                <span className="label-mono text-[9px] opacity-60">{en}</span>
              </div>
            </div>
          ))}
        </dl>
      </section>

      {/* ─── 墨色对局预览 ─── */}
      <section className="mt-16">
        <div className="dark bg-background text-foreground">
          <div className="container py-12 lg:py-16">
            <div className="flex flex-wrap items-baseline justify-between gap-3">
              <span className="label-mono flex items-center gap-2 text-[10px]">
                <span className="h-[5px] w-[5px] rounded-full bg-accent" aria-hidden />
                live · 对局界面示意
              </span>
              <span className="mono-num text-[11px] text-muted-foreground">
                第 7 轮 · 00:42 · 锁定后直接输入
              </span>
            </div>

            <div className="relative mt-8 overflow-hidden border border-border">
              <div
                className="absolute inset-0 opacity-[0.22]"
                aria-hidden
                style={{
                  backgroundImage:
                    'linear-gradient(to right, hsl(var(--border)) 1px, transparent 1px), linear-gradient(to bottom, hsl(var(--border)) 1px, transparent 1px)',
                  backgroundSize: '56px 56px',
                }}
              />
              {[
                'left-0 top-0 border-l border-t',
                'right-0 top-0 border-r border-t',
                'left-0 bottom-0 border-l border-b',
                'right-0 bottom-0 border-r border-b',
              ].map(c => (
                <span
                  key={c}
                  aria-hidden
                  className={`pointer-events-none absolute z-10 h-2.5 w-2.5 border-foreground/40 ${c}`}
                />
              ))}
              <span className="label-mono absolute top-3 left-3 z-10 text-[9px] text-muted-foreground/70">
                wave 07
              </span>

              <div className="relative h-[236px] sm:h-[268px]">
                <span className="mono-num absolute top-[56%] left-[8%] text-[20px] text-muted-foreground/80 sm:text-[24px]">
                  ther
                </span>
                <span className="absolute top-[22%] left-[33%] font-mono text-[22px] text-foreground sm:text-[27px]">
                  wa<span className="text-accent">i</span>ts
                  <span className="absolute -bottom-[15px] left-0 z-10 h-[2px] w-full bg-accent/25">
                    <span className="block h-full w-[62%] bg-accent" />
                  </span>
                </span>
                <span className="mono-num absolute top-[62%] left-[62%] text-[19px] text-muted-foreground/70 sm:text-[22px]">
                  sam
                </span>
                <span className="mono-num absolute top-[17%] left-[76%] text-[18px] text-muted-foreground/60 sm:text-[22px]">
                  coat
                </span>
                <span className="mono-num absolute top-[80%] left-[48%] text-[15px] text-muted-foreground/50 sm:text-[18px]">
                  ri
                </span>
              </div>
              <div className="relative flex flex-wrap items-end gap-x-10 gap-y-4 border-t border-border px-6 py-5">
                {[
                  ['0.83', 'accuracy'],
                  ['27', 'combo'],
                  ['0.42', 'difficulty D'],
                ].map(([v, k]) => (
                  <div key={k} className="min-w-[92px]">
                    <div className="mono-num text-[22px] leading-none">{v}</div>
                    <div className="mt-2 h-[1px] w-full bg-border" />
                    <div className="label-mono mt-2 text-[9px]">{k}</div>
                  </div>
                ))}
                <div className="ml-auto flex flex-wrap items-center gap-1.5">
                  {['Tab', 'Shift', 'Caps'].map(k => (
                    <span key={k} className="keycap">{k}</span>
                  ))}
                </div>
              </div>
            </div>

            <div className="mt-8 grid gap-6 sm:grid-cols-3">
              {[
                ['打字即攻击', '锁定的目标会被逐步击穿，敲错则前功尽弃。'],
                ['Tab 换目标', '放弃当前锁定，重新选一个更近的敌人。'],
                ['结算出档案', '每局结束给你逐键的失误与停顿复盘。'],
              ].map(([t, d]) => (
                <div key={t} className="border-t border-border pt-3">
                  <p className="display-serif text-[14px]">{t}</p>
                  <p className="mt-1.5 text-[12px] leading-[1.8] text-muted-foreground">{d}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ─── 三条机制 ─── */}
      <section className="container mt-20">
        <div className="flex flex-wrap items-baseline justify-between gap-4">
          <h2 className="display-serif text-[clamp(1.6rem,3vw,2.1rem)] tracking-[0.01em]">它到底在做什么</h2>
          <span className="label-mono text-[10px]">机制 · mechanics</span>
        </div>

        <div className="mt-10 grid gap-x-10 gap-y-10 md:grid-cols-3">
          {MECHANICS.map(m => (
            <article key={m.index} className="border-t border-foreground/80 pt-5">
              <div className="flex items-baseline justify-between">
                <span className="label-mono text-[10px]">{m.index}</span>
                <ArrowRight className="h-3.5 w-3.5 opacity-40" aria-hidden />
              </div>
              <h3 className="mt-5 display-serif text-[19px]">{m.title}</h3>
              <p className="mt-3 text-[13px] leading-[1.95] text-muted-foreground">{m.body}</p>
              <p className="mt-4 overflow-x-auto whitespace-nowrap font-mono text-[10.5px] text-muted-foreground/80">
                {m.data}
              </p>
            </article>
          ))}
        </div>
      </section>

      {/* ─── 两种模式 ─── */}
      <section className="container mt-20">
        <div className="flex flex-wrap items-baseline justify-between gap-4">
          <h2 className="display-serif text-[clamp(1.6rem,3vw,2.1rem)] tracking-[0.01em]">两种训练方式</h2>
          <span className="label-mono text-[10px]">模式 · modes</span>
        </div>

        <div className="mt-8 divide-y divide-border border-y border-border">
          {MODES.map(mode => (
            <div key={mode.index} className="grid gap-6 py-8 md:grid-cols-12 md:items-baseline">
              <div className="md:col-span-5">
                <span className="label-mono text-[10px] text-accent">{mode.index}</span>
                <h3 className="mt-3 display-serif text-[22px]">{mode.title}</h3>
                <p className="mt-2 text-[13px] leading-[1.9] text-muted-foreground">{mode.lead}</p>
              </div>
              <ul className="md:col-span-7 md:col-start-7">
                {mode.items.map((item, i) => (
                  <li key={item} className="flex items-baseline gap-4 border-b border-border/70 py-2.5 last:border-0">
                    <span className="label-mono w-6 shrink-0 text-[9px]">{String(i + 1).padStart(2, '0')}</span>
                    <span className="text-[13px]">{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      {/* ─── 怪物图鉴 ─── */}
      <section className="container mt-20">
        <div className="flex flex-wrap items-baseline justify-between gap-4">
          <h2 className="display-serif text-[clamp(1.6rem,3vw,2.1rem)] tracking-[0.01em]">字符怪物</h2>
          <span className="label-mono text-[10px]">图鉴 · bestiary</span>
        </div>

        <ul className="mt-8 grid gap-x-10 gap-y-0 sm:grid-cols-2">
          {MONSTERS.map(m => (
            <li key={m.name} className="flex items-start gap-4 border-b border-border py-5">
              <span
                className="mt-[7px] h-2.5 w-2.5 shrink-0 rounded-full"
                style={{ backgroundColor: m.color }}
                aria-hidden
              />
              <div className="min-w-0 flex-1">
                <div className="flex items-baseline justify-between gap-4">
                  <p className="display-serif text-[17px]">{m.name}</p>
                  <span className="mono-num shrink-0 text-[11px] text-muted-foreground">{m.points}</span>
                </div>
                <p className="mt-1.5 text-[12.5px] leading-[1.8] text-muted-foreground">{m.note}</p>
              </div>
            </li>
          ))}
        </ul>
      </section>

      {/* ─── 操作 ─── */}
      <section className="container mt-20">
        <div className="grid gap-10 md:grid-cols-12">
          <div className="md:col-span-4">
            <h2 className="display-serif text-[clamp(1.6rem,3vw,2.1rem)]">操作</h2>
            <p className="mt-3 text-[13px] leading-[1.9] text-muted-foreground">
              只有键盘。这是打字游戏，不该让鼠标参与。
            </p>
          </div>
          <dl className="md:col-span-8 divide-y divide-border border-t border-border">
            {KEYS.map(k => (
              <div key={k.action} className="flex flex-col gap-2 py-4 sm:flex-row sm:items-center sm:gap-8">
                <dt className="flex w-[132px] shrink-0 gap-1.5">
                  {k.keys.map(key => (
                    <span key={key} className="keycap">{key}</span>
                  ))}
                </dt>
                <dd className="text-[13px] leading-[1.8] text-muted-foreground">{k.action}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      {/* ─── 结尾行动 ─── */}
      <section className="container mt-24">
        <div className="border-t border-foreground/80 pt-10">
          <div className="flex flex-col items-start justify-between gap-8 md:flex-row md:items-end">
            <h2 className="display-serif max-w-2xl text-[clamp(1.9rem,4vw,2.9rem)] leading-[1.25]">
              错一次，
              <br className="sm:hidden" />
              下一局它就站在你面前。
            </h2>
            <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
              <Button size="lg" asChild>
                <Link to={user ? '/prepare' : '/register'}>
                  开始一局
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
              <Link
                to="/leaderboard"
                className="text-[13px] underline decoration-accent decoration-2 underline-offset-[6px] transition-colors hover:text-accent"
              >
                看排行榜
              </Link>
            </div>
          </div>
          <p className="mono-num mt-8 text-[11px] text-muted-foreground">
            50 行代码的进化，胜过 50 小时的盲打。
          </p>
        </div>
      </section>
    </div>
  );
}
