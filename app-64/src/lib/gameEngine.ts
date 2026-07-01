// 游戏引擎核心逻辑
import type {
  Monster,
  MonsterType,
  GameConfig,
  GameStats,
  CharacterInput,
  RoundData,
} from '@/types/game';

// 字符集定义
const CHAR_SETS = {
  lowercase: 'abcdefghijklmnopqrstuvwxyz',
  uppercase: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
  numbers: '0123456789',
  punctuation: ',.!?;:\'"',
  operators: '+-*/=<>',
  specialChars: '@#$%^&*()_[]{}|\\',
  chinese: '的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能下过子说产种面而方后多定行学法所民得经十三之进着等部度家电力里如水化高自二理起小物现实加量都两体制机当使点从业本去把性好应开它合还因由其些然前外天政四日那社义事平形相全表间样与关各重新线内数正心反你明看原又么利比或但质气第向道命此变条只没结解问意建月公无系军很情者最立代想已通并提直题党程展五果料象员革位入常文总次品式活设及管特件长求老头基资边流路级少图山统接知较将组见计别她手角期根论运农指几九区强放决西被干做必战先回则任取据处队南给色光门即保治北造百规热领七海口东导器压志世金增争济阶油思术极交受联什认六共权收证改清己美再采转更单风切打白教速花带安场身车例真务具万每目至达走积示议声报斗完类八离华名确才科张信马节话米整空元况今集温传土许步群广石记需段研界拉林律叫且究观越织装影算低持音众书布复容儿须际商非验连断深难近矿千周委素技备半办青省列习响约支般史感劳便团往酸历市克何除消构府称太准精值号率族维划选标写存候毛亲快效斯院查江型眼王按格养易置派层片始却专状育厂京识适属圆包火住调满县局照参红细引听该铁价严',
};

export class GameEngine {
  private config: GameConfig;
  private stats: GameStats;
  private monsters: Monster[] = [];
  private roundData: RoundData[] = [];
  private currentRound = 0;
  private characterHistory: CharacterInput[] = [];
  private weakCharacters: Map<string, { errorCount: number; accuracy: number }> = new Map();
  private canvas: HTMLCanvasElement;
  private ctx: CanvasRenderingContext2D;
  private animationFrameId: number | null = null;
  private lastFrameTime = 0;
  private playerY: number;
  private isWaitingForNextRound = false; // 防止重复开始新轮次

  constructor(canvas: HTMLCanvasElement, config: GameConfig) {
    this.canvas = canvas;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('无法获取Canvas上下文');
    this.ctx = ctx;
    this.config = config;
    this.playerY = canvas.height - 50;
    
    this.stats = {
      score: 0,
      rounds: 0,
      totalCharacters: 0,
      correctCharacters: 0,
      errorCharacters: 0,
      startTime: Date.now(),
      currentSpeed: 0,
      currentAccuracy: 100,
      peakSpeed: 0,
      peakAccuracy: 100,
      consecutiveCorrect: 0,
      maxConsecutiveCorrect: 0,
    };
  }

  // 获取启用的字符集
  private getEnabledCharacters(): string {
    let chars = '';
    if (this.config.enableLowercase) chars += CHAR_SETS.lowercase;
    if (this.config.enableUppercase) chars += CHAR_SETS.uppercase;
    if (this.config.enableNumbers) chars += CHAR_SETS.numbers;
    if (this.config.enablePunctuation) chars += CHAR_SETS.punctuation;
    if (this.config.enableOperators) chars += CHAR_SETS.operators;
    if (this.config.enableSpecialChars) chars += CHAR_SETS.specialChars;
    if (this.config.enableChinese) chars += CHAR_SETS.chinese;
    return chars || CHAR_SETS.lowercase; // 默认至少有小写字母
  }

  // 生成随机字符串
  private generateRandomString(length: number, chars: string): string {
    let result = '';
    for (let i = 0; i < length; i++) {
      result += chars[Math.floor(Math.random() * chars.length)];
    }
    return result;
  }

