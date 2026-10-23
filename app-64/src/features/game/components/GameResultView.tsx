import ErrorDistributionChart from '@/components/stats/ErrorDistributionChart';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { CharacterStat, GameStats } from '@/game/types';
import type { GameRecordUploadResult } from '@/db/api';

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

function StatBlock({ label, value, tone }: { label: string; value: string | number; tone?: string }) {
  return (
    <div>
      <div className="text-sm text-muted-foreground">{label}</div>
      <div className={`text-2xl font-bold ${tone ?? ''}`}>{value}</div>
    </div>
  );
}

function SummaryCards({ stats }: { stats: GameStats }) {
  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm text-muted-foreground">最终得分</CardTitle>
        </CardHeader>
        <CardContent><div className="text-3xl font-bold">{stats.score}</div></CardContent>
      </Card>
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm text-muted-foreground">准确率</CardTitle>
        </CardHeader>
        <CardContent><div className="text-3xl font-bold">{stats.accuracy.toFixed(1)}%</div></CardContent>
      </Card>
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm text-muted-foreground">打字速度</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-3xl font-bold">{Math.floor(stats.speed)}</div>
          <div className="text-xs text-muted-foreground">字符/分钟</div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm text-muted-foreground">最长连击</CardTitle>
        </CardHeader>
        <CardContent><div className="text-3xl font-bold">{stats.longestStreak}</div></CardContent>
      </Card>
    </div>
  );
}

function CharacterRanking({ title, stats, mode }: { title: string; stats: CharacterStat[]; mode: 'errors' | 'accuracy' }) {
  const emptyText = mode === 'errors' ? '太棒了！没有错误字符' : '暂无足够数据（需要至少输入5次）';

  return (
    <Card>
      <CardHeader><CardTitle>{title}</CardTitle></CardHeader>
      <CardContent>
        {stats.length > 0 ? (
          <div className="space-y-2">
            {stats.map((stat, index) => {
              const totalCount = stat.correctCount + stat.errorCount;
              const accuracy = stat.accuracy * 100;
              return (
                <div key={stat.char} className="flex flex-col gap-3 rounded-lg bg-muted p-3 md:flex-row md:items-center md:justify-between">
                  <div className="flex items-center gap-4">
                    <div className="w-8 text-lg font-bold text-muted-foreground">#{index + 1}</div>
                    <div className="font-mono text-2xl font-bold">{stat.char}</div>
                  </div>
                  <div className="grid grid-cols-3 gap-4 text-right md:flex md:items-center md:gap-6">
                    <div>
                      <div className="text-sm text-muted-foreground">准确率</div>
                      <div className={`text-xl font-bold ${accuracy < 50 ? 'text-red-500' : accuracy < 80 ? 'text-orange-500' : ''}`}>{accuracy.toFixed(1)}%</div>
                    </div>
                    <div>
                      <div className="text-sm text-muted-foreground">正确/总数</div>
                      <div className="text-xl font-bold">{stat.correctCount}/{totalCount}</div>
                    </div>
                    <div>
                      <div className="text-sm text-muted-foreground">错误次数</div>
                      <div className="text-xl font-bold text-red-500">{stat.errorCount}</div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="py-8 text-center text-muted-foreground">{emptyText}</div>
        )}
      </CardContent>
    </Card>
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
      <div className="container max-w-4xl py-8">
        <Card className="p-8">
          <div className="space-y-6 text-center">
            <h1 className="text-3xl font-bold text-accent">游戏结束</h1>
            <div className="grid grid-cols-2 gap-4 text-left">
              <StatBlock label="最终得分" value={stats.score} />
              <StatBlock label="轮次" value={stats.round} />
              <StatBlock label="准确率" value={`${stats.accuracy.toFixed(1)}%`} />
              <StatBlock label="速度" value={Math.floor(stats.speed)} />
              <StatBlock label="消灭怪兽" value={stats.monstersKilled} />
              <StatBlock label="最长连击" value={stats.longestStreak} />
              {uploadResult?.currentDifficulty != null && (
                <StatBlock label="当前难度 Dt" value={Number(uploadResult.currentDifficulty).toFixed(2)} />
              )}
            </div>
            <div className="flex flex-wrap justify-center gap-4">
              <Button onClick={onShowDetails}>查看详细结果</Button>
              <Button variant="outline" onClick={onRestart}>重新开始</Button>
              <Button variant="ghost" onClick={onBackToPrepare}>返回配置</Button>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="container max-w-6xl py-8">
      <Card className="p-8">
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <h1 className="text-3xl font-bold gradient-text">详细结果</h1>
            <Button variant="outline" onClick={onBackToSummary}>返回</Button>
          </div>
          <SummaryCards stats={stats} />
          <div className="grid grid-cols-2 gap-4 md:grid-cols-5">
            <StatBlock label="轮次" value={stats.round} />
            <StatBlock label="消灭怪兽" value={stats.monstersKilled} />
            <StatBlock label="总字符数" value={stats.totalChars} />
            <StatBlock label="错误字符" value={stats.errorChars} tone="text-red-500" />
            {uploadResult?.currentDifficulty != null && (
              <StatBlock label="当前难度 Dt" value={Number(uploadResult.currentDifficulty).toFixed(2)} />
            )}
          </div>
          <CharacterRanking title="易错字符排行 Top 10" stats={errorRanking} mode="errors" />
          <CharacterRanking title="准确率最低字符 Top 10" stats={accuracyRanking} mode="accuracy" />
          <Card>
            <CardHeader><CardTitle>字符错误分布图</CardTitle></CardHeader>
            <CardContent><ErrorDistributionChart characterStats={characterStats} /></CardContent>
          </Card>
          <div className="flex justify-center gap-4">
            <Button onClick={onRestart}>重新开始</Button>
            <Button variant="outline" onClick={onBackToPrepare}>返回配置</Button>
          </div>
        </div>
      </Card>
    </div>
  );
}
