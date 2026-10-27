import { describe, expect, it } from 'vitest';
import type { GameRuleSimulationPoint } from '@/services/admin';
import { parseSeries, summarizeCurve } from './simulateSeries';

function point(round: number, difficulty: number, monsterCount: number): GameRuleSimulationPoint {
  return {
    round,
    accuracy: 0.85,
    difficulty,
    monsterCount,
    normalRatio: 50,
    attackRatio: 20,
    evasiveRatio: 18,
    smartRatio: 12,
  };
}

describe('parseSeries', () => {
  it('splits comma, chinese comma, space and newline separated values', () => {
    expect(parseSeries('0.85,0.9，0.95 0.88\n0.91')).toEqual([0.85, 0.9, 0.95, 0.88, 0.91]);
  });

  it('drops empty and non numeric entries', () => {
    expect(parseSeries(' 0.8 ,, abc , 1.2 ')).toEqual([0.8, 1.2]);
  });

  it('returns an empty list for blank input', () => {
    expect(parseSeries('   ')).toEqual([]);
  });
});

describe('summarizeCurve', () => {
  it('reports first, last and rounded average monster count', () => {
    const curve = [point(1, 0.3, 4), point(2, 0.35, 5), point(3, 0.4, 7)];
    const summary = summarizeCurve(curve);
    expect(summary.first?.difficulty).toBe(0.3);
    expect(summary.last?.difficulty).toBe(0.4);
    expect(summary.averageMonsters).toBe(5);
  });

  it('returns nulls for an empty curve', () => {
    expect(summarizeCurve([])).toEqual({ first: null, last: null, averageMonsters: 0 });
  });

  it('rounds the average instead of truncating', () => {
    expect(summarizeCurve([point(1, 0.3, 4), point(2, 0.3, 5)]).averageMonsters).toBe(5);
  });
});
