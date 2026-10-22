import { describe, expect, it } from 'vitest';
import { DNA_RUNGS, clampProgress, dnaGeometry, litRungs } from './dnaBar';

describe('dna bar', () => {
  it('clamps progress and lights half, none, and all rungs', () => {
    expect(clampProgress(-1)).toBe(0);
    expect(clampProgress(2)).toBe(1);
    expect(clampProgress(Number.NaN)).toBe(0);
    expect(litRungs(0)).toBe(0);
    expect(litRungs(0.5)).toBe(12);
    expect(litRungs(1)).toBe(DNA_RUNGS);
  });

  it('drops the wave when motion is reduced and follows canvas height', () => {
    const moving = dnaGeometry(800, 600, 0.5, 0, false);
    const still = dnaGeometry(800, 480, 0.5, 0, true);
    expect(moving.y).toBe(572);
    expect(still.y).toBe(452);
    expect(still.amplitude).toBe(0);
    expect(still.rungs.filter((rung) => rung.lit)).toHaveLength(12);
    expect(still.rungs[12].current).toBe(true);
  });
});
