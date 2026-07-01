/**
 * 数据分析器
 * 用于分析游戏数据，支持两条优化道路
 */

import type { CharacterStat, RoundStats } from '@/types/database';

// 字符类型分布
export interface CharTypeDistribution {
  lowercase: number;
  uppercase: number;
  number: number;
  punctuation: number;
  special: number;
  chinese: number;
}

// 易错字符信息
export interface ErrorCharInfo {
  character: string;
  errorCount: number;
  totalAttempts: number;
  accuracy: number;
  charType: string;
}

// 字符串长度分布
export interface StringLengthDistribution {
  avgLength: number;
  minLength: number;
  maxLength: number;
  distribution: Record<number, number>; // 长度 -> 出现次数
}

// 道路一：字符统计分析结果
export interface CharacterAnalysis {
  charTypeDistribution: CharTypeDistribution;
  topErrorChars: ErrorCharInfo[];
  errorCharRatio: number;
  stringLengthDistribution: StringLengthDistribution;
  totalCharacters: number;
  uniqueCharacters: number;
}

// 道路二：数学模型分析结果
export interface ModelAnalysis {
  avgAccuracy: number;
  peakAccuracy: number;
  avgSpeed: number;
  peakSpeed: number;
  avgCharErrorRate: number;
  errorCharRatio: number;
  roundCount: number;
  difficultyTrend: number[]; // 难度系数变化趋势
  speedTrend: number[]; // 速度变化趋势
  accuracyTrend: number[]; // 准确率变化趋势
}

export class DataAnalyzer {
  /**
   * 分析字符类型分布（道路一）
   */
  analyzeCharTypeDistribution(charStats: CharacterStat[]): CharTypeDistribution {
    const distribution: CharTypeDistribution = {
      lowercase: 0,
      uppercase: 0,
      number: 0,
      punctuation: 0,
      special: 0,
      chinese: 0,
    };

    charStats.forEach(stat => {
      const char = stat.character;
      const type = this.getCharType(char);
      
      if (type in distribution) {
        distribution[type as keyof CharTypeDistribution] += stat.total_attempts;
      }
    });

    return distribution;
  }

