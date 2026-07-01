import { describe, it, expect } from 'vitest';
import { trendPointsToChartData } from './trendData';

describe('trendPointsToChartData', () => {
  it('returns empty array for null or undefined', () => {
    expect(trendPointsToChartData(null)).toEqual([]);
    expect(trendPointsToChartData(undefined)).toEqual([]);
  });

  it('returns empty array for empty array', () => {
    expect(trendPointsToChartData([])).toEqual([]);
  });

  it('maps backend trend points to chart data with defaults for missing fields', () => {
    const input = [
      { label: '2024-01-01', avgSpeed: 100, peakSpeed: 120, avgAccuracy: 0.9, peakAccuracy: 1, errorCount: 5 },
      { label: '2024-01-02', avgSpeed: 105 },
    ];
    const result = trendPointsToChartData(input);
    expect(result).toHaveLength(2);
    expect(result[0]).toEqual({
      date: '2024-01-01',
      avgSpeed: 100,
      peakSpeed: 120,
      avgAccuracy: 0.9,
      peakAccuracy: 1,
      errorCount: 5,
    });
    expect(result[1]).toEqual({
      date: '2024-01-02',
      avgSpeed: 105,
      peakSpeed: 0,
      avgAccuracy: 0,
      peakAccuracy: 0,
      errorCount: 0,
    });
  });

  it('preserves zero values', () => {
    const input = [{ label: 'day1', avgSpeed: 0, peakSpeed: 0, avgAccuracy: 0, peakAccuracy: 0, errorCount: 0 }];
    const result = trendPointsToChartData(input);
    expect(result[0].avgSpeed).toBe(0);
    expect(result[0].errorCount).toBe(0);
  });
});
