/**
 * 难度控制器
 * 实现基于数学模型的自主调控算法
 */

import type { DifficultyParams, RoundStats } from '@/types/database';

// 模型参数配置
interface ModelWeights {
  // 综合难度系数权重
  WA: number;   // 实时准确率权重
  WS: number;   // 实时速度权重
  WE: number;   // 单字符错误率权重
  WAh: number;  // 历史平均准确率权重
  WSh: number;  // 历史峰值速度权重
  WRe: number;  // 易错字符占比权重
  WK: number;   // 当前轮次权重
  
  // 平滑系数
  alpha: number;
  
  // 参考值
  Smax: number;  // 预设最大参考速度
  Sref: number;  // 参考峰值速度
}

// 历史数据
interface HistoricalData {
  avgAccuracy: number;
  peakSpeed: number;
  errorCharRatio: number;
}

// 当前轮次数据
interface CurrentRoundData {
  accuracy: number;
  speed: number;
  avgCharErrorRate: number;
  roundNumber: number;
}

export class DifficultyController {
  private weights: ModelWeights;
  private prevDifficulty: number;
  private prevSpeed: number;
  private prevMonsterCount: number;
  private prevStringLength: number;

  constructor() {
    // 默认权重配置
    this.weights = {
      WA: 0.3,
      WS: 0.2,
      WE: 0.2,
      WAh: 0.15,
      WSh: 0.1,
      WRe: 0.03,
      WK: 0.02,
      alpha: 0.7,
      Smax: 150,
      Sref: 120,
    };

    // 初始值
    this.prevDifficulty = 0.3;
    this.prevSpeed = 0.8;
    this.prevMonsterCount = 5;
    this.prevStringLength = 5;
  }

  /**
   * 更新模型权重配置
   */
  updateWeights(weights: Partial<ModelWeights>) {
    this.weights = { ...this.weights, ...weights };
  }

  /**
   * 计算综合难度系数 Dₜ
   */
  calculateDifficultyCoefficient(
    current: CurrentRoundData,
    historical: HistoricalData
  ): number {
    const { WA, WS, WE, WAh, WSh, WRe, WK, alpha, Smax, Sref } = this.weights;
    const { accuracy, speed, avgCharErrorRate, roundNumber } = current;
    const { avgAccuracy, peakSpeed, errorCharRatio } = historical;

    // 归一化速度
    const normalizedSpeed = speed / Smax;
    const normalizedPeakSpeed = peakSpeed / Sref;

    // 计算当前轮难度分量
    const currentComponent =
      WA * accuracy +
      WS * normalizedSpeed +
      WE * avgCharErrorRate +
      WAh * avgAccuracy +
      WSh * normalizedPeakSpeed +
      WRe * errorCharRatio +
      WK * Math.log2(roundNumber);

    // 平滑处理
    const Dt = alpha * this.prevDifficulty + (1 - alpha) * currentComponent;

    // 约束在合理范围
    const clampedDt = Math.max(0.1, Math.min(1.0, Dt));

    this.prevDifficulty = clampedDt;
    return clampedDt;
  }

  /**
   * 调节怪物基础行进速度 Vₜ
   */
  adjustBaseSpeed(
    accuracy: number,
    speed: number,
    peakSpeed: number,
    difficulty: number
  ): number {
    const { alpha, Sref } = this.weights;
    let Vt = this.prevSpeed;

    // 条件1: 高准确率且高速度 -> 提升速度
    if (accuracy >= 0.85 && speed >= peakSpeed * 0.8) {
      Vt = alpha * this.prevSpeed + 0.1;
    }
    // 条件2: 低准确率或低速度 -> 降低速度
    else if (accuracy <= 0.4 || speed <= 20) {
      Vt = alpha * this.prevSpeed - 0.1;
    }
    // 条件3: 其他情况 -> 根据难度系数调节
    else {
      Vt = alpha * this.prevSpeed + (difficulty - 0.5) * 0.2;
    }

    // 约束: Vt ∈ [0.8, 3.0]
    Vt = Math.max(0.8, Math.min(3.0, Vt));

    this.prevSpeed = Vt;
    return Vt;
  }

  /**
   * 调节单轮怪物生成数量 Nₜ
   */
  adjustMonsterCount(
    roundNumber: number,
    speed: number,
    accuracy: number,
    difficulty: number
  ): number {
    const { alpha } = this.weights;
    let Nt = this.prevMonsterCount;

    // 条件1: 新手期且速度尚可 -> 缓慢增加
    if (roundNumber <= 10 && speed >= 40) {
      Nt = Math.min(25, alpha * this.prevMonsterCount + 1);
    }
    // 条件2: 高速度且高准确率 -> 快速增加
    else if (speed >= 120 && accuracy >= 0.7) {
      Nt = Math.min(25, alpha * this.prevMonsterCount + 2);
    }
    // 条件3: 低准确率或低速度 -> 保持最小值
    else if (accuracy <= 0.5 || speed <= 30) {
      Nt = Math.max(3, alpha * this.prevMonsterCount);
    }
    // 条件4: 其他情况 -> 根据难度系数调节
    else {
      Nt = alpha * this.prevMonsterCount + Math.floor(difficulty * 2);
    }

    // 约束: Nt ∈ [3, 25]
    Nt = Math.max(3, Math.min(25, Math.round(Nt)));

    this.prevMonsterCount = Nt;
    return Nt;
  }