  /**
   * 获取字符类型
   */
  private getCharType(char: string): string {
    if (/[a-z]/.test(char)) return 'lowercase';
    if (/[A-Z]/.test(char)) return 'uppercase';
    if (/[0-9]/.test(char)) return 'number';
    if (/[,.!?;:'"()[\]{}]/.test(char)) return 'punctuation';
    if (/[\u4e00-\u9fa5]/.test(char)) return 'chinese';
    return 'special';
  }

  /**
   * 获取易错字符Top N（道路一）
   */
  getTopErrorChars(charStats: CharacterStat[], topN: number = 10): ErrorCharInfo[] {
    return charStats
      .map(stat => ({
        character: stat.character,
        errorCount: stat.error_count,
        totalAttempts: stat.total_attempts,
        accuracy: stat.avg_accuracy,
        charType: stat.char_type || this.getCharType(stat.character),
      }))
      .sort((a, b) => b.errorCount - a.errorCount)
      .slice(0, topN);
  }

  /**
   * 计算易错字符占比
   */
  calculateErrorCharRatio(charStats: CharacterStat[]): number {
    if (charStats.length === 0) return 0;

    const errorChars = charStats.filter(stat => stat.avg_accuracy < 70);
    return errorChars.length / charStats.length;
  }

  /**
   * 分析字符串长度分布（道路一）
   */
  analyzeStringLengthDistribution(rounds: RoundStats[]): StringLengthDistribution {
    if (rounds.length === 0) {
      return {
        avgLength: 0,
        minLength: 0,
        maxLength: 0,
        distribution: {},
      };
    }

    const lengths = rounds.map(r => r.avgStringLength);
    const distribution: Record<number, number> = {};

    lengths.forEach(length => {
      const rounded = Math.round(length);
      distribution[rounded] = (distribution[rounded] || 0) + 1;
    });

    return {
      avgLength: lengths.reduce((sum, len) => sum + len, 0) / lengths.length,
      minLength: Math.min(...lengths),
      maxLength: Math.max(...lengths),
      distribution,
    };
  }

  /**
   * 综合字符统计分析（道路一）
   */
  analyzeCharacterStats(
    charStats: CharacterStat[],
    rounds: RoundStats[]
  ): CharacterAnalysis {
    return {
      charTypeDistribution: this.analyzeCharTypeDistribution(charStats),
      topErrorChars: this.getTopErrorChars(charStats, 10),
      errorCharRatio: this.calculateErrorCharRatio(charStats),
      stringLengthDistribution: this.analyzeStringLengthDistribution(rounds),
      totalCharacters: charStats.reduce((sum, stat) => sum + stat.total_attempts, 0),
      uniqueCharacters: charStats.length,
    };
  }

  /**
   * 计算历史平均准确率（道路二）
   */
  calculateAvgAccuracy(rounds: RoundStats[]): number {
    if (rounds.length === 0) return 0;
    return rounds.reduce((sum, r) => sum + r.accuracy, 0) / rounds.length;
  }

  /**
   * 计算历史峰值准确率（道路二）
   */
  calculatePeakAccuracy(rounds: RoundStats[]): number {
    if (rounds.length === 0) return 0;
    return Math.max(...rounds.map(r => r.accuracy));
  }

  /**
   * 计算历史平均速度（道路二）
   */
  calculateAvgSpeed(rounds: RoundStats[]): number {
    if (rounds.length === 0) return 0;
    return rounds.reduce((sum, r) => sum + r.speed, 0) / rounds.length;
  }

  /**
   * 计算历史峰值速度（道路二）
   */
  calculatePeakSpeed(rounds: RoundStats[]): number {
    if (rounds.length === 0) return 0;
    return Math.max(...rounds.map(r => r.speed));
  }

  /**
   * 计算平均字符错误率（道路二）
   */
  calculateAvgCharErrorRate(rounds: RoundStats[]): number {
    if (rounds.length === 0) return 0;

    const totalErrors = rounds.reduce((sum, r) => {
      const errorCount = Object.values(r.charErrors).reduce((s, c) => s + c, 0);
      return sum + errorCount;
    }, 0);

    const totalChars = rounds.reduce((sum, r) => {
      return sum + r.monsterCount * r.avgStringLength;
    }, 0);

    return totalChars > 0 ? totalErrors / totalChars : 0;
  }

  /**
   * 综合数学模型分析（道路二）
   */
  analyzeModelData(rounds: RoundStats[], charStats: CharacterStat[]): ModelAnalysis {
    return {
      avgAccuracy: this.calculateAvgAccuracy(rounds),
      peakAccuracy: this.calculatePeakAccuracy(rounds),
      avgSpeed: this.calculateAvgSpeed(rounds),
      peakSpeed: this.calculatePeakSpeed(rounds),
      avgCharErrorRate: this.calculateAvgCharErrorRate(rounds),
      errorCharRatio: this.calculateErrorCharRatio(charStats),
      roundCount: rounds.length,
      difficultyTrend: rounds.map(r => r.difficultyCoefficient),
      speedTrend: rounds.map(r => r.speed),
      accuracyTrend: rounds.map(r => r.accuracy),
    };
  }

  /**
   * 生成字符串（基于易错字符定向变异 - 道路一）
   */
  generateTargetString(
    baseString: string,
    errorChars: ErrorCharInfo[],
    mutationRate: number = 0.3
  ): string {
    if (errorChars.length === 0) return baseString;

    const chars = baseString.split('');
    const mutationCount = Math.floor(chars.length * mutationRate);

    // 随机选择位置进行变异
    for (let i = 0; i < mutationCount; i++) {
      const pos = Math.floor(Math.random() * chars.length);
      const errorChar = errorChars[Math.floor(Math.random() * errorChars.length)];
      chars[pos] = errorChar.character;
    }

    return chars.join('');
  }

  /**
   * 根据难度参数生成字符串长度（使用正态分布）
   */
  generateStringLength(avgLength: number, stdDev: number = 1.5): number {
    // Box-Muller变换生成正态分布随机数
    const u1 = Math.random();
    const u2 = Math.random();
    const z = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    
    const length = Math.round(avgLength + z * stdDev);
    return Math.max(1, Math.min(15, length)); // 约束在[1, 15]
  }

  /**
   * 根据类型占比生成怪物类型
   */
  generateMonsterType(ratios: {
    normal: number;
    attacker: number;
    evasive: number;
    smart: number;
  }): 'NORMAL' | 'ATTACKER' | 'EVASIVE' | 'SMART' {
    const rand = Math.random() * 100;
    let cumulative = 0;

    if (rand < (cumulative += ratios.normal)) return 'NORMAL';
    if (rand < (cumulative += ratios.attacker)) return 'ATTACKER';
    if (rand < (cumulative += ratios.evasive)) return 'EVASIVE';
    return 'SMART';
  }

  /**
   * 计算易错字符增强后的出现概率
   */
  calculateEnhancedProbability(
    baseProbability: number,
    errorCharBoost: number
  ): number {
    return baseProbability * (1 + errorCharBoost);
  }
}

// 导出单例
export const dataAnalyzer = new DataAnalyzer();
