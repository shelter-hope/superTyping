import type { GameRecordUploadResult } from '@/db/api';
import type { CharacterStat, GameStats } from '@/game/types';
import ErrorDistributionChart from '@/components/stats/ErrorDistributionChart';
import { Button } from '@/components/ui/button';
import { buildReviewDiagnosis, displayChar } from '@/features/game/reviewDiagnosis';

interface GameResultViewProps {
  stats: GameStats;
  characterStats: CharacterStat[];
  uploadResult: GameRecordUploadResult | null;
  showDetailedResult: boolean;
  onShowDetails: () => void;
  onBackToSummary: () => void;
  onRestart: () => void;
  onBackToPrepare: () => void;
}

const MODE_LABEL: Record<GameStats['mode'], string> = {
  ENDLESS: '无尽适应',
  TIMED: '限时挑战',
  SURVIVAL: '生存模式',
};

function StatBlock({ label, value, tone }: { label: string; value: string | number; tone?: string }) {
  return (
    <div className="px-5 py-4">
      <div className="label-mono text-muted-foreground">{label}</div>
      <div className={`mono-num mt-1.5 font-serif text-[30px] leading-none ${tone ?? 'text-foreground'}`}>{value}</div>
    </div>
  );
}

function SummaryRow({ stats, difficulty }: { stats: GameStats; difficulty?: number | null }) {
  return (
    <div className="grid grid-cols-2 border-y border-border md:grid-cols-3">
      <div className="border-r border-b border-border md:border-b-0">
        <StatBlock label="最终得分" value={stats.score} />
      </div>
      <div className="border-b border-border md:border-r md:border-b-0">
        <StatBlock label="准确率" value={`${stats.accuracy.toFixed(1)}%`} />
      </div>
      <div className="border-r border-border md:border-r-0">
        <StatBlock label="速度 · CPM" value={Math.floor(stats.speed)} />
      </div>
      <div className="border-t border-r border-border">
        <StatBlock label="最长连击" value={stats.longestStreak} />
      </div>
      <div className="border-t border-border md:border-r">
        <StatBlock label="消灭怪物" value={stats.monstersKilled} />
      </div>
      <div>
        <StatBlock
          label={difficulty != null ? '难度 Dt' : '轮次'}
          value={difficulty != null ? Number(difficulty).toFixed(2) : stats.round}
        />
      </div>
    </div>
  );
}

function CharacterRanking({ title, note, stats, mode }: { title: string; note: string; stats: CharacterStat[]; mode: 'errors' | 'accuracy' }) {
  const emptyText = mode === 'errors' ? '这一局没有错字——下次可以更快一点。' : '样本不足：每个字符至少输入 5 次才会入榜。';

  return (
    <section className="border-t border-border">
      <div className="flex flex-wrap items-baseline justify-between gap-2 pt-6 pb-3">
        <h2 className="font-serif text-xl leading-none">{title}</h2>
        <span className="label-mono text-muted-foreground">{note}</span>
      </div>
      {stats.length > 0 ? (
        <ol className="border-t border-border">
          {stats.map((stat, index) => {
            const totalCount = stat.correctCount + stat.errorCount;
            const accuracy = stat.accuracy * 100;
            return (
              <li
                key={stat.char}
                className="grid grid-cols-[3rem_1fr_auto] items-center gap-4 border-b border-border py-3"
              >
                <span className="mono-num text-[12px] text-muted-foreground">{String(index + 1).padStart(2, '0')}</span>
                <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1">
                  <span className="font-serif text-xl leading-none">{stat.char === ' ' ? '空格' : stat.char}</span>
                  <span className="mono-num text-[12px] text-muted-foreground">
                    准确率 {accuracy.toFixed(1)}% · {stat.correctCount}/{totalCount} · 错 {stat.errorCount}
                  </span>
                </div>
                <span
                  className={`mono-num text-[13px] ${
                    mode === 'errors' ? 'text-accent' : accuracy < 80 ? 'text-accent' : 'text-muted-foreground'
                  }`}
                >
                  {mode === 'errors' ? `${stat.errorCount} 次` : `${accuracy.toFixed(0)}%`}
                </span>
              </li>
            );
          })}
        </ol>
      ) : (
        <p className="border-t border-border py-8 text-[13px] text-muted-foreground">{emptyText}</p>
      )}
    </section>
  );
}