  /**
   * 调节平均字符串长度 Lₜ
   */
  adjustStringLength(
    accuracy: number,
    speed: number,
    avgCharErrorRate: number,
    difficulty: number
  ): number {
    const { alpha } = this.weights;
    let Lt = this.prevStringLength;

    // 条件1: 低准确率且高错误率 -> 降低长度
    if (accuracy <= 0.5 && avgCharErrorRate >= 0.3) {
      Lt = Math.max(1, alpha * this.prevStringLength - 1);
    }
    // 条件2: 高准确率且高速度 -> 提升长度
    else if (accuracy >= 0.8 && speed >= 80) {
      Lt = Math.min(15, alpha * this.prevStringLength + 1);
    }
    // 条件3: 其他情况 -> 根据难度系数调节
    else {
      Lt = alpha * this.prevStringLength + Math.floor((difficulty - 0.4) * 3);
    }

    // 约束: Lt ∈ [1, 15]
    Lt = Math.max(1, Math.min(15, Math.round(Lt)));

    this.prevStringLength = Lt;
    return Lt;
  }

  /**
   * 调节怪物类型占比
   */
  adjustMonsterTypeRatios(
    difficulty: number,
    errorCharRatio: number
  ): { normal: number; attacker: number; evasive: number; smart: number } {
    const { WE } = this.weights;

    // 计算类型占比基础调节系数
    const Tt = difficulty * WE * errorCharRatio;

    // 分配各类型占比
    let Pn = 80 - Math.floor(Tt * 100); // 普通型
    let Pa = 5 + Math.floor(Tt * 40);   // 攻击型
    let Pe = 5 + Math.floor(Tt * 30);   // 回避型
    let Ps = 10 + Math.floor(Tt * 30);  // 智慧型

    // 约束各占比在合理范围
    Pn = Math.max(40, Math.min(90, Pn));
    Pa = Math.max(0, Math.min(30, Pa));
    Pe = Math.max(0, Math.min(25, Pe));
    Ps = Math.max(5, Math.min(35, Ps));

    // 归一化确保总和为100%
    const total = Pn + Pa + Pe + Ps;
    Pn = Math.round((Pn / total) * 100);
    Pa = Math.round((Pa / total) * 100);
    Pe = Math.round((Pe / total) * 100);
    Ps = 100 - Pn - Pa - Pe; // 确保总和为100

    return {
      normal: Pn,
      attacker: Pa,
      evasive: Pe,
      smart: Ps,
    };
  }

  /**
   * 计算易错字符出现概率增强系数
   */
  calculateErrorCharBoost(
    difficulty: number,
    errorCharRatio: number
  ): number {
    const { WE } = this.weights;
    
    // 易错字符出现概率 = 基础概率 × (1 + WE × Re × Dt)
    // 这里返回增强系数: WE × Re × Dt
    const boost = WE * errorCharRatio * difficulty;
    
    return Math.max(0, Math.min(2.0, boost)); // 约束在[0, 2.0]
  }

  /**
   * 综合调节 - 计算下一轮的所有参数
   */
  adjustDifficulty(
    current: CurrentRoundData,
    historical: HistoricalData
  ): DifficultyParams {
    // 1. 计算综合难度系数
    const difficulty = this.calculateDifficultyCoefficient(current, historical);

    // 2. 调节各项参数
    const baseSpeed = this.adjustBaseSpeed(
      current.accuracy,
      current.speed,
      historical.peakSpeed,
      difficulty
    );

    const monsterCount = this.adjustMonsterCount(
      current.roundNumber,
      current.speed,
      current.accuracy,
      difficulty
    );

    const avgStringLength = this.adjustStringLength(
      current.accuracy,
      current.speed,
      current.avgCharErrorRate,
      difficulty
    );

    const monsterTypeRatios = this.adjustMonsterTypeRatios(
      difficulty,
      historical.errorCharRatio
    );

    const errorCharBoost = this.calculateErrorCharBoost(
      difficulty,
      historical.errorCharRatio
    );

    return {
      difficultyCoefficient: difficulty,
      baseSpeed,
      monsterCount,
      avgStringLength,
      monsterTypeRatios,
      errorCharBoost,
    };
  }

  /**
   * 重置控制器状态（新游戏开始时调用）
   */
  reset() {
    this.prevDifficulty = 0.3;
    this.prevSpeed = 0.8;
    this.prevMonsterCount = 5;
    this.prevStringLength = 5;
  }

  /**
   * 获取当前状态
   */
  getCurrentState() {
    return {
      prevDifficulty: this.prevDifficulty,
      prevSpeed: this.prevSpeed,
      prevMonsterCount: this.prevMonsterCount,
      prevStringLength: this.prevStringLength,
    };
  }

  /**
   * 设置当前状态（用于恢复游戏）
   */
  setState(state: {
    prevDifficulty: number;
    prevSpeed: number;
    prevMonsterCount: number;
    prevStringLength: number;
  }) {
    this.prevDifficulty = state.prevDifficulty;
    this.prevSpeed = state.prevSpeed;
    this.prevMonsterCount = state.prevMonsterCount;
    this.prevStringLength = state.prevStringLength;
  }
}

// 导出单例
export const difficultyController = new DifficultyController();