  // 从弱项字符中选择
  private selectWeakCharacters(count: number): string[] {
    const sorted = Array.from(this.weakCharacters.entries())
      .sort((a, b) => {
        // 优先选择错误次数多的
        if (b[1].errorCount !== a[1].errorCount) {
          return b[1].errorCount - a[1].errorCount;
        }
        // 其次选择准确率低的
        return a[1].accuracy - b[1].accuracy;
      })
      .slice(0, count)
      .map(([char]) => char);
    
    return sorted;
  }

  // 生成初始怪物（第一轮）
  private generateInitialMonsters(): Monster[] {
    const monsters: Monster[] = [];
    const chars = this.getEnabledCharacters();
    const weakChars = this.selectWeakCharacters(6);
    const monsterCount = 5 + Math.floor(Math.random() * 3); // 5-7个怪物

    for (let i = 0; i < monsterCount; i++) {
      const length = 3 + Math.floor(Math.random() * 4); // 3-6个字符
      let text = '';
      
      // 30%概率使用弱项字符
      if (weakChars.length > 0 && Math.random() < 0.3) {
        const weakChar = weakChars[Math.floor(Math.random() * weakChars.length)];
        text = weakChar + this.generateRandomString(length - 1, chars);
      } else {
        text = this.generateRandomString(length, chars);
      }

      // 每个怪物延迟500-1500毫秒生成
      const spawnDelay = i * (500 + Math.random() * 1000);
      monsters.push(this.createMonster(text, this.getRandomMonsterType(), spawnDelay));
    }

    return monsters;
  }

  // 遗传算法：选择优质父本
  private selectParents(): Monster[] {
    if (this.roundData.length === 0) return [];
    
    const lastRound = this.roundData[this.roundData.length - 1];
    const allMonsters = lastRound.monsters;
    
    // 评分：存活时间长 + 消灭耗时长 + 错误次数多
    const scored = allMonsters.map(m => {
      const survivalTime = (m.killTime || lastRound.endTime || Date.now()) - m.spawnTime;
      const score = survivalTime + m.errorCount * 1000;
      return { monster: m, score };
    });
    
    // 选择前3个最优
    scored.sort((a, b) => b.score - a.score);
    return scored.slice(0, 3).map(s => s.monster);
  }

  // 字符串切片重组
  private recombineStrings(parents: Monster[]): string[] {
    if (parents.length === 0) return [];
    
    const results: string[] = [];
    const parentTexts = parents.map(p => p.text);
    
    // 生成3-5个新字符串
    const count = 3 + Math.floor(Math.random() * 3);
    
    for (let i = 0; i < count; i++) {
      // 随机选择两个父本
      const p1 = parentTexts[Math.floor(Math.random() * parentTexts.length)];
      const p2 = parentTexts[Math.floor(Math.random() * parentTexts.length)];
      
      // 切片重组
      const cutPoint1 = Math.floor(Math.random() * p1.length);
      const cutPoint2 = Math.floor(Math.random() * p2.length);
      
      let newText = p1.substring(0, cutPoint1) + p2.substring(cutPoint2);
      
      // 限制长度在3-8之间
      if (newText.length < 3) {
        newText += this.generateRandomString(3 - newText.length, this.getEnabledCharacters());
      } else if (newText.length > 8) {
        newText = newText.substring(0, 8);
      }
      
      results.push(newText);
    }
    
    return results;
  }

  // 定向变异
  private mutateString(text: string): string {
    const weakChars = this.selectWeakCharacters(5);
    if (weakChars.length === 0) return text;
    
    // 随机替换1-2个字符为弱项字符
    const mutationCount = 1 + Math.floor(Math.random() * 2);
    let result = text.split('');
    
    for (let i = 0; i < mutationCount && i < result.length; i++) {
      const pos = Math.floor(Math.random() * result.length);
      const weakChar = weakChars[Math.floor(Math.random() * weakChars.length)];
      result[pos] = weakChar;
    }
    
    return result.join('');
  }

