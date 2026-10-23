import { describe, expect, it } from 'vitest';
import { shouldSuppressToast } from '@/hooks/use-toast';

describe('toast dedupe', () => {
  it('suppresses the same toast inside two seconds', () => {
    expect(shouldSuppressToast(undefined, 1000)).toBe(false);
    expect(shouldSuppressToast(1000, 2500)).toBe(true);
    expect(shouldSuppressToast(1000, 3000)).toBe(false);
  });
});
