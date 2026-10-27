// 历史趋势页面 - 使用后端统计接口
import { useEffect, useState } from 'react';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/AuthContext';
import { getErrorComparison, getTrainingSuggestions, getTrendData } from '@/db/api';
import { trendPointsToChartData } from '@/utils/trendData';
import type { ChartDataPoint } from '@/utils/trendData';
import { formatNum } from '@/utils';
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

const RANGES: Array<{ value: TimeRange; label: string }> = [
  { value: 'three-days', label: '近三天' },
  { value: 'week', label: '近一周' },
  { value: 'month', label: '近一月' },
  { value: 'year', label: '近一年' },
  { value: 'all', label: '全部' },
];

/** 载入态：一个等宽小标签 + 一根扫光细线，不用转圈图标 */
function LoadingNote({ label }: { label: string }) {
  return (
    <div className="mt-10">
      <div className="flex items-center gap-3">
        <span className="label-mono text-[10px]">{label}</span>
        <span className="block h-[1px] w-24 overflow-hidden bg-border">
          <span className="block h-full w-1/3 animate-loading-sweep bg-foreground" />
        </span>
      </div>
      <div className="mt-8 border-t border-border">
        {[0, 1, 2].map((row) => (
          <div key={row} className="h-10 border-b border-border" />
        ))}
      </div>
    </div>
  );
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

  const suggestion = trainingSuggestion;
  const focusChars = suggestion?.focusChars?.slice(0, 10) ?? [];
  const hasSuggestion =
    suggestion != null &&
    (Boolean(suggestion.summary) ||
      focusChars.length > 0 ||
      suggestion.recentAccuracy != null ||
      suggestion.recentSpeed != null);
  const hasCharts = chartData.length > 0;
  const recentPoints = chartData.slice(-8).reverse();
  const currentRange = RANGES.find((range) => range.value === timeRange) ?? RANGES[1];

  return (
    <PageBody>
      <PageHeader
        eyebrow="统计 · TRENDS"
        title="历史趋势"
        description="速度、准确率与错字随时间的走势，按日或按周聚合，只统计你自己的对局。"
      />

      {userId == null ? (
        <p className="mt-10 border-t border-border pt-6 text-[13px] text-muted-foreground">
          请先登录后查看趋势分析
        </p>
      ) : loading && chartData.length === 0 ? (
        <LoadingNote label="载入趋势 · LOADING" />
      ) : (
        <>
          {suggestion && hasSuggestion ? (
            <section className="mt-10 border-t border-border pt-6">
              <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 pb-3">
                <div className="flex flex-wrap items-baseline gap-x-3">
                  <h2 className="font-serif text-xl leading-none">针对性训练建议</h2>
                  <span className="label-mono text-[9px]">SUGGESTION</span>
                </div>
                <span className="label-mono text-[9px]">依据近期对局</span>
              </div>

              <div className="grid gap-8 md:grid-cols-12">
                <div className="md:col-span-5">
                  {suggestion.summary ? (
                    <p className="pull-quote text-[13px] leading-[1.9] text-foreground/90">
                      {suggestion.summary}
                    </p>
                  ) : null}

                  {(suggestion.recentAccuracy != null || suggestion.recentSpeed != null) && (
                    <dl className="mt-6 grid grid-cols-2 border-t border-border">
                      <div className="border-r border-border pr-4 pt-4">
                        <dt className="label-mono text-[9px]">近期准确率</dt>
                        <dd className="mono-num mt-1.5 font-serif text-[28px] leading-none">
                          {suggestion.recentAccuracy?.toFixed(1) ?? '-'}
                          <span className="ml-1 text-[11px] text-muted-foreground">%</span>
                        </dd>
                      </div>
                      <div className="pl-4 pt-4">
                        <dt className="label-mono text-[9px]">近期速度</dt>
                        <dd className="mono-num mt-1.5 font-serif text-[28px] leading-none">
                          {suggestion.recentSpeed?.toFixed(0) ?? '-'}
                          <span className="ml-1 text-[11px] text-muted-foreground">字符/分</span>
                        </dd>
                      </div>
                    </dl>
                  )}
                </div>

                {focusChars.length > 0 ? (
                  <div className="md:col-span-7">
                    <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
                      <span className="label-mono text-[9px]">重点字符 · FOCUS</span>
                      <span className="label-mono text-[9px]">按错误次数取前 {focusChars.length} 个</span>
                    </div>
                    <div className="mt-3 grid grid-cols-2 border-l border-t border-border sm:grid-cols-5">
                      {focusChars.map((item, index) => (
                        <div
                          key={`${item.character}-${index}`}
                          className="border-b border-r border-border px-3 py-3"
                        >
                          <div className="flex items-baseline justify-between gap-2">
                            <span className="font-serif text-[20px] leading-none">
                              {item.character === ' ' ? '空格' : item.character}
                            </span>
                            <span className="mono-num text-[10px] text-muted-foreground">
                              错 {item.errorCount}
                            </span>
                          </div>
                          <div className="mono-num mt-1.5 text-[10px] text-muted-foreground">
                            准确率 {item.accuracy != null ? item.accuracy.toFixed(0) : '-'}%
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : null}
              </div>
            </section>
          ) : null}

          <section className="mt-12 border-t border-border pt-6">
            <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 pb-3">
              <div className="flex flex-wrap items-baseline gap-x-3">
                <h2 className="font-serif text-xl leading-none">走势</h2>
                <span className="label-mono text-[9px]">TIMELINE</span>
              </div>
              <span className="label-mono text-[9px]">
                {currentRange.label} · {chartData.length} 个采样点
              </span>
            </div>

            <Tabs value={timeRange} onValueChange={(v) => setTimeRange(v as TimeRange)} className="gap-0">
              <TabsList className="grid h-auto w-full grid-cols-5 gap-px rounded-none border border-border bg-border p-0">
                {RANGES.map((range) => (
                  <TabsTrigger
                    key={range.value}
                    value={range.value}
                    className="h-auto rounded-none border-0 bg-background px-1 py-2 text-center text-[12px] font-normal text-muted-foreground hover:text-foreground data-[state=active]:bg-foreground data-[state=active]:text-background data-[state=active]:shadow-none sm:px-2 sm:text-[13px]"
                  >
                    {range.label}
                  </TabsTrigger>
                ))}
              </TabsList>

              <TabsContent value={timeRange} className="mt-0 pt-6">
                {hasCharts ? (
                  <div className="space-y-10">
                    <SpeedChart data={chartData} />
                    <AccuracyChart data={chartData} />
                    <ErrorComparison
                      thisWeekErrors={thisWeekErrors}
                      lastWeekErrors={lastWeekErrors}
                    />

                    <section className="border-t border-border pt-6">
                      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 pb-3">
                        <div className="flex flex-wrap items-baseline gap-x-3">
                          <h2 className="font-serif text-xl leading-none">最近采样</h2>
                          <span className="label-mono text-[9px]">RECENT</span>
                        </div>
                        <span className="label-mono text-[9px]">按时间聚合 · 非单局记录</span>
                      </div>

                      <div className="hidden grid-cols-[minmax(0,1fr)_5.5rem_5.5rem_5rem_4rem] items-center gap-4 py-2 md:grid">
                        <span className="label-mono text-[9px]">时间</span>
                        <span className="label-mono text-[9px] text-right">均速 · CPM</span>
                        <span className="label-mono text-[9px] text-right">峰值 · CPM</span>
                        <span className="label-mono text-[9px] text-right">准确率</span>
                        <span className="label-mono text-[9px] text-right">错字</span>
                      </div>

                      <ol className="divide-y divide-border border-y border-border">
                        {recentPoints.map((point, index) => (
                          <li
                            key={`${point.date}-${index}`}
                            className="grid grid-cols-[minmax(0,1fr)_auto_auto] items-baseline gap-x-5 py-2.5 md:grid-cols-[minmax(0,1fr)_5.5rem_5.5rem_5rem_4rem] md:items-center md:gap-4"
                          >
                            <div className="min-w-0">
                              <div className="truncate font-mono text-[12px]">{point.date || '—'}</div>
                              <div className="mono-num mt-0.5 text-[11px] text-muted-foreground md:hidden">
                                均速 {formatNum(point.avgSpeed, 0)} · 峰值 {formatNum(point.peakSpeed, 0)}
                              </div>
                            </div>
                            <span className="mono-num hidden text-right text-[12px] text-muted-foreground md:block">
                              {formatNum(point.avgSpeed, 0)}
                            </span>
                            <span className="mono-num hidden text-right text-[12px] text-muted-foreground md:block">
                              {formatNum(point.peakSpeed, 0)}
                            </span>
                            <span className="mono-num text-right text-[12px]">
                              <span className="text-muted-foreground md:hidden">准 </span>
                              {formatNum(point.avgAccuracy)}%
                            </span>
                            <span
                              className={`mono-num text-right text-[12px] ${
                                point.errorCount > 0 ? 'text-accent' : 'text-muted-foreground'
                              }`}
                            >
                              <span className="text-muted-foreground md:hidden">错 </span>
                              {point.errorCount}
                            </span>
                          </li>
                        ))}
                      </ol>
                    </section>
                  </div>
                ) : (
                  <p className="border-t border-border pt-8 text-[13px] text-muted-foreground">
                    该时间范围内暂无数据
                  </p>
                )}
              </TabsContent>
            </Tabs>
          </section>
        </>
      )}
    </PageBody>
  );
}