  // 生成下一轮怪物（无尽适应模式）
  private generateAdaptiveMonsters(): Monster[] {
    const monsters: Monster[] = [];
    const parents = this.selectParents();
    const recombined = this.recombineStrings(parents);
    const chars = this.getEnabledCharacters();
    
    // 根据准确率调整怪物数量
    let monsterCount = 5;
    if (this.stats.currentAccuracy >= 85) {
      monsterCount = 7 + Math.floor(Math.random() * 3); // 7-9个
    } else if (this.stats.currentAccuracy <= 40) {
      monsterCount = 3 + Math.floor(Math.random() * 2); // 3-4个
    } else {
      monsterCount = 5 + Math.floor(Math.random() * 3); // 5-7个
    }
    
    // 根据准确率调整字符串长度
    let avgLength = 4;
    if (this.stats.currentAccuracy >= 80) {
      avgLength = 6; // 6字符占比提升
    } else if (this.stats.currentAccuracy <= 50) {
      avgLength = 3; // 短字符串
    }
    
    for (let i = 0; i < monsterCount; i++) {
      let text: string;
      
      if (recombined.length > 0 && Math.random() < 0.5) {
        // 50%使用重组的字符串
        text = recombined[Math.floor(Math.random() * recombined.length)];
        // 30%概率进行变异
        if (Math.random() < 0.3) {
          text = this.mutateString(text);
        }
      } else {
        // 生成新的随机字符串
        const length = Math.max(3, avgLength + Math.floor(Math.random() * 3) - 1);
        text = this.generateRandomString(length, chars);
      }
      
      // 每个怪物延迟500-1500毫秒生成
      const spawnDelay = i * (500 + Math.random() * 1000);
      monsters.push(this.createMonster(text, this.getRandomMonsterType(), spawnDelay));
    }
    
    return monsters;
  }

  // 生成下一轮怪物（词源界定模式）
  private generateDictionaryMonsters(): Monster[] {
    const monsters: Monster[] = [];
    const dictionary = this.config.customDictionary || [];
    
    if (dictionary.length === 0) {
      return this.generateAdaptiveMonsters();
    }
    
    const parents = this.selectParents();
    const recombined = this.recombineStrings(parents);
    
    // 随机选择词库中的词
    const randomWords = [];
    const wordCount = 3 + Math.floor(Math.random() * 3);
    for (let i = 0; i < wordCount; i++) {
      randomWords.push(dictionary[Math.floor(Math.random() * dictionary.length)]);
    }
    
    // 混合词库词和重组词
    const allTexts = [...randomWords, ...recombined];
    
    const monsterCount = 5 + Math.floor(Math.random() * 3);
    for (let i = 0; i < monsterCount; i++) {
      let text = allTexts[Math.floor(Math.random() * allTexts.length)];
      
      // 20%概率变异
      if (Math.random() < 0.2) {
        text = this.mutateString(text);
      }
      
      // 每个怪物延迟500-1500毫秒生成
      const spawnDelay = i * (500 + Math.random() * 1000);
      monsters.push(this.createMonster(text, this.getRandomMonsterType(), spawnDelay));
    }
    
    return monsters;
  }

  // 创建怪物
  private createMonster(text: string, type: MonsterType, spawnDelay: number = 0): Monster {
    const x = Math.random() * (this.canvas.width - 100) + 50;
    const y = 50; // 从顶部固定位置开始
    const baseSpeed = 0.8 + this.currentRound * 0.08; // 速度随轮数增加
    
    // 根据准确率调整速度
    let speed = baseSpeed;
    if (this.stats.currentAccuracy >= 85) {
      speed += 0.2;
    } else if (this.stats.currentAccuracy <= 40) {
      speed -= 0.3;
    }
    
    // 计算朝向玩家的方向向量
    const playerX = this.canvas.width / 2;
    const playerY = this.playerY;
    const dx = playerX - x;
    const dy = playerY - y;
    const distance = Math.sqrt(dx * dx + dy * dy);
    
    // 归一化方向向量并乘以速度
    const vx = (dx / distance) * speed;
    const vy = (dy / distance) * speed;
    
    // 根据文本长度计算碰撞半径
    const radius = Math.max(30, text.length * 8);
    
    return {
      id: `${Date.now()}-${Math.random()}`,
      text,
      type,
      x,
      y,
      vx,
      vy,
      speed: Math.max(0.3, speed),
      radius,
      health: text.length,
      maxHealth: text.length,
      currentIndex: 0,
      isAlive: true,
      spawnTime: Date.now(),
      spawnDelay,
      isSpawned: spawnDelay === 0,
      errorCount: 0,
    };
  }

