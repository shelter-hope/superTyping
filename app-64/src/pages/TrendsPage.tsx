// 历史趋势页面 - 使用后端统计接口
import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { getTrendData, getErrorComparison, getTrainingSuggestions } from '@/db/api';
import { trendPointsToChartData } from '@/utils/trendData';
import type { ChartDataPoint } from '@/utils/trendData';
import { Loader2, Lightbulb } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import SpeedChart from '@/components/trends/SpeedChart';
import AccuracyChart from '@/components/trends/AccuracyChart';
import ErrorComparison from '@/components/trends/ErrorComparison';

type TimeRange = 'all' | 'year' | 'month' | 'week' | 'three-days';

/** 图表所需格式（与 utils/trendData ChartDataPoint 一致） */
type ChartData = ChartDataPoint;

/** 训练建议（与后端 TrainingSuggestionDTO 对应） */
interface TrainingSuggestion {
  focusChars?: Array<{ character: string; errorCount: number; accuracy?: number; charType?: string }>;
  suggestedMode?: string;
  summary?: string;
  recentAccuracy?: number;
  recentSpeed?: number;
}

export default function TrendsPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [timeRange, setTimeRange] = useState<TimeRange>('week');
  const [chartData, setChartData] = useState<ChartData[]>([]);
  const [thisWeekErrors, setThisWeekErrors] = useState(0);
  const [lastWeekErrors, setLastWeekErrors] = useState(0);
  const [trainingSuggestion, setTrainingSuggestion] = useState<TrainingSuggestion | null>(null);

  const userId = user?.userId;

  useEffect(() => {
    if (userId != null) {
      loadData(timeRange);
      loadErrorComparison();
      loadTrainingSuggestion();
    }
  }, [userId, timeRange]);

  async function loadData(range: TimeRange) {
    if (userId == null) return;
    setLoading(true);
    try {
      const apiTimeRange = range === 'three-days' ? '3days' : range;
      const list = await getTrendData(userId, apiTimeRange as 'week' | 'month' | 'year' | 'all' | '3days');
      const data = trendPointsToChartData(list ?? undefined);
      setChartData(data);
    } catch (error) {
      console.error('加载趋势数据失败:', error);
      setChartData([]);
    } finally {
      setLoading(false);
    }
  }

  async function loadErrorComparison() {
    if (userId == null) return;
    try {
      const res = await getErrorComparison(userId);
      if (res) {
        setThisWeekErrors(res.thisWeekErrors ?? 0);
        setLastWeekErrors(res.lastWeekErrors ?? 0);
      }
    } catch (error) {
      console.error('加载错误对比失败:', error);
    }
  }

  async function loadTrainingSuggestion() {
    if (userId == null) return;
    try {
      const res = await getTrainingSuggestions(userId);
      setTrainingSuggestion(res ?? null);
    } catch (error) {
      console.error('加载训练建议失败:', error);
      setTrainingSuggestion(null);
    }
  }

  if (userId == null) {
    return (
      <div className="container max-w-7xl py-8 text-center text-muted-foreground">
        请先登录后查看趋势分析
      </div>
    );
  }

  if (loading && chartData.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-4rem)]">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="container max-w-7xl py-8">
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold gradient-text">历史趋势分析</h1>
          <p className="text-muted-foreground mt-2">
            查看你的打字能力随时间的变化趋势
          </p>
        </div>

        {/* 训练建议卡片 */}
        {trainingSuggestion && (
          <div className="rounded-lg border bg-card p-6">
            <div className="flex items-center gap-2 mb-4">
              <Lightbulb className="w-5 h-5 text-primary" />
              <h2 className="text-xl font-semibold">针对性训练建议</h2>
            </div>
            <p className="text-muted-foreground mb-4">{trainingSuggestion.summary}</p>
            {trainingSuggestion.focusChars && trainingSuggestion.focusChars.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {trainingSuggestion.focusChars.slice(0, 10).map((item, i) => (
                  <span
                    key={i}
                    className="inline-flex items-center gap-1 px-3 py-1.5 rounded-md bg-muted text-sm"
                  >
                    <span className="font-mono font-bold">{item.character}</span>
                    <span className="text-muted-foreground">错误 {item.errorCount} · 准确率 {item.accuracy != null ? item.accuracy.toFixed(0) : '-'}%</span>
                  </span>
                ))}
              </div>
            )}
            {(trainingSuggestion.recentAccuracy != null || trainingSuggestion.recentSpeed != null) && (
              <p className="text-sm text-muted-foreground mt-4">
                近期准确率 {trainingSuggestion.recentAccuracy?.toFixed(1) ?? '-'}% · 近期速度 {trainingSuggestion.recentSpeed?.toFixed(0) ?? '-'} 字符/分钟
              </p>
            )}
          </div>
        )}

        <Tabs value={timeRange} onValueChange={(v) => setTimeRange(v as TimeRange)}>
          <TabsList className="grid w-full grid-cols-5">
            <TabsTrigger value="three-days">近三天</TabsTrigger>
            <TabsTrigger value="week">近一周</TabsTrigger>
            <TabsTrigger value="month">近一月</TabsTrigger>
            <TabsTrigger value="year">近一年</TabsTrigger>
            <TabsTrigger value="all">全部</TabsTrigger>
          </TabsList>

          <TabsContent value={timeRange} className="space-y-6 mt-6">
            {chartData.length === 0 ? (
              <div className="text-center py-12 text-muted-foreground">
                该时间范围内暂无数据
              </div>
            ) : (
              <>
                <SpeedChart data={chartData} />
                <AccuracyChart data={chartData} />
                <ErrorComparison
                  thisWeekErrors={thisWeekErrors}
                  lastWeekErrors={lastWeekErrors}
                />
              </>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
