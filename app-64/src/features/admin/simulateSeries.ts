import type { GameRuleSimulationPoint } from '@/services/admin';

/** 把「0.85, 0.9 0.95」这类手输序列解析成数字数组，非法项直接丢弃 */
export function parseSeries(text: string): number[] {
  return text
    .split(/[,，\s]+/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
    .map(Number)
    .filter((item) => Number.isFinite(item));
}

export interface CurveSummary {
  first: GameRuleSimulationPoint | null;
  last: GameRuleSimulationPoint | null;
  averageMonsters: number;
}

/** 曲线摘要：首末轮难度与平均怪物数 */
export function summarizeCurve(curve: GameRuleSimulationPoint[]): CurveSummary {
  if (curve.length === 0) {
    return { first: null, last: null, averageMonsters: 0 };
  }
  const total = curve.reduce((sum, point) => sum + point.monsterCount, 0);
  return {
    first: curve[0],
    last: curve[curve.length - 1],
    averageMonsters: Math.round(total / curve.length),
  };
}
