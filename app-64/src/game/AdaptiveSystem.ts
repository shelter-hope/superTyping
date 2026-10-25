/**
 * 自适应系统 — 前端本地表现层调节（速度/刷怪视觉节奏）。
 *
 * 权威难度 D 由服务端 DifficultyEngine / user_model_params 决定。
 * 本类不得改写服务端 D；仅影响客户端观感参数。后续若要消除双源，
 * 应删除客户端调节或改成只读展示服务端下发的速度/密度。
 */
import type { 
  AdaptiveConfig, 
  GameStats, 
  CharacterStat,
  GameEventHandler 
} from './types';

export class AdaptiveSystem {
  private config: AdaptiveConfig;           // 自适应系统配置
  private characterStats: Map<string, CharacterStat>;  // 字符统计数据
  private lastAdjustTime: number;           // 上次调整时间
  private currentSpeed: number;             // 当前速度
  private currentMaxMonsters: number;       // 当前最大怪物数量
  private eventHandler: GameEventHandler | null;  // 事件处理器
  private speedMultiplier: number;          // 速度倍数（用于生存模式）

  /**
   * 构造函数
   * @param config 自适应系统配置
   * @param baseSpeed 基础速度
   * @param baseMaxMonsters 基础最大怪物数量
   * @param eventHandler 事件处理器
   */
  constructor(
    config: AdaptiveConfig,
    baseSpeed: number,
    baseMaxMonsters: number,
    eventHandler: GameEventHandler | null = null
  ) {
    this.config = config;
    this.characterStats = new Map();
    this.lastAdjustTime = Date.now();
    this.currentSpeed = baseSpeed;
    this.currentMaxMonsters = baseMaxMonsters;
    this.eventHandler = eventHandler;
    this.speedMultiplier = 1.0;
  }

  /**
   * 更新统计数据
   * @param stats 游戏统计数据
   */
  update(stats: GameStats): void {
    const now = Date.now();
    const timeSinceLastAdjust = now - this.lastAdjustTime;

    // 检查是否需要调整难度
    if (timeSinceLastAdjust >= this.config.difficultyAdjustInterval) {
      this.adjustDifficulty(stats);
      this.lastAdjustTime = now;
    }
  }

  /**
   * 调整难度
   * @param stats 游戏统计数据
   */
  private adjustDifficulty(stats: GameStats): void {
    const accuracy = stats.accuracy;
    let speedChanged = false;
    let monstersChanged = false;

    // 根据准确率调整速度（更温和的调整）
    if (accuracy >= 90) {
      // 准确率很高，增加速度
      const newSpeed = Math.min(
        this.config.maxSpeed,
        this.currentSpeed + 0.15
      );
      if (newSpeed !== this.currentSpeed) {
        this.currentSpeed = newSpeed;
        speedChanged = true;
      }
    } else if (accuracy >= 85) {
      // 准确率高，小幅增加速度
      const newSpeed = Math.min(
        this.config.maxSpeed,
        this.currentSpeed + 0.1
      );
      if (newSpeed !== this.currentSpeed) {
        this.currentSpeed = newSpeed;
        speedChanged = true;
      }
    } else if (accuracy <= 40) {
      // 准确率低，降低速度
      const newSpeed = Math.max(
        this.config.minSpeed,
        this.currentSpeed - 0.2
      );
      if (newSpeed !== this.currentSpeed) {
        this.currentSpeed = newSpeed;
        speedChanged = true;
      }
    } else if (accuracy <= 60) {
      // 准确率较低，小幅降低速度
      const newSpeed = Math.max(
        this.config.minSpeed,
        this.currentSpeed - 0.1
      );
      if (newSpeed !== this.currentSpeed) {
        this.currentSpeed = newSpeed;
        speedChanged = true;
      }
    }

    // 根据准确率调整怪兽数量
    if (accuracy >= 85) {
      // 准确率高，增加怪兽数量
      const newMax = Math.min(
        this.config.maxMonsters,
        this.currentMaxMonsters + 1
      );
      if (newMax !== this.currentMaxMonsters) {
        this.currentMaxMonsters = newMax;
        monstersChanged = true;
      }
    } else if (accuracy <= 50) {
      // 准确率低，减少怪兽数量
      const newMax = Math.max(
        this.config.minMonsters,
        this.currentMaxMonsters - 1
      );
      if (newMax !== this.currentMaxMonsters) {
        this.currentMaxMonsters = newMax;
        monstersChanged = true;
      }
    }

    // 发送难度调整事件
    if (speedChanged || monstersChanged) {
      this.emitEvent({
        type: 'DIFFICULTY_ADJUSTED',
        newSpeed: this.currentSpeed,
        newMaxMonsters: this.currentMaxMonsters,
      });
    }
  }