  // 获取随机怪物类型
  private getRandomMonsterType(): MonsterType {
    const rand = Math.random();
    if (rand < 0.6) return 'NORMAL'; // 60%
    if (rand < 0.75) return 'EVASIVE'; // 15%
    if (rand < 0.9) return 'ATTACKER'; // 15%
    return 'SMART'; // 10%
  }

  // 开始新一轮
  startNewRound(): void {
    this.isWaitingForNextRound = true; // 设置等待标志
    this.currentRound++;
    this.stats.rounds = this.currentRound; // 更新统计中的轮数
    
    let newMonsters: Monster[];
    if (this.currentRound === 1) {
      newMonsters = this.generateInitialMonsters();
    } else if (this.config.gameMode === 'endless') {
      newMonsters = this.generateAdaptiveMonsters();
    } else {
      newMonsters = this.generateDictionaryMonsters();
    }
    
    this.monsters = newMonsters;
    
    this.roundData.push({
      roundNumber: this.currentRound,
      monsters: [...newMonsters],
      startTime: Date.now(),
      accuracy: this.stats.currentAccuracy,
      speed: this.stats.currentSpeed,
    });
    
    // 延迟重置等待标志，确保不会立即再次触发
    setTimeout(() => {
      this.isWaitingForNextRound = false;
    }, 3000);
  }

  // 处理玩家输入
  handleInput(char: string, lockedMonsterId: string | null): {
    success: boolean;
    monsterId?: string;
    completed?: boolean;
  } {
    this.stats.totalCharacters++;
    
    const monster = this.monsters.find(m => m.id === lockedMonsterId && m.isAlive);
    
    if (!monster) {
      this.stats.errorCharacters++;
      this.stats.consecutiveCorrect = 0;
      this.updateCharacterStats(char, false);
      return { success: false };
    }
    
    // 获取当前需要输入的字符
    const expectedChar = monster.text[monster.currentIndex];
    
    // EVASIVE类型有30%概率规避攻击
    if (monster.type === 'EVASIVE' && Math.random() < 0.3) {
      this.stats.errorCharacters++;
      monster.errorCount++;
      this.stats.consecutiveCorrect = 0;
      this.updateCharacterStats(char, false);
      return { success: false, monsterId: monster.id };
    }
    
    // 比较输入字符与期望字符
    if (char === expectedChar) {
      // ✅ 输入正确：消除对应字符
      this.stats.correctCharacters++;
      this.stats.consecutiveCorrect++;
      this.stats.maxConsecutiveCorrect = Math.max(
        this.stats.maxConsecutiveCorrect,
        this.stats.consecutiveCorrect
      );
      
      // 移动到下一个字符，减少生命值
      monster.currentIndex++;
      monster.health--;
      
      this.updateCharacterStats(char, true);
      
      // 检查怪物是否被完全消灭（所有字符都被输入）
      if (monster.health <= 0) {
        monster.isAlive = false;
        monster.killTime = Date.now();
        this.stats.score += this.getMonsterScore(monster.type);
        return { success: true, monsterId: monster.id, completed: true };
      }
      
      return { success: true, monsterId: monster.id, completed: false };
    } else {
      // ❌ 输入错误：怪物不受影响
      this.stats.errorCharacters++;
      monster.errorCount++;
      this.stats.consecutiveCorrect = 0;
      this.updateCharacterStats(char, false);
      return { success: false, monsterId: monster.id };
    }
  }

  // 获取怪物分数
  private getMonsterScore(type: MonsterType): number {
    switch (type) {
      case 'NORMAL': return 1;
      case 'EVASIVE': return 2;
      case 'ATTACKER': return 3;
      case 'SMART': return 3;
      default: return 1;
    }
  }

