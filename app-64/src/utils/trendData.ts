/**
 * 趋势数据转换：后端 TrendPointDTO 列表 → 图表用 ChartData
 * 纯函数，便于单元测试
 */

export interface TrendPoint {
  label: string;
  avgSpeed?: number;
  peakSpeed?: number;
  avgAccuracy?: number;
  peakAccuracy?: number;
  gameCount?: number;
  errorCount?: number;
}

export interface ChartDataPoint {
  date: string;
  avgSpeed: number;
  peakSpeed: number;
  avgAccuracy: number;
  peakAccuracy: number;
  errorCount: number;
}

export function trendPointsToChartData(points: TrendPoint[] | null | undefined): ChartDataPoint[] {
  if (points == null || !Array.isArray(points)) return [];
  return points.map((p) => ({
    date: p.label ?? '',
    avgSpeed: p.avgSpeed ?? 0,
    peakSpeed: p.peakSpeed ?? 0,
    avgAccuracy: p.avgAccuracy ?? 0,
    peakAccuracy: p.peakAccuracy ?? 0,
    errorCount: p.errorCount ?? 0,
  }));
}
