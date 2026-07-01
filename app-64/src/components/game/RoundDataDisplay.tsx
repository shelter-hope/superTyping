/**
 * 实时轮次数据展示组件
 * 在游戏进行中实时显示每轮数据
 */

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { RoundStats } from '@/types/database';
import { TrendingUp, TrendingDown, Target, Zap, Hash, Activity } from 'lucide-react';

interface RoundDataDisplayProps {
  rounds: RoundStats[];
  currentRound: number;
  className?: string;
}

export function RoundDataDisplay({ rounds, currentRound, className }: RoundDataDisplayProps) {
  if (rounds.length === 0) {
    return (
      <Card className={className}>
        <CardHeader>
          <CardTitle className="text-lg">轮次数据</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">暂无轮次数据</p>
        </CardContent>
      </Card>
    );
  }

  const latestRound = rounds[rounds.length - 1];
  const prevRound = rounds.length > 1 ? rounds[rounds.length - 2] : null;

  // 计算趋势
  const accuracyTrend = prevRound
    ? latestRound.accuracy - prevRound.accuracy
    : 0;
  const speedTrend = prevRound ? latestRound.speed - prevRound.speed : 0;

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">第 {currentRound} 轮</CardTitle>
          <Badge variant="outline">
            难度系数: {latestRound.difficultyCoefficient.toFixed(2)}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* 得分 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Target className="h-4 w-4 text-primary" />
            <span className="text-sm font-medium">得分</span>
          </div>
          <span className="text-lg font-bold">{latestRound.score}</span>
        </div>

        {/* 准确率 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Activity className="h-4 w-4 text-green-500" />
            <span className="text-sm font-medium">准确率</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-lg font-bold">
              {latestRound.accuracy.toFixed(1)}%
            </span>
            {prevRound && (
              <span
                className={`text-xs flex items-center ${
                  accuracyTrend >= 0 ? 'text-green-500' : 'text-red-500'
                }`}
              >
                {accuracyTrend >= 0 ? (
                  <TrendingUp className="h-3 w-3" />
                ) : (
                  <TrendingDown className="h-3 w-3" />
                )}
                {Math.abs(accuracyTrend).toFixed(1)}%
              </span>
            )}
          </div>
        </div>

        {/* 速度 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Zap className="h-4 w-4 text-yellow-500" />
            <span className="text-sm font-medium">速度</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-lg font-bold">
              {latestRound.speed.toFixed(0)} 字符/分
            </span>
            {prevRound && (
              <span
                className={`text-xs flex items-center ${
                  speedTrend >= 0 ? 'text-green-500' : 'text-red-500'
                }`}
              >
                {speedTrend >= 0 ? (
                  <TrendingUp className="h-3 w-3" />
                ) : (
                  <TrendingDown className="h-3 w-3" />
                )}
                {Math.abs(speedTrend).toFixed(0)}
              </span>
            )}
          </div>
        </div>

        {/* 怪物统计 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Hash className="h-4 w-4 text-purple-500" />
            <span className="text-sm font-medium">怪物</span>
          </div>
          <span className="text-sm">
            {latestRound.monstersDestroyed} / {latestRound.monsterCount}
          </span>
        </div>

        {/* 平均字符串长度 */}
        <div className="flex items-center justify-between">
          <span className="text-sm text-muted-foreground">平均长度</span>
          <span className="text-sm font-medium">
            {latestRound.avgStringLength.toFixed(1)} 字符
          </span>
        </div>

        {/* 历史轮次概览 */}
        {rounds.length > 1 && (
          <div className="pt-4 border-t">
            <div className="text-xs text-muted-foreground mb-2">
              历史 {rounds.length} 轮
            </div>
            <div className="grid grid-cols-3 gap-2 text-xs">
              <div>
                <div className="text-muted-foreground">平均准确率</div>
                <div className="font-medium">
                  {(
                    rounds.reduce((sum, r) => sum + r.accuracy, 0) /
                    rounds.length
                  ).toFixed(1)}
                  %
                </div>
              </div>
              <div>
                <div className="text-muted-foreground">平均速度</div>
                <div className="font-medium">
                  {(
                    rounds.reduce((sum, r) => sum + r.speed, 0) / rounds.length
                  ).toFixed(0)}
                </div>
              </div>
              <div>
                <div className="text-muted-foreground">总得分</div>
                <div className="font-medium">
                  {rounds.reduce((sum, r) => sum + r.score, 0)}
                </div>
              </div>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
