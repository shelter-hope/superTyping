// 怪兽生成器
import { MonsterEntity } from './Monster';
import type { 
  Monster, 
  MonsterType, 
  MonsterSpawnConfig, 
  CharacterStat,
  EvolutionData 
} from './types';

export class MonsterGenerator {
  private config: MonsterSpawnConfig;
  private characterPool: string[];
  private weakCharacters: CharacterStat[];
  private evolutionHistory: Monster[];
  private idCounter: number;

  constructor(
    config: MonsterSpawnConfig,
    characterPool: string[],
    weakCharacters: CharacterStat[] = []
  ) {
    this.config = config;
    this.characterPool = characterPool;
    this.weakCharacters = weakCharacters;
    this.evolutionHistory = [];
    this.idCounter = 0;
  }

  // 生成怪兽
  generate(canvasWidth: number, baseSpeed: number): MonsterEntity {
    const type = this.selectType();
    const text = this.generateText(type);
    const x = Math.random() * (canvasWidth - 100) + 50;
    const y = -50;
    const speed = this.calculateSpeed(baseSpeed, type);
    const size = this.calculateSize(type);

    const monster = new MonsterEntity(
      `monster-${this.idCounter++}`,
      text,
      type,
      x,
      y,
      speed,
      size
    );

    return monster;
  }

  // 选择怪兽类型
  private selectType(): MonsterType {
    const totalWeight = 
      this.config.normalWeight +
      this.config.attackerWeight +
      this.config.evasiveWeight +
      this.config.smartWeight;

    const random = Math.random() * totalWeight;
    let cumulative = 0;

    cumulative += this.config.normalWeight;
    if (random < cumulative) return 'NORMAL';

    cumulative += this.config.attackerWeight;
    if (random < cumulative) return 'ATTACKER';

    cumulative += this.config.evasiveWeight;
    if (random < cumulative) return 'EVASIVE';

    return 'SMART';
  }

  // 生成字符串
  private generateText(type: MonsterType): string {
    // 根据类型调整长度
    let length = this.config.preferredLength;
    
    if (type === 'NORMAL') {
      length = Math.floor(Math.random() * 3) + this.config.minLength;
    } else if (type === 'ATTACKER' || type === 'SMART') {
      length = Math.floor(Math.random() * 2) + this.config.preferredLength;
    }

    length = Math.max(this.config.minLength, Math.min(this.config.maxLength, length));

    // 如果有进化历史，使用进化算法
    if (this.evolutionHistory.length > 0) {
      return this.evolveText(length);
    }

    // 否则随机生成
    return this.randomText(length);
  }

  // 随机生成字符串
  private randomText(length: number): string {
    if (this.characterPool.length === 0) {
      return this.generateDefaultText(length);
    }

    let text = '';
    for (let i = 0; i < length; i++) {
      const char = this.characterPool[Math.floor(Math.random() * this.characterPool.length)];
      text += char;
    }
    return text;
  }

  // 生成默认字符串（英文单词）
  private generateDefaultText(length: number): string {
    const words = [
      'the', 'be', 'to', 'of', 'and', 'a', 'in', 'that', 'have', 'it',
      'for', 'not', 'on', 'with', 'he', 'as', 'you', 'do', 'at', 'this',
      'but', 'his', 'by', 'from', 'they', 'we', 'say', 'her', 'she', 'or',
      'function', 'const', 'let', 'var', 'return', 'if', 'else', 'for', 'while', 'switch',
      'class', 'interface', 'type', 'import', 'export', 'async', 'await', 'promise',
    ];

    // 选择长度接近的单词
    const candidates = words.filter(w => Math.abs(w.length - length) <= 2);
    if (candidates.length > 0) {
      return candidates[Math.floor(Math.random() * candidates.length)];
    }

    return words[Math.floor(Math.random() * words.length)];
  }

  // 进化生成字符串
  private evolveText(length: number): string {
    // 选择父本（存活时间长、错误次数少的怪兽）
    const parents = this.selectParents();
    
    if (parents.length === 0) {
      return this.randomText(length);
    }

    // 字符串切片重组
    let text = this.recombine(parents, length);

    // 定向变异（插入易错字符）
    text = this.mutate(text);

    return text;
  }

