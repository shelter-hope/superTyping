// 字符错误分布柱状图
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import type { CharacterStat } from '@/game/types';

interface ErrorDistributionChartProps {
  characterStats: CharacterStat[];
}

export default function ErrorDistributionChart({ characterStats }: ErrorDistributionChartProps) {
  // 只显示有错误的字符，按错误次数排序，取前10个
  const errorData = characterStats
    .filter(stat => stat.errorCount > 0)
    .sort((a, b) => b.errorCount - a.errorCount)
    .slice(0, 10)
    .map(stat => ({
      char: stat.char,
      errors: stat.errorCount,
      accuracy: stat.accuracy,
    }));

  if (errorData.length === 0) {
    return (
      <div className="flex items-center justify-center h-64 text-muted-foreground">
        <div className="text-center">
          <div className="text-4xl mb-2">🎉</div>
          <div>太棒了！没有错误字符</div>
        </div>
      </div>
    );
  }

  // 根据准确率设置颜色
  const getColor = (accuracy: number) => {
    if (accuracy < 50) return 'hsl(var(--destructive))';
    if (accuracy < 80) return 'hsl(var(--amber))';
    return 'hsl(var(--primary))';
  };

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={errorData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
        <XAxis 
          dataKey="char" 
          stroke="hsl(var(--foreground))"
          style={{ fontSize: '14px', fontFamily: 'monospace' }}
        />
        <YAxis 
          stroke="hsl(var(--foreground))"
          style={{ fontSize: '12px' }}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: 'hsl(var(--card))',
            border: '1px solid hsl(var(--border))',
            borderRadius: '8px',
            color: 'hsl(var(--foreground))',
          }}
          formatter={(value: number, name: string) => {
            if (name === 'errors') return [value, '错误次数'];
            return [value, name];
          }}
        />
        <Bar dataKey="errors" radius={[8, 8, 0, 0]}>
          {errorData.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={getColor(entry.accuracy)} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