  // 更新字符统计
  private updateCharacterStats(char: string, isCorrect: boolean): void {
    const existing = this.weakCharacters.get(char) || { errorCount: 0, accuracy: 100 };
    
    if (!isCorrect) {
      existing.errorCount++;
    }
    
    // 简单的准确率计算
    const totalAttempts = (existing.errorCount / (1 - existing.accuracy / 100)) || 1;
    const correctAttempts = isCorrect ? totalAttempts - existing.errorCount + 1 : totalAttempts - existing.errorCount;
    existing.accuracy = (correctAttempts / (totalAttempts + 1)) * 100;
    
    this.weakCharacters.set(char, existing);
    
    this.characterHistory.push({
      character: char,
      isCorrect,
      timestamp: Date.now(),
    });
  }

  // 碰撞检测和分离
  private resolveCollisions(): void {
    const spawnedMonsters = this.monsters.filter(m => m.isAlive && m.isSpawned);
    
    // 检查每对怪物之间的碰撞
    for (let i = 0; i < spawnedMonsters.length; i++) {
      for (let j = i + 1; j < spawnedMonsters.length; j++) {
        const m1 = spawnedMonsters[i];
        const m2 = spawnedMonsters[j];
        
        const dx = m2.x - m1.x;
        const dy = m2.y - m1.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        const minDistance = m1.radius + m2.radius;
        
        // 如果发生碰撞
        if (distance < minDistance && distance > 0) {
          // 计算重叠量
          const overlap = minDistance - distance;
          
          // 归一化方向向量
          const nx = dx / distance;
          const ny = dy / distance;
          
          // 分离怪物（各移动一半的重叠量）
          const separationX = nx * overlap * 0.5;
          const separationY = ny * overlap * 0.5;
          
          m1.x -= separationX;
          m1.y -= separationY;
          m2.x += separationX;
          m2.y += separationY;
        }
      }
    }
  }

  // 更新游戏状态
  update(deltaTime: number): boolean {
    const now = Date.now();
    
    // 检查并激活延迟生成的怪物
    for (const monster of this.monsters) {
      if (!monster.isSpawned && now - monster.spawnTime >= monster.spawnDelay) {
        monster.isSpawned = true;
      }
    }
    
    // 更新所有已生成怪物的位置
    for (const monster of this.monsters) {
      if (!monster.isAlive || !monster.isSpawned) continue;
      
      // 使用速度向量更新位置（斜线移动）
      monster.x += monster.vx * deltaTime / 16;
      monster.y += monster.vy * deltaTime / 16;
      
      // ATTACKER类型生成新怪物
      if (monster.type === 'ATTACKER') {
        if (!monster.lastAttackTime) {
          monster.lastAttackTime = now;
        }
        
        const timeSinceLastAttack = now - monster.lastAttackTime;
        
        // 每5秒生成1个普通小怪
        if (timeSinceLastAttack >= 5000 && timeSinceLastAttack < 5100) {
          const newMonster = this.createMonster(
            this.generateRandomString(1, this.getEnabledCharacters()),
            'NORMAL',
            0
          );
          newMonster.x = monster.x + (Math.random() - 0.5) * 100;
          newMonster.y = monster.y;
          // 重新计算朝向玩家的方向
          const dx = this.canvas.width / 2 - newMonster.x;
          const dy = this.playerY - newMonster.y;
          const distance = Math.sqrt(dx * dx + dy * dy);
          newMonster.vx = (dx / distance) * newMonster.speed;
          newMonster.vy = (dy / distance) * newMonster.speed;
          this.monsters.push(newMonster);
        }
        
        // 每15秒生成8个单字符小怪
        if (timeSinceLastAttack >= 15000 && timeSinceLastAttack < 15100) {
          for (let i = 0; i < 8; i++) {
            const newMonster = this.createMonster(
              this.generateRandomString(1, this.getEnabledCharacters()),
              'NORMAL',
              i * 100
            );
            newMonster.x = monster.x + (Math.random() - 0.5) * 200;
            newMonster.y = monster.y;
            // 重新计算朝向玩家的方向
            const dx = this.canvas.width / 2 - newMonster.x;
            const dy = this.playerY - newMonster.y;
            const distance = Math.sqrt(dx * dx + dy * dy);
            newMonster.vx = (dx / distance) * newMonster.speed;
            newMonster.vy = (dy / distance) * newMonster.speed;
            this.monsters.push(newMonster);
          }
          monster.lastAttackTime = now;
        }
      }
      
      // SMART类型吸引周围怪物
      if (monster.type === 'SMART') {
        for (const other of this.monsters) {
          if (other.id === monster.id || !other.isAlive || !other.isSpawned || other.type !== 'NORMAL') continue;
          
          const dx = monster.x - other.x;
          const dy = monster.y - other.y;
          const distance = Math.sqrt(dx * dx + dy * dy);
          
          if (distance < 150 && distance > 0) {
            other.x += (dx / distance) * 0.5;
          }
        }
      }
      
      // 检查是否到达玩家
      const distToPlayer = Math.sqrt(
        Math.pow(monster.x - this.canvas.width / 2, 2) + 
        Math.pow(monster.y - this.playerY, 2)
      );
      if (distToPlayer < monster.radius + 20) {
        return true; // 游戏结束
      }
    }
    
    // 碰撞检测和分离
    this.resolveCollisions();

    
    // 更新统计数据
    this.updateStats();
    
    // 检查是否所有怪物都被消灭
    const allDead = this.monsters.every(m => !m.isAlive);
    if (allDead && this.monsters.length > 0) {
      // 结束当前轮
      const currentRoundData = this.roundData[this.roundData.length - 1];
      if (currentRoundData && !currentRoundData.endTime) {
        currentRoundData.endTime = Date.now();
      }
    }
    
    return false;
  }

