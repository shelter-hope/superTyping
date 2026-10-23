import { describe, expect, it } from 'vitest';
import { applyPracticeWords, createCharacterPool, practiceWords, selectedCharsLabel } from './gameRuntime';
import type { GameConfig } from '@/types/game';

const baseConfig: GameConfig = {
  enableLowercase: true,
  enableUppercase: false,
  enableNumbers: false,
  enablePunctuation: false,
  enableOperators: false,
  enableSpecialChars: false,
  enableChinese: false,
  gameMode: 'endless',
  soundEnabled: true,
};

describe('game runtime loops', () => {
  it('records the selected character classes', () => {
    expect(selectedCharsLabel({ ...baseConfig, enableNumbers: true, enableChinese: true })).toBe('lower,number,chinese');
  });

  it('adds Chinese practice characters when that class is enabled', () => {
    const pool = createCharacterPool({ ...baseConfig, enableChinese: true, enableLowercase: false });
    expect(pool).toContain('的');
    expect(pool).not.toContain('a');
  });

  it('uses recommended words only when no dictionary id is selected', () => {
    expect(practiceWords({ ...baseConfig, gameMode: 'dictionary', customDictionary: ['alpha'] })).toEqual(['alpha']);
    expect(practiceWords({ ...baseConfig, gameMode: 'dictionary', customDictionary: ['alpha'], customDictIds: [3] })).toBeUndefined();
    expect(practiceWords({ ...baseConfig, gameMode: 'endless', customDictionary: ['alpha'] })).toBeUndefined();
  });

  it('replaces server targets with the recommended words', () => {
    const monsters = applyPracticeWords(
      [{ targetStr: 'server-a' }, { targetStr: 'server-b' }, { targetStr: 'server-c' }],
      ['one', 'two'],
    );
    expect(monsters?.map((monster) => monster.targetStr)).toEqual(['one', 'two', 'one']);
  });
});