export function GameResultView({
  stats,
  characterStats,
  uploadResult,
  showDetailedResult,
  onShowDetails,
  onBackToSummary,
  onRestart,
  onBackToPrepare,
}: GameResultViewProps) {
  const difficulty = uploadResult?.currentDifficulty != null ? Number(uploadResult.currentDifficulty) : null;

  const diagnosis = buildReviewDiagnosis(stats, characterStats);

  const errorRanking = [...characterStats]
    .filter((stat) => stat.errorCount > 0)
    .sort((a, b) => b.errorCount - a.errorCount)
    .slice(0, 10);

  const accuracyRanking = [...characterStats]
    .filter((stat) => stat.correctCount + stat.errorCount >= 5)
    .sort((a, b) => a.accuracy - b.accuracy)
    .slice(0, 10);

  if (!showDetailedResult) {
    return (
      <div className="container max-w-4xl py-12">
        <div className="flex items-center gap-3">
          <span className="status-light" />
          <span className="label-mono text-muted-foreground">对局结束 · SUMMARY</span>
        </div>
        <h1 className="mt-5 font-serif text-[44px] leading-[1.1] md:text-[56px]">这一局，写完了。</h1>
        <p className="mt-4 max-w-xl text-[14px] leading-7 text-muted-foreground">
          {MODE_LABEL[stats.mode]} · 共 {stats.round} 轮 · 输入 {stats.totalChars} 个字符。数据已经归档，下一局的字表会照着你这一局的错字重排。
        </p>

        <div className="mt-8">
          <SummaryRow stats={stats} difficulty={difficulty} />
        </div>

        <section className="mt-8 border border-border">
          <div className="flex flex-wrap items-baseline justify-between gap-2 border-b border-border px-5 py-3">
            <span className="label-mono text-muted-foreground">复盘 · REVIEW</span>
            <span className="label-mono text-muted-foreground">NEXT ROUND</span>
          </div>
          <div className="px-5 py-5">
            <h2 className="font-serif text-[22px] leading-snug">{diagnosis.headline}</h2>
            <p className="mt-2 max-w-2xl text-[13px] leading-6 text-muted-foreground">{diagnosis.detail}</p>
            {diagnosis.focusChars.length > 0 ? (
              <div className="mt-4 flex flex-wrap items-center gap-2">
                <span className="label-mono text-muted-foreground">下一轮重点</span>
                {diagnosis.focusChars.map((char) => (
                  <span key={char} className="keycap text-[12px] text-accent">
                    {displayChar(char)}
                  </span>
                ))}
              </div>
            ) : null}
          </div>
        </section>

        <div className="mt-8 flex flex-wrap items-center gap-3">
          <Button onClick={onRestart}>再来一局</Button>
          <Button variant="outline" onClick={onShowDetails}>
            查看详细结果
          </Button>
          <Button variant="ghost" onClick={onBackToPrepare}>
            返回配置
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="container max-w-6xl py-12">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b border-border pb-4">
        <div>
          <span className="label-mono text-muted-foreground">对局复盘 · DETAIL</span>
          <h1 className="mt-3 font-serif text-[36px] leading-none md:text-[44px]">详细结果</h1>
        </div>
        <Button variant="outline" size="sm" onClick={onBackToSummary}>
          返回概要
        </Button>
      </div>

      <div className="mt-6">
        <SummaryRow stats={stats} difficulty={difficulty} />
      </div>

      <div className="mt-4 grid grid-cols-2 gap-x-8 border-y border-border py-1 md:grid-cols-4">
        <StatBlock label="轮次" value={stats.round} />
        <StatBlock label="总字符" value={stats.totalChars} />
        <StatBlock label="正确字符" value={stats.correctChars} />
        <StatBlock label="错误字符" value={stats.errorChars} tone="text-accent" />
      </div>

      <CharacterRanking title="易错字符 Top 10" note="按错误次数排序" stats={errorRanking} mode="errors" />
      <CharacterRanking title="准确率最低 Top 10" note="仅统计输入 ≥ 5 次的字符" stats={accuracyRanking} mode="accuracy" />

      <section className="border-t border-border pt-6">
        <div className="flex flex-wrap items-baseline justify-between gap-2 pb-3">
          <h2 className="font-serif text-xl leading-none">字符错误分布</h2>
          <span className="label-mono text-muted-foreground">DISTRIBUTION</span>
        </div>
        <div className="border border-border p-4">
          <ErrorDistributionChart characterStats={characterStats} />
        </div>
      </section>

      <div className="mt-10 flex flex-wrap items-center gap-3 border-t border-border pt-6">
        <Button onClick={onRestart}>再来一局</Button>
        <Button variant="outline" onClick={onBackToPrepare}>
          返回配置
        </Button>
      </div>
    </div>
  );
}
