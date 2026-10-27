// 字符错误分布柱状图（纸面配色：柱色与轴色走具体色值，
// 因为 SVG 的 fill 呈现属性不解析 CSS 变量，用 hsl(var(--x)) 会画不出来）
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { CharacterStat } from '@/game/types';

interface ErrorDistributionChartProps {
  characterStats: CharacterStat[];
}

const INK = {
  line: '#d8d0bf',
  grid: '#e7e1d3',
  text: '#6e6659',
  ink: '#17150f',
  paper: '#f7f4ed',
  vermilion: '#c8503f',
  ochre: '#b08a45',
  teal: '#5c8a80',
};

export default function ErrorDistributionChart({ characterStats }: ErrorDistributionChartProps) {
  const errorData = characterStats
    .filter((stat) => stat.errorCount > 0)
    .sort((a, b) => b.errorCount - a.errorCount)
    .slice(0, 10)
    .map((stat) => ({
      char: stat.char === ' ' ? '空格' : stat.char,
      errors: stat.errorCount,
      accuracy: stat.accuracy,
    }));

  if (errorData.length === 0) {
    return (
      <div className="flex h-56 items-center justify-center">
        <p className="text-[13px] text-muted-foreground">这一局没有错字，没有可统计的分布。</p>
      </div>
    );
  }

  const tone = (accuracy: number) => {
    if (accuracy < 0.5) return INK.vermilion;
    if (accuracy < 0.8) return INK.ochre;
    return INK.teal;
  };

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={errorData} margin={{ top: 16, right: 12, left: 0, bottom: 4 }}>
        <CartesianGrid strokeDasharray="2 4" stroke={INK.grid} vertical={false} />
        <XAxis
          dataKey="char"
          tickLine={false}
          axisLine={{ stroke: INK.line }}
          tick={{ fill: INK.text, fontSize: 12, fontFamily: 'ui-monospace, monospace' }}
        />
        <YAxis
          width={36}
          allowDecimals={false}
          tickLine={false}
          axisLine={false}
          tick={{ fill: INK.text, fontSize: 11 }}
        />
        <Tooltip
          cursor={{ fill: 'rgba(23, 21, 15, 0.04)' }}
          contentStyle={{
            backgroundColor: INK.paper,
            border: `1px solid ${INK.line}`,
            borderRadius: 2,
            color: INK.ink,
            fontSize: 12,
          }}
          labelStyle={{ color: INK.ink }}
          formatter={(value: number, name: string) => (name === 'errors' ? [value, '错误次数'] : [value, name])}
        />
        <Bar dataKey="errors" radius={0} isAnimationActive={false}>
          {errorData.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={tone(entry.accuracy)} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
