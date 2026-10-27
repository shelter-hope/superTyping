// 对局侧栏：只显示数据，不放控制按钮（控制在页面顶栏）
import type { GameStats, GameState } from '@/game/types';

interface GameUIProps {
  stats: GameStats;
  gameState: GameState;
}

const MODE_LABEL: Record<GameStats['mode'], string> = {
  ENDLESS: '无尽适应',
  TIMED: '限时挑战',
  SURVIVAL: '生存模式',
};

const STATE_LABEL: Record<GameState, string> = {
  READY: '待开始',
  PLAYING: '进行中',
  PAUSED: '已暂停',
  GAME_OVER: '已结束',
};

function Metric({
  label,
  value,
  unit,
  sub,
}: {
  label: string;
  value: string | number;
  unit?: string;
  sub?: string;
}) {
  return (
    <div className="px-5 py-4">
      <div className="label-mono text-muted-foreground">{label}</div>
      <div className="mt-1.5 flex items-baseline gap-1">
        <span className="mono-num font-serif text-[30px] leading-none text-foreground">{value}</span>
        {unit ? <span className="label-mono text-muted-foreground">{unit}</span> : null}
      </div>
      {sub ? <div className="label-mono mt-2 text-muted-foreground">{sub}</div> : null}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="flex items-baseline justify-between gap-3 py-2">
      <span className="text-[13px] text-muted-foreground">{label}</span>
      <span className="mono-num text-[13px] text-foreground">{value}</span>
    </div>
  );
}

export default function GameUI({ stats, gameState }: GameUIProps) {
  const timer =
    stats.mode === 'TIMED' && stats.timeRemaining !== undefined
      ? `剩余 ${Math.max(0, Math.floor(stats.timeRemaining))} 秒`
      : stats.mode === 'SURVIVAL' && stats.survivalTime !== undefined
        ? `生存 ${Math.floor(stats.survivalTime)} 秒`
        : null;

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-border px-5 py-4">
        <div className="flex items-center justify-between gap-3">
          <span className="label-mono text-muted-foreground">LIVE · {STATE_LABEL[gameState]}</span>
          <span className="label-mono text-muted-foreground">第 {stats.round} 轮</span>
        </div>
        <div className="mt-2 flex items-baseline justify-between gap-3">
          <span className="font-serif text-xl leading-none">{MODE_LABEL[stats.mode]}</span>
          {timer ? <span className="mono-num text-[13px] text-accent">{timer}</span> : null}
        </div>
      </div>

      <div className="grid grid-cols-2 border-b border-border">
        <div className="border-r border-border">
          <Metric label="得分 · SCORE" value={stats.score} />
        </div>
        <Metric label="准确率 · ACC" value={stats.accuracy.toFixed(1)} unit="%" />
        <div className="border-t border-r border-border">
          <Metric label="速度 · SPEED" value={Math.floor(stats.speed)} unit="cpm" />
        </div>
        <div className="border-t border-border">
          <Metric label="连击 · COMBO" value={stats.currentStreak} sub={`最高 ${stats.longestStreak}`} />
        </div>
      </div>

      <div className="border-b border-border px-5 py-4">
        <div className="label-mono mb-1 text-muted-foreground">明细 · DETAIL</div>
        <div className="divide-y divide-border/70">
          <Row label="总字符" value={stats.totalChars} />
          <Row label="正确" value={stats.correctChars} />
          <Row label="错误" value={stats.errorChars} />
          <Row label="消灭怪物" value={stats.monstersKilled} />
          <Row label="峰值速度" value={Math.floor(stats.peakSpeed)} />
        </div>
      </div>

      <div className="px-5 py-4">
        <div className="label-mono mb-2 text-muted-foreground">操作 · KEYS</div>
        <div className="space-y-2 text-[12px] text-muted-foreground">
          <div className="flex items-center gap-2">
            <span className="keycap">A–Z</span>
            <span>输入首字符即锁定目标</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="keycap">Tab</span>
            <span>取消锁定，重新选最近的</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="keycap">Shift</span>
            <span>切换中 / 英输入</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="keycap">Space</span>
            <span>发射中文输入缓冲</span>
          </div>
        </div>
      </div>
    </div>
  );
}
