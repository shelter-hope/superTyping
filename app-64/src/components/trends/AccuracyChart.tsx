// 打字准确率趋势图组件
// 注意：recharts 的 fill/stroke 是 SVG 呈现属性，不解析 hsl(var(--x))，这里用具体色值。
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

const INK = '#17150f';
const VERMILION = '#ce3a2a';
const MUTED = '#6e6659';
const GRID = '#e3dccd';
const PAPER = '#f7f4ed';

interface AccuracyChartProps {
  data: Array<{
    date: string;
    avgAccuracy: number;
    peakAccuracy: number;
  }>;
}

export default function AccuracyChart({ data }: AccuracyChartProps) {
  return (
    <Card>
      <CardHeader className="flex-row items-baseline justify-between border-b border-border">
        <CardTitle className="font-serif text-lg">准确率趋势</CardTitle>
        <span className="label-mono text-muted-foreground">ACCURACY · %</span>
      </CardHeader>
      <CardContent className="pt-6">
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={data} margin={{ top: 4, right: 8, bottom: 0, left: 0 }}>
            <CartesianGrid strokeDasharray="2 4" stroke={GRID} vertical={false} />
            <XAxis
              dataKey="date"
              tick={{ fontSize: 10, fill: MUTED, fontFamily: 'JetBrains Mono, monospace' }}
              axisLine={{ stroke: GRID }}
              tickLine={false}
              angle={-35}
              textAnchor="end"
              height={54}
            />
            <YAxis
              domain={[0, 100]}
              tick={{ fontSize: 10, fill: MUTED, fontFamily: 'JetBrains Mono, monospace' }}
              axisLine={false}
              tickLine={false}
              width={38}
            />
            <Tooltip
              cursor={{ stroke: GRID }}
              contentStyle={{
                backgroundColor: PAPER,
                border: `1px solid ${GRID}`,
                borderRadius: 2,
                fontSize: 12,
                boxShadow: 'none',
              }}
              formatter={(value: number) => `${value.toFixed(1)}%`}
            />
            <Legend wrapperStyle={{ fontSize: 11 }} />
            <Line
              type="monotone"
              dataKey="avgAccuracy"
              stroke={INK}
              name="平均"
              strokeWidth={1.5}
              dot={{ r: 2.5, fill: PAPER, strokeWidth: 1.5 }}
              activeDot={{ r: 4 }}
              isAnimationActive={false}
            />
            <Line
              type="monotone"
              dataKey="peakAccuracy"
              stroke={VERMILION}
              name="峰值"
              strokeWidth={1.5}
              strokeDasharray="4 3"
              dot={{ r: 2.5, fill: PAPER, strokeWidth: 1.5 }}
              activeDot={{ r: 4 }}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