  /**
   * 记录字符输入
   * @param char 输入的字符
   * @param correct 是否正确
   */
  recordCharacterInput(char: string, correct: boolean): void {
    let stat = this.characterStats.get(char);

    if (!stat) {
      stat = {
        char,
        correctCount: 0,
        errorCount: 0,
        accuracy: 0,
        lastUsed: Date.now(),
      };
      this.characterStats.set(char, stat);
    }

    if (correct) {
      stat.correctCount++;
    } else {
      stat.errorCount++;
    }

    stat.lastUsed = Date.now();
    stat.accuracy = stat.correctCount / (stat.correctCount + stat.errorCount);
  }

  /**
   * 获取易错字符
   * @param count 返回数量
   * @returns 易错字符列表
   */
  getWeakCharacters(count: number = 10): CharacterStat[] {
    const stats = Array.from(this.characterStats.values());

    // 按错误率排序
    return stats
      .filter(s => s.correctCount + s.errorCount >= 5) // 至少使用5次
      .sort((a, b) => {
        const errorRateA = a.errorCount / (a.correctCount + a.errorCount);
        const errorRateB = b.errorCount / (b.correctCount + b.errorCount);
        return errorRateB - errorRateA;
      })
      .slice(0, count);
  }

  /**
   * 获取所有字符统计
   * @returns 所有字符统计数据
   */
  getAllCharacterStats(): CharacterStat[] {
    return Array.from(this.characterStats.values());
  }

  /**
   * 获取当前速度
   * @returns 当前速度（考虑速度倍数）
   */
  getCurrentSpeed(): number {
    return this.currentSpeed * this.speedMultiplier;
  }

  /**
   * 设置速度倍数（用于生存模式）
   * @param multiplier 速度倍数
   */
  setSpeedMultiplier(multiplier: number): void {
    this.speedMultiplier = multiplier;
  }

  /**
   * 获取当前最大怪兽数量
   * @returns 当前最大怪兽数量
   */
  getCurrentMaxMonsters(): number {
    return this.currentMaxMonsters;
  }

  /**
   * 由外部（如 next-round 接口）设置当前速度与怪物数，用于每轮串联
   * @param speed 速度值
   */
  setCurrentSpeed(speed: number): void {
    this.currentSpeed = Math.max(this.config.minSpeed, Math.min(this.config.maxSpeed, speed));
  }

  /**
   * 由外部（如 next-round 接口）设置当前最大怪物数
   * @param maxMonsters 最大怪物数量
   */
  setCurrentMaxMonsters(maxMonsters: number): void {
    this.currentMaxMonsters = Math.max(this.config.minMonsters, Math.min(this.config.maxMonsters, maxMonsters));
  }

  /**
   * 获取推荐字符串长度
   * @param accuracy 准确率
   * @returns 推荐的字符串长度
   */
  getRecommendedLength(accuracy: number): number {
    if (accuracy >= 80) {
      // 准确率高，增加长度
      return 6;
    } else if (accuracy <= 50) {
      // 准确率低，减少长度
      return 3;
    }
    return 4;
  }

  /**
   * 获取推荐怪兽类型权重
   * @param accuracy 准确率
   * @returns 怪兽类型权重
   */
  getRecommendedTypeWeights(accuracy: number): {
    normalWeight: number;
    attackerWeight: number;
    evasiveWeight: number;
    smartWeight: number;
  } {
    if (accuracy >= 80) {
      // 准确率高，增加难度
      return {
        normalWeight: 50,
        attackerWeight: 20,
        evasiveWeight: 15,
        smartWeight: 15,
      };
    } else if (accuracy <= 50) {
      // 准确率低，降低难度
      return {
        normalWeight: 80,
        attackerWeight: 10,
        evasiveWeight: 5,
        smartWeight: 5,
      };
    }

    // 中等难度
    return {
      normalWeight: 60,
      attackerWeight: 15,
      evasiveWeight: 15,
      smartWeight: 10,
    };
  }

