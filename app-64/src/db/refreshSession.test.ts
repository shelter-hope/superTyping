import { describe, expect, it } from 'vitest';
import { createSingleFlight, shouldRefresh } from './refreshSession';

describe('refresh session', () => {
  it('refreshes a protected 401 only once', () => {
    expect(shouldRefresh(401, '/game/rounds', false)).toBe(true);
    expect(shouldRefresh(401, '/game/rounds', true)).toBe(false);
    expect(shouldRefresh(401, '/auth/login', false)).toBe(false);
    expect(shouldRefresh(401, '/auth/refresh', false)).toBe(false);
    expect(shouldRefresh(403, '/game/rounds', false)).toBe(false);
  });

  it('shares one refresh among concurrent callers', async () => {
    let calls = 0;
    const refresh = createSingleFlight(async () => {
      calls += 1;
      await Promise.resolve();
      return 'next-token';
    });
    const [first, second] = await Promise.all([refresh(), refresh()]);
    expect(first).toBe('next-token');
    expect(second).toBe('next-token');
    expect(calls).toBe(1);
  });
});