  // 选择父本
  private selectParents(): Monster[] {
    // 按适应度排序（存活时间长、错误次数少）
    const sorted = [...this.evolutionHistory].sort((a, b) => {
      const scoreA = this.calculateFitness(a);
      const scoreB = this.calculateFitness(b);
      return scoreB - scoreA;
    });

    // 选择前30%作为父本
    const count = Math.max(2, Math.floor(sorted.length * 0.3));
    return sorted.slice(0, count);
  }

  // 计算适应度
  private calculateFitness(monster: Monster): number {
    const survivalTime = (monster.eliminatedAt || Date.now()) - monster.createdAt;
    const errorPenalty = monster.errorCount * 1000;
    return survivalTime - errorPenalty;
  }

  // 字符串切片重组
  private recombine(parents: Monster[], targetLength: number): string {
    let text = '';
    
    while (text.length < targetLength) {
      // 随机选择一个父本
      const parent = parents[Math.floor(Math.random() * parents.length)];
      
      // 随机选择一个切片长度
      const sliceLength = Math.min(
        Math.floor(Math.random() * 3) + 1,
        parent.text.length,
        targetLength - text.length
      );
      
      // 随机选择起始位置
      const startPos = Math.floor(Math.random() * (parent.text.length - sliceLength + 1));
      
      // 提取切片
      const slice = parent.text.substring(startPos, startPos + sliceLength);
      text += slice;
    }

    return text.substring(0, targetLength);
  }

  // 定向变异
  private mutate(text: string): string {
    if (this.weakCharacters.length === 0) return text;

    // 30%概率进行变异
    if (Math.random() > 0.3) return text;

    // 随机选择1-2个位置进行变异
    const mutationCount = Math.floor(Math.random() * 2) + 1;
    const chars = text.split('');

    for (let i = 0; i < mutationCount && i < chars.length; i++) {
      const pos = Math.floor(Math.random() * chars.length);
      
      // 选择一个易错字符
      const weakChar = this.weakCharacters[
        Math.floor(Math.random() * this.weakCharacters.length)
      ];
      
      chars[pos] = weakChar.char;
    }

    return chars.join('');
  }

  // 计算速度
  private calculateSpeed(baseSpeed: number, type: MonsterType): number {
    switch (type) {
      case 'NORMAL':
        return baseSpeed;
      case 'ATTACKER':
        return baseSpeed * 0.8; // 慢一点
      case 'EVASIVE':
        return baseSpeed * 1.2; // 快一点
      case 'SMART':
        return baseSpeed;
      default:
        return baseSpeed;
    }
  }

  // 计算大小（供引擎在词源模式创建怪物时使用）
  calculateSize(type: MonsterType): number {
    switch (type) {
      case 'NORMAL':
        return 1;
      case 'ATTACKER':
        return 1.5;
      case 'EVASIVE':
        return 0.8;
      case 'SMART':
        return 1.2;
      default:
        return 1;
    }
  }

  // 更新进化历史
  updateEvolutionHistory(monsters: Monster[]): void {
    // 只保留已消灭的怪兽
    const eliminated = monsters.filter(m => !m.isAlive && m.eliminatedAt);
    
    // 添加到历史
    this.evolutionHistory.push(...eliminated);
    
    // 只保留最近100个
    if (this.evolutionHistory.length > 100) {
      this.evolutionHistory = this.evolutionHistory.slice(-100);
    }
  }

  // 更新易错字符
  updateWeakCharacters(characters: CharacterStat[]): void {
    // 按错误率排序，选择前10个
    this.weakCharacters = [...characters]
      .sort((a, b) => {
        const errorRateA = a.errorCount / (a.correctCount + a.errorCount || 1);
        const errorRateB = b.errorCount / (b.correctCount + b.errorCount || 1);
        return errorRateB - errorRateA;
      })
      .slice(0, 10);
  }

  // 更新配置
  updateConfig(config: Partial<MonsterSpawnConfig>): void {
    this.config = { ...this.config, ...config };
  }

  // 更新字符池
  updateCharacterPool(pool: string[]): void {
    this.characterPool = pool;
  }
}
