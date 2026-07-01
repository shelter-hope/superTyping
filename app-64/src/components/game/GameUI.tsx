// 游戏UI组件
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import type { GameStats, GameState } from '@/game/types';
import { Pause, Play, Volume2, VolumeX, Trophy, Target, Zap, TrendingUp, Maximize, Minimize } from 'lucide-react';

interface GameUIProps {
  stats: GameStats;
  gameState: GameState;
  soundEnabled: boolean;
  isFullscreen: boolean;
  onPause: () => void;
  onResume: () => void;
  onToggleSound: () => void;
  onToggleFullscreen: () => void;
}

export default function GameUI({
  stats,
  gameState,
  soundEnabled,
  isFullscreen,
  onPause,
  onResume,
  onToggleSound,
  onToggleFullscreen,
}: GameUIProps) {
  return (
    <div className="space-y-4">
      {/* 顶部控制栏 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          {gameState === 'PLAYING' ? (
            <Button onClick={onPause} variant="outline" size="sm">
              <Pause className="w-4 h-4 mr-2" />
              暂停
            </Button>
          ) : gameState === 'PAUSED' ? (
            <Button onClick={onResume} variant="outline" size="sm">
              <Play className="w-4 h-4 mr-2" />
              继续
            </Button>
          ) : null}

          <Button onClick={onToggleSound} variant="outline" size="sm">
            {soundEnabled ? (
              <>
                <Volume2 className="w-4 h-4 mr-2" />
                音效开
              </>
            ) : (
              <>
                <VolumeX className="w-4 h-4 mr-2" />
                音效关
              </>
            )}
          </Button>

          <Button onClick={onToggleFullscreen} variant="outline" size="sm">
            {isFullscreen ? (
              <>
                <Minimize className="w-4 h-4 mr-2" />
                退出全屏
              </>
            ) : (
              <>
                <Maximize className="w-4 h-4 mr-2" />
                全屏
              </>
            )}
          </Button>
        </div>

        <div className="flex items-center gap-4">
          {/* 游戏模式名称 - 大字体显示 */}
          <div className="text-2xl font-bold text-primary">
            {stats.mode === 'ENDLESS' && '无尽适应'}
            {stats.mode === 'TIMED' && '限时挑战'}
            {stats.mode === 'SURVIVAL' && '生存模式'}
          </div>
          
          {/* 轮次显示 */}
          <div className="text-sm text-muted-foreground">
            轮次: <span className="font-bold text-foreground">{stats.round}</span>
          </div>
          
          {/* 限时模式：剩余时间 */}
          {stats.mode === 'TIMED' && stats.timeRemaining !== undefined && (
            <div className="text-sm">
              <span className="text-muted-foreground">剩余: </span>
              <span className={`font-bold ${stats.timeRemaining < 30 ? 'text-red-500' : 'text-primary'}`}>
                {Math.floor(stats.timeRemaining)}秒
              </span>
            </div>
          )}
          
          {/* 生存模式：生存时间 */}
          {stats.mode === 'SURVIVAL' && stats.survivalTime !== undefined && (
            <div className="text-sm">
              <span className="text-muted-foreground">生存: </span>
              <span className="font-bold text-primary">
                {Math.floor(stats.survivalTime)}秒
              </span>
            </div>
          )}
        </div>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {/* 得分 */}
        <Card className="p-4">
          <div className="flex items-center gap-2 mb-2">
            <Trophy className="w-5 h-5 text-yellow-500" />
            <span className="text-sm text-muted-foreground">得分</span>
          </div>
          <div className="text-2xl font-bold">{stats.score}</div>
        </Card>

        {/* 准确率 */}
        <Card className="p-4">
          <div className="flex items-center gap-2 mb-2">
            <Target className="w-5 h-5 text-green-500" />
            <span className="text-sm text-muted-foreground">准确率</span>
          </div>
          <div className="text-2xl font-bold">{stats.accuracy.toFixed(1)}%</div>
          <Progress value={stats.accuracy} className="mt-2 h-2" />
        </Card>

        {/* 速度 */}
        <Card className="p-4">
          <div className="flex items-center gap-2 mb-2">
            <TrendingUp className="w-5 h-5 text-blue-500" />
            <span className="text-sm text-muted-foreground">速度</span>
          </div>
          <div className="text-2xl font-bold">{Math.floor(stats.speed)}</div>
          <div className="text-xs text-muted-foreground">字符/分钟</div>
        </Card>

        {/* 连击 */}
        <Card className="p-4">
          <div className="flex items-center gap-2 mb-2">
            <Zap className="w-5 h-5 text-orange-500" />
            <span className="text-sm text-muted-foreground">连击</span>
          </div>
          <div className="text-2xl font-bold">{stats.currentStreak}</div>
          <div className="text-xs text-muted-foreground">
            最高: {stats.longestStreak}
          </div>
        </Card>
      </div>

      {/* 详细统计 */}
      <Card className="p-4">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div>
            <div className="text-muted-foreground">总字符</div>
            <div className="font-bold">{stats.totalChars}</div>
          </div>
          <div>
            <div className="text-muted-foreground">正确字符</div>
            <div className="font-bold text-green-500">{stats.correctChars}</div>
          </div>
          <div>
            <div className="text-muted-foreground">错误字符</div>
            <div className="font-bold text-red-500">{stats.errorChars}</div>
          </div>
          <div>
            <div className="text-muted-foreground">消灭怪兽</div>
            <div className="font-bold text-primary">{stats.monstersKilled}</div>
          </div>
        </div>
      </Card>

      {/* 游戏提示 */}
      <Card className="p-4 bg-muted/30">
        <div className="text-sm space-y-1">
          <div className="font-bold mb-2">操作提示：</div>
          <div>• 输入字符攻击怪兽</div>
          <div>• Tab键取消锁定</div>
          <div>• Shift键切换中英文</div>
          <div>• 空格/Ctrl发射中文输入</div>
        </div>
      </Card>
    </div>
  );
}
