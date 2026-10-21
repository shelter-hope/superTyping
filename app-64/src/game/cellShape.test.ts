import { describe, expect, it } from 'vitest';
import { cellShape } from './cellShape';

describe('cellShape', () => {
  it('gives attackers tentacles that grow with generation', () => {
    expect(cellShape('ATTACKER', 0).tentacles).toBe(3);
    expect(cellShape('ATTACKER', 2).tentacles).toBe(5);
  });

  it('keeps evasive cells translucent and smart cells noded', () => {
    expect(cellShape('EVASIVE', 1).alpha).toBeLessThan(0.6);
    expect(cellShape('SMART', 1).nodes).toBeGreaterThan(0);
    expect(cellShape('NORMAL', 0).tentacles).toBe(0);
  });
});
