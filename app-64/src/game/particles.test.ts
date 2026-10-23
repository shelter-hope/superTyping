import { describe, expect, it } from 'vitest';
import {
  BURST_LIFE,
  POOL_SIZE,
  activeBurstCount,
  burstCount,
  createFxState,
  syncEffects,
  type MonsterSnap,
} from './particles';

const monster = (id: string, generation = 0): MonsterSnap => ({
  id,
  x: 100,
  y: 80,
  color: 0x3dffe0,
  generation,
});

describe('particles', () => {
  it('spawns one burst when an id disappears', () => {
    const state = createFxState();
    syncEffects(state, [monster('a', 2)], 0, 16, false, 800, 600);
    syncEffects(state, [], 20, 16, false, 800, 600);
    expect(activeBurstCount(state)).toBe(burstCount(2));
    expect(state.particles).toHaveLength(POOL_SIZE);
  });

  it('throttles ripples for the same monster', () => {
    const state = createFxState();
    const hit = { monsterId: 'a', x: 10, y: 12, timestamp: 50 };
    syncEffects(state, [monster('a')], 50, 16, false, 800, 600, hit);
    syncEffects(state, [monster('a')], 80, 16, false, 800, 600, hit);
    expect(state.ripples).toHaveLength(1);
    syncEffects(state, [monster('a')], 200, 16, false, 800, 600, { ...hit, timestamp: 200 });
    expect(state.ripples).toHaveLength(2);
  });

  it('does not grow past the pool and skips bursts when motion is reduced', () => {
    const state = createFxState();
    const crowd = Array.from({ length: 30 }, (_, index) => monster(`m${index}`, 8));
    syncEffects(state, crowd, 0, 16, false, 800, 600);
    syncEffects(state, [], 30, 16, false, 800, 600);
    expect(state.particles).toHaveLength(POOL_SIZE);
    expect(activeBurstCount(state)).toBeLessThanOrEqual(POOL_SIZE);
    const quiet = createFxState();
    syncEffects(quiet, [monster('a')], 0, 16, true, 800, 600);
    syncEffects(quiet, [], 20, 16, true, 800, 600, { monsterId: 'a', x: 1, y: 1, timestamp: 20 });
    expect(activeBurstCount(quiet)).toBe(0);
    expect(quiet.ripples).toHaveLength(0);
    expect(quiet.particles.some((particle) => particle.kind === 'dust' && particle.vx === 0)).toBe(true);
  });

  it('expires burst particles', () => {
    const state = createFxState();
    syncEffects(state, [monster('a')], 0, 16, false, 400, 300);
    syncEffects(state, [], 10, 16, false, 400, 300);
    syncEffects(state, [], BURST_LIFE + 40, BURST_LIFE + 20, false, 400, 300);
    expect(activeBurstCount(state)).toBe(0);
  });
});
