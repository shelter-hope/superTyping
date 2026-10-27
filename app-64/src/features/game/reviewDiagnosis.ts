import type { CharacterStat, GameStats } from '@/game/types';

/** 结算页的复盘诊断：一句话结论 + 依据 + 下一轮重点字符。 */
export interface ReviewDiagnosis {
  headline: string;
  detail: string;
  focusChars: string[];
  tone: 'steady' | 'low-confidence' | 'warn';
}

/** 低于这个样本量的字符不参与判定，避免一两次失误被放大。 */
export const MIN_SAMPLE = 5;
/** 低于这个准确率就算「没打准」。 */
export const LOW_ACCURACY = 0.8;
/** 前三个错字占比达到这个比例，就算错误集中。 */
export const CONCENTRATED_SHARE = 0.5;
export const MAX_FOCUS = 4;

/** 空格、换行这类字符在界面上要写成可读文案。 */
export function displayChar(char: string): string {
  if (char === ' ') return '空格';
  if (char === '\n') return '回车';
  if (char === '\t') return 'Tab';
  return char;
}

function percent(value: number): string {
  return `${(value * 100).toFixed(1)}%`;
}

/**
 * 两处准确率的单位不一致：GameStats.accuracy 是百分数（0–100，引擎里直接 ×100），
 * CharacterStat.accuracy 是小数（0–1）。诊断统一按小数比较。
 */
function normalizedAccuracy(stats: GameStats): number {
  return stats.accuracy > 1 ? stats.accuracy / 100 : stats.accuracy;
}

function charList(chars: string[]): string {
  return chars.map((char) => `「${displayChar(char)}」`).join('');
}

/**
 * 根据本局逐字成绩给出复盘结论。
 * 只做确定性判断（不看历史、不做预测），保证同样的数据得到同样的文案。
 */
export function buildReviewDiagnosis(stats: GameStats, characterStats: CharacterStat[]): ReviewDiagnosis {
  const accuracy = normalizedAccuracy(stats);

  if (stats.totalChars === 0) {
    return {
      headline: '这一局没有输入记录。',
      detail: '可能是刚开局就退出了，下一次多打几个字，才能看到诊断。',
      focusChars: [],
      tone: 'low-confidence',
    };
  }

  if (stats.errorChars === 0) {
    return {
      headline: '零错字。',
      detail: `本局输入 ${stats.totalChars} 个字符全部命中，准确率 ${percent(accuracy)}。下一局的难度会上浮一档，字表也会跟着变长。`,
      focusChars: [],
      tone: 'steady',
    };
  }

  const weakChars = characterStats
    .filter((stat) => stat.correctCount + stat.errorCount >= MIN_SAMPLE)
    .filter((stat) => stat.accuracy < LOW_ACCURACY)
    .sort((a, b) => b.errorCount - a.errorCount || a.accuracy - b.accuracy);

  const focusChars = weakChars.slice(0, MAX_FOCUS).map((stat) => stat.char);

  if (weakChars.length === 0) {
    return {
      headline: '小失误，还没到要改手法的程度。',
      detail: `本局错误 ${stats.errorChars} 次、准确率 ${percent(accuracy)}，没有一个字符的样本量到 ${MIN_SAMPLE} 次，暂时不判定具体键位。再来一局样本就够。`,
      focusChars: [],
      tone: 'low-confidence',
    };
  }

  const topThreeErrors = weakChars.slice(0, 3).reduce((sum, stat) => sum + stat.errorCount, 0);
  const share = stats.errorChars > 0 ? topThreeErrors / stats.errorChars : 0;

  if (share >= CONCENTRATED_SHARE) {
    const list = charList(focusChars.slice(0, 3));
    return {
      headline: `错字集中在 ${focusChars.slice(0, 3).length} 个键上。`,
      detail: `本局 ${stats.errorChars} 次错误里有 ${Math.round(share * 100)}% 落在 ${list}。这些字符会被优先编进下一局的怪物序列，难度也会略微下调。`,
      focusChars,
      tone: 'warn',
    };
  }

  if (accuracy < LOW_ACCURACY) {
    return {
      headline: '准确率没过线，先稳再快。',
      detail: `本局准确率 ${percent(accuracy)}，低于 80% 的及格线，错误散布在 ${focusChars.length} 个键上。下一局难度会往下让一步，但错字仍会被带进去。`,
      focusChars,
      tone: 'warn',
    };
  }

  const focusWord = focusChars.length > 1 ? '这几个键' : '这个键';
  return {
    headline: '整体准度在线，个别键要盯一下。',
    detail: `本局准确率 ${percent(accuracy)}，错误主要落在 ${charList(focusChars.slice(0, 3))}。下一局${focusWord}的出现频率会提高。`,
    focusChars,
    tone: 'steady',
  };
}