  // 更新统计数据
  private updateStats(): void {
    const elapsedMinutes = (Date.now() - this.stats.startTime) / 60000;
    this.stats.currentSpeed = this.stats.correctCharacters / elapsedMinutes;
    this.stats.currentAccuracy = this.stats.totalCharacters > 0
      ? (this.stats.correctCharacters / this.stats.totalCharacters) * 100
      : 100;
    
    this.stats.peakSpeed = Math.max(this.stats.peakSpeed, this.stats.currentSpeed);
    this.stats.peakAccuracy = Math.max(this.stats.peakAccuracy, this.stats.currentAccuracy);
  }

  // 渲染游戏
  render(): void {
    // 清空画布并绘制背景
    this.ctx.fillStyle = '#1a1a2e';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
    
    // 绘制网格背景
    this.ctx.strokeStyle = 'rgba(33, 150, 243, 0.1)';
    this.ctx.lineWidth = 1;
    for (let i = 0; i < this.canvas.width; i += 50) {
      this.ctx.beginPath();
      this.ctx.moveTo(i, 0);
      this.ctx.lineTo(i, this.canvas.height);
      this.ctx.stroke();
    }
    for (let i = 0; i < this.canvas.height; i += 50) {
      this.ctx.beginPath();
      this.ctx.moveTo(0, i);
      this.ctx.lineTo(this.canvas.width, i);
      this.ctx.stroke();
    }
    
    // 渲染怪物
    for (const monster of this.monsters) {
      if (!monster.isAlive || !monster.isSpawned) continue;
      this.renderMonster(monster);
    }
    
    // 渲染玩家
    this.renderPlayer();
    
    // 渲染游戏提示
    if (this.monsters.length === 0) {
      this.ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
      this.ctx.font = 'bold 32px Arial';
      this.ctx.textAlign = 'center';
      this.ctx.textBaseline = 'middle';
      this.ctx.fillText('准备下一轮...', this.canvas.width / 2, this.canvas.height / 2);
    }
  }

