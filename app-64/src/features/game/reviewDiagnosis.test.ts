import { describe, expect, it } from 'vitest';
import type { CharacterStat, GameStats } from '@/game/types';
import { buildReviewDiagnosis, displayChar } from '@/features/game/reviewDiagnosis';

function makeStats(overrides: Partial<GameStats> = {}): GameStats {
  return {
    mode: 'ENDLESS',
    score: 0,
    round: 1,
    totalChars: 0,
    correctChars: 0,
    errorChars: 0,
    accuracy: 1,
    speed: 100,
    peakSpeed: 120,
    startTime: 0,
    currentTime: 0,
    monstersKilled: 0,
    longestStreak: 0,
    currentStreak: 0,
    ...overrides,
  };
}

function makeChar(char: string, correctCount: number, errorCount: number): CharacterStat {
  const sample = correctCount + errorCount;
  return {
    char,
    correctCount,
    errorCount,
    accuracy: sample === 0 ? 1 : correctCount / sample,
    lastUsed: 0,
  };
}

describe('buildReviewDiagnosis', () => {
  it('零错字时给出口径明确的结论', () => {
    const diagnosis = buildReviewDiagnosis(
      makeStats({ totalChars: 120, correctChars: 120, errorChars: 0, accuracy: 1 }),
      [makeChar('a', 40, 0)]
    );
    expect(diagnosis.headline).toBe('零错字。');
    expect(diagnosis.focusChars).toEqual([]);
    expect(diagnosis.tone).toBe('steady');
    expect(diagnosis.detail).toContain('120');
  });

  it('错误集中在少数键上时，focusChars 按错误次数排序', () => {
    const diagnosis = buildReviewDiagnosis(
      makeStats({ totalChars: 41, correctChars: 20, errorChars: 21, accuracy: 0.49 }),
      [
        makeChar('i', 1, 9),
        makeChar('t', 2, 8),
        makeChar('m', 3, 2),
        makeChar('n', 7, 1),
        makeChar('o', 7, 1),
      ]
    );
    expect(diagnosis.headline).toBe('错字集中在 3 个键上。');
    expect(diagnosis.focusChars[0]).toBe('i');
    expect(diagnosis.focusChars[1]).toBe('t');
    expect(diagnosis.tone).toBe('warn');
    expect(diagnosis.detail).toContain('90%');
  });

  it('准确率偏低但错误分散时，提示先稳再快', () => {
    const chars = Array.from({ length: 10 }, (_, index) => makeChar(String.fromCharCode(97 + index), 3, 2));
    const diagnosis = buildReviewDiagnosis(
      makeStats({ totalChars: 50, correctChars: 30, errorChars: 20, accuracy: 0.6 }),
      chars
    );
    expect(diagnosis.headline).toBe('准确率没过线，先稳再快。');
    expect(diagnosis.focusChars).toHaveLength(4);
    expect(diagnosis.tone).toBe('warn');
  });

  it('样本不足时不猜测具体键位', () => {
    const diagnosis = buildReviewDiagnosis(
      makeStats({ totalChars: 6, correctChars: 3, errorChars: 3, accuracy: 0.5 }),
      [makeChar('a', 1, 1), makeChar('b', 1, 1), makeChar('c', 1, 1)]
    );
    expect(diagnosis.headline).toBe('小失误，还没到要改手法的程度。');
    expect(diagnosis.focusChars).toEqual([]);
    expect(diagnosis.tone).toBe('low-confidence');
  });

  it('准度在线但个别键偏弱时，点名要盯的键', () => {
    const diagnosis = buildReviewDiagnosis(
      makeStats({ totalChars: 100, correctChars: 85, errorChars: 7, accuracy: 0.85 }),
      [makeChar('q', 7, 3), makeChar('w', 20, 1), makeChar('e', 20, 1), makeChar('r', 20, 1), makeChar('t', 20, 1)]
    );
    expect(diagnosis.headline).toBe('整体准度在线，个别键要盯一下。');
    expect(diagnosis.focusChars).toEqual(['q']);
    expect(diagnosis.detail).toContain('「q」');
  });

  it('GameStats 用百分数（引擎口径）时也能正确比较', () => {
    const chars = Array.from({ length: 10 }, (_, index) => makeChar(String.fromCharCode(97 + index), 3, 2));
    const diagnosis = buildReviewDiagnosis(
      makeStats({ totalChars: 50, correctChars: 30, errorChars: 20, accuracy: 60 }),
      chars
    );
    expect(diagnosis.headline).toBe('准确率没过线，先稳再快。');
    expect(diagnosis.detail).toContain('60.0%');
  });

  it('空局不生成具体建议', () => {
    const diagnosis = buildReviewDiagnosis(makeStats(), []);
    expect(diagnosis.headline).toBe('这一局没有输入记录。');
    expect(diagnosis.focusChars).toEqual([]);
  });

  it('特殊字符有可读文案', () => {
    expect(displayChar(' ')).toBe('空格');
    expect(displayChar('\n')).toBe('回车');
    expect(displayChar('\t')).toBe('Tab');
    expect(displayChar('a')).toBe('a');
  });
});
