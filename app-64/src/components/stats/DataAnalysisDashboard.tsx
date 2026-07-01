/**
 * 数据分析仪表板
 * 展示游戏数据分析结果，支持两条优化道路
 */

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import type { CharacterAnalysis, ModelAnalysis } from '@/game/DataAnalyzer';
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';

interface DataAnalysisDashboardProps {
  characterAnalysis: CharacterAnalysis;
  modelAnalysis: ModelAnalysis;
  className?: string;
}

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

export function DataAnalysisDashboard({
  characterAnalysis,
  modelAnalysis,
  className,
}: DataAnalysisDashboardProps) {
  // 准备字符类型分布数据
  const charTypeData = Object.entries(characterAnalysis.charTypeDistribution).map(
    ([type, count]) => ({
      name: getCharTypeName(type),
      value: count,
    })
  );

  // 准备易错字符数据
  const errorCharData = characterAnalysis.topErrorChars.map(char => ({
    character: char.character,
    errorCount: char.errorCount,
    accuracy: char.accuracy,
  }));

  // 准备字符串长度分布数据
  const lengthDistData = Object.entries(
    characterAnalysis.stringLengthDistribution.distribution
  )
    .map(([length, count]) => ({
      length: parseInt(length),
      count,
    }))
    .sort((a, b) => a.length - b.length);

  // 准备难度趋势数据
  const difficultyTrendData = modelAnalysis.difficultyTrend.map((d, i) => ({
    round: i + 1,
    difficulty: d,
    accuracy: modelAnalysis.accuracyTrend[i],
    speed: modelAnalysis.speedTrend[i],
  }));

  return (
    <div className={className}>
      <Tabs defaultValue="path1" className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="path1">道路一：字符统计</TabsTrigger>
          <TabsTrigger value="path2">道路二：数学模型</TabsTrigger>
        </TabsList>

        {/* 道路一：基于字符统计的定向变异 */}
        <TabsContent value="path1" className="space-y-4">
          {/* 字符类型分布 */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">字符类型分布</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie
                    data={charTypeData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) =>
                      `${name} ${(percent * 100).toFixed(0)}%`
                    }
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {charTypeData.map((entry, index) => (
                      <Cell
                        key={`cell-${index}`}
                        fill={COLORS[index % COLORS.length]}
                      />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
              <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
                <div>
                  <span className="text-muted-foreground">总字符数：</span>
                  <span className="font-medium">
                    {characterAnalysis.totalCharacters}
                  </span>
                </div>
                <div>
                  <span className="text-muted-foreground">唯一字符：</span>
                  <span className="font-medium">
                    {characterAnalysis.uniqueCharacters}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 易错字符Top 10 */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">易错字符 Top 10</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={errorCharData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="character" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="errorCount" fill="#ef4444" name="错误次数" />
                </BarChart>
              </ResponsiveContainer>
              <div className="mt-4">
                <Badge variant="outline">
                  易错字符占比: {(characterAnalysis.errorCharRatio * 100).toFixed(1)}%
                </Badge>
              </div>
            </CardContent>
          </Card>

          {/* 字符串长度分布 */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">字符串长度分布</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={lengthDistData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="length" label={{ value: '长度', position: 'insideBottom', offset: -5 }} />
                  <YAxis label={{ value: '次数', angle: -90, position: 'insideLeft' }} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#3b82f6" />
                </BarChart>
              </ResponsiveContainer>
              <div className="mt-4 grid grid-cols-3 gap-2 text-sm">
                <div>
                  <span className="text-muted-foreground">平均：</span>
                  <span className="font-medium">
                    {characterAnalysis.stringLengthDistribution.avgLength.toFixed(1)}
                  </span>
                </div>
                <div>
                  <span className="text-muted-foreground">最小：</span>
                  <span className="font-medium">
                    {characterAnalysis.stringLengthDistribution.minLength.toFixed(0)}
                  </span>
                </div>
                <div>
                  <span className="text-muted-foreground">最大：</span>
                  <span className="font-medium">
                    {characterAnalysis.stringLengthDistribution.maxLength.toFixed(0)}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* 道路二：基于数学模型的自主调控 */}
        <TabsContent value="path2" className="space-y-4">
          {/* 综合统计 */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">综合统计</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">平均准确率</span>
                    <span className="text-sm font-medium">
                      {modelAnalysis.avgAccuracy.toFixed(1)}%
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">峰值准确率</span>
                    <span className="text-sm font-medium">
                      {modelAnalysis.peakAccuracy.toFixed(1)}%
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">平均速度</span>
                    <span className="text-sm font-medium">
                      {modelAnalysis.avgSpeed.toFixed(0)} 字符/分
                    </span>
                  </div>
                </div>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">峰值速度</span>
                    <span className="text-sm font-medium">
                      {modelAnalysis.peakSpeed.toFixed(0)} 字符/分
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">平均错误率</span>
                    <span className="text-sm font-medium">
                      {(modelAnalysis.avgCharErrorRate * 100).toFixed(1)}%
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">总轮次</span>
                    <span className="text-sm font-medium">
                      {modelAnalysis.roundCount}
                    </span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 难度系数变化趋势 */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">难度系数变化趋势</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={250}>
                <LineChart data={difficultyTrendData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="round" label={{ value: '轮次', position: 'insideBottom', offset: -5 }} />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="difficulty"
                    stroke="#8b5cf6"
                    name="难度系数"
                    strokeWidth={2}
                  />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          {/* 准确率和速度变化趋势 */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">准确率与速度变化</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={250}>
                <LineChart data={difficultyTrendData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="round" label={{ value: '轮次', position: 'insideBottom', offset: -5 }} />
                  <YAxis yAxisId="left" />
                  <YAxis yAxisId="right" orientation="right" />
                  <Tooltip />
                  <Legend />
                  <Line
                    yAxisId="left"
                    type="monotone"
                    dataKey="accuracy"
                    stroke="#10b981"
                    name="准确率(%)"
                    strokeWidth={2}
                  />
                  <Line
                    yAxisId="right"
                    type="monotone"
                    dataKey="speed"
                    stroke="#f59e0b"
                    name="速度(字符/分)"
                    strokeWidth={2}
                  />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

// 辅助函数：获取字符类型中文名称
function getCharTypeName(type: string): string {
  const names: Record<string, string> = {
    lowercase: '小写字母',
    uppercase: '大写字母',
    number: '数字',
    punctuation: '标点符号',
    special: '特殊字符',
    chinese: '中文字符',
  };
  return names[type] || type;
}