  // 渲染怪物
  private renderMonster(monster: Monster): void {
    const { x, y, text, type, currentIndex, health, maxHealth, radius } = monster;
    
    // 设置颜色
    let color: string;
    switch (type) {
      case 'NORMAL': color = '#2196F3'; break;
      case 'ATTACKER': color = '#F44336'; break;
      case 'EVASIVE': color = '#9C27B0'; break;
      case 'SMART': color = '#00E676'; break;
    }
    
    // 绘制碰撞圆圈（半透明，用于调试）
    this.ctx.strokeStyle = `${color}40`;
    this.ctx.lineWidth = 2;
    this.ctx.beginPath();
    this.ctx.arc(x, y, radius, 0, Math.PI * 2);
    this.ctx.stroke();
    
    // 绘制血条背景
    const barWidth = Math.max(text.length * 12, 60);
    const barHeight = 6;
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
    this.ctx.fillRect(x - barWidth / 2, y - radius - 10, barWidth, barHeight);
    
    // 绘制血条
    this.ctx.fillStyle = color;
    this.ctx.fillRect(x - barWidth / 2, y - radius - 10, (health / maxHealth) * barWidth, barHeight);
    
    // 绘制文字 - 只显示未输入的字符
    this.ctx.font = 'bold 24px "Courier New", monospace';
    this.ctx.textAlign = 'left';
    this.ctx.textBaseline = 'middle';
    this.ctx.fillStyle = color;
    
    const charWidth = 14; // 每个字符的宽度
    const remainingText = text.slice(currentIndex); // 只取未输入的部分
    const totalWidth = remainingText.length * charWidth;
    const startX = x - totalWidth / 2;
    
    for (let i = 0; i < remainingText.length; i++) {
      const char = remainingText[i];
      const charX = startX + i * charWidth;
      this.ctx.fillText(char, charX, y);
    }
    
    // 绘制类型标识
    this.ctx.font = 'bold 10px Arial';
    this.ctx.fillStyle = color;
    this.ctx.textAlign = 'center';
    let typeText = '';
    switch (type) {
      case 'NORMAL': typeText = '普通'; break;
      case 'ATTACKER': typeText = '攻击'; break;
      case 'EVASIVE': typeText = '回避'; break;
      case 'SMART': typeText = '智慧'; break;
    }
    this.ctx.fillText(typeText, x, y - radius - 20);
  }

  // 渲染玩家
  private renderPlayer(): void {
    const x = this.canvas.width / 2;
    const y = this.playerY;
    
    // 绘制玩家底座
    this.ctx.fillStyle = '#424242';
    this.ctx.fillRect(x - 40, y + 10, 80, 10);
    
    // 绘制玩家主体（圆形）
    const gradient = this.ctx.createRadialGradient(x, y, 0, x, y, 20);
    gradient.addColorStop(0, '#2196F3');
    gradient.addColorStop(1, '#1976D2');
    this.ctx.fillStyle = gradient;
    this.ctx.beginPath();
    this.ctx.arc(x, y, 20, 0, Math.PI * 2);
    this.ctx.fill();
    
    // 绘制边框
    this.ctx.strokeStyle = '#00E676';
    this.ctx.lineWidth = 2;
    this.ctx.stroke();
    
    // 绘制键盘图标
    this.ctx.fillStyle = 'white';
    this.ctx.font = 'bold 20px Arial';
    this.ctx.textAlign = 'center';
    this.ctx.textBaseline = 'middle';
    this.ctx.fillText('⌨', x, y);
  }

  // 获取当前统计数据
  getStats(): GameStats {
    return { ...this.stats };
  }

  // 获取当前怪物列表
  getMonsters(): Monster[] {
    return this.monsters;
  }

  // 获取字符统计
  getCharacterStats(): Map<string, { errorCount: number; accuracy: number }> {
    return new Map(this.weakCharacters);
  }

  // 获取轮次数据
  getRoundData(): RoundData[] {
    return [...this.roundData];
  }

  // 检查是否可以开始新一轮
  canStartNewRound(): boolean {
    // 只有当所有怪物都被消灭且不在等待状态时才能开始新轮次
    const allMonstersDefeated = this.monsters.every(m => !m.isAlive);
    return allMonstersDefeated && !this.isWaitingForNextRound;
  }

  // 清理
  destroy(): void {
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
    }
  }
}
