import { describe, expect, it } from 'vitest';
import { isTypingKey } from './InputHandler';

describe('isTypingKey', () => {
  it('does not score Tab, Shift, or the mode switch', () => {
    expect(isTypingKey({ key: 'Tab' })).toBe(false);
    expect(isTypingKey({ key: 'Shift' })).toBe(false);
    expect(isTypingKey({ key: '`' })).toBe(false);
    expect(isTypingKey({ key: 'a' })).toBe(true);
  });
});