  /**
   * 生成分析报告
   * @returns 分析报告
   */
  generateReport(): {
    weakCharacters: CharacterStat[];
    recommendations: string[];
    stats: {
      totalCharacters: number;
      averageAccuracy: number;
      mostDifficult: CharacterStat[];
      leastDifficult: CharacterStat[];
    };
  } {
    const allStats = this.getAllCharacterStats();
    const weakChars = this.getWeakCharacters(5);

    // 计算平均准确率
    const totalAccuracy = allStats.reduce((sum, s) => sum + s.accuracy, 0);
    const averageAccuracy = allStats.length > 0 ? totalAccuracy / allStats.length : 0;

    // 最难的字符
    const mostDifficult = [...allStats]
      .sort((a, b) => a.accuracy - b.accuracy)
      .slice(0, 5);

    // 最简单的字符
    const leastDifficult = [...allStats]
      .sort((a, b) => b.accuracy - a.accuracy)
      .slice(0, 5);

    // 生成建议
    const recommendations: string[] = [];

    if (weakChars.length > 0) {
      const chars = weakChars.map(c => `'${c.char}'`).join(', ');
      recommendations.push(`重点练习字符: ${chars}`);
    }

    if (averageAccuracy < 0.7) {
      recommendations.push('建议降低游戏速度，专注于准确率提升');
    } else if (averageAccuracy > 0.9) {
      recommendations.push('准确率优秀！可以尝试更高难度');
    }

    // 检查是否有混淆的字符对
    const confusedPairs = this.findConfusedPairs();
    if (confusedPairs.length > 0) {
      confusedPairs.forEach(pair => {
        recommendations.push(`注意区分 '${pair[0]}' 和 '${pair[1]}'`);
      });
    }

    return {
      weakCharacters: weakChars,
      recommendations,
      stats: {
        totalCharacters: allStats.length,
        averageAccuracy,
        mostDifficult,
        leastDifficult,
      },
    };
  }

  /**
   * 查找容易混淆的字符对
   * @returns 容易混淆的字符对列表
   */
  private findConfusedPairs(): string[][] {
    const pairs: string[][] = [];
    const commonPairs = [
      ['s', 't'],
      ['i', 'l'],
      ['o', '0'],
      ['1', 'l'],
      ['b', 'd'],
      ['p', 'q'],
    ];

    for (const [char1, char2] of commonPairs) {
      const stat1 = this.characterStats.get(char1);
      const stat2 = this.characterStats.get(char2);

      if (stat1 && stat2) {
        // 如果两个字符的准确率都低于70%
        if (stat1.accuracy < 0.7 && stat2.accuracy < 0.7) {
          pairs.push([char1, char2]);
        }
      }
    }

    return pairs;
  }

  /**
   * 重置自适应系统
   */
  reset(): void {
    this.characterStats.clear();
    this.lastAdjustTime = Date.now();
  }

  /**
   * 发送事件
   * @param event 事件对象
   */
  private emitEvent(event: any): void {
    if (this.eventHandler) {
      this.eventHandler(event);
    }
  }

  /**
   * 设置事件处理器
   * @param handler 事件处理器
   */
  setEventHandler(handler: GameEventHandler): void {
    this.eventHandler = handler;
  }

  /**
   * 导出数据
   * @returns 导出的数据
   */
  exportData(): {
    characterStats: CharacterStat[];
    currentSpeed: number;
    currentMaxMonsters: number;
  } {
    return {
      characterStats: this.getAllCharacterStats(),
      currentSpeed: this.currentSpeed,
      currentMaxMonsters: this.currentMaxMonsters,
    };
  }

  /**
   * 导入数据
   * @param data 导入的数据
   */
  importData(data: {
    characterStats: CharacterStat[];
    currentSpeed: number;
    currentMaxMonsters: number;
  }): void {
    this.characterStats.clear();
    data.characterStats.forEach(stat => {
      this.characterStats.set(stat.char, stat);
    });
    this.currentSpeed = data.currentSpeed;
    this.currentMaxMonsters = data.currentMaxMonsters;
  }
}
