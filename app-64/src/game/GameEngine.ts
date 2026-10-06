// 游戏引擎核心
import { MonsterEntity } from './Monster';
import { MonsterGenerator } from './MonsterGenerator';
import { InputHandler } from './InputHandler';
import { AdaptiveSystem } from './AdaptiveSystem';
import type {
  GameState,
  GameStats,
  GameConfig,
  AdaptiveConfig,
  MonsterSpawnConfig,
  PlayerState,
  GameEvent,
  GameEventHandler,
  CharacterStat,
  MonsterType,
} from './types';

/** 后端下发的本轮怪物项（词源界定等模式用） */
export interface RoundMonsterItem {
  targetStr?: string;
  speed?: number;
  monsterType?: string;
}

export class GameEngine {
  // 配置
  private config: GameConfig;
  private adaptiveConfig: AdaptiveConfig;
  private spawnConfig: MonsterSpawnConfig;

  // 游戏状态
  private state: GameState;
  private monsters: MonsterEntity[];
  private player: PlayerState;
  private stats: GameStats;

  // 系统
  private generator: MonsterGenerator;
  private inputHandler: InputHandler;
  private adaptiveSystem: AdaptiveSystem;

  // 时间管理
  private lastFrameTime: number;
  private lastSpawnTime: number;
  private animationFrameId: number | null;

  // 事件处理
  private eventHandlers: GameEventHandler[];

  // 字符池
  private characterPool: string[];

  // 怪物历史记录
  private monsterHistory: any[] = [];
  private spawnedMinionsThisRound = 0;
  private static readonly MAX_MINIONS_PER_ROUND = 16;

  /** 本轮需生成的怪物总数（由模型/后端调节）；无尽/词源模式：全部消灭后进入下一轮 */
  private roundMonsterTotal: number = 0;
  /** 本轮已生成的怪物数（仅计主怪，ATTACKER 小怪不计） */
  private roundMonstersSpawned: number = 0;
  /** 词源界定模式：后端下发的本轮怪物列表（targetStr 等），按序消耗 */
  private roundMonstersQueue: RoundMonsterItem[] = [];
  private monsterIdCounter: number = 0;

  constructor(
    config: GameConfig,
    adaptiveConfig: AdaptiveConfig,
    spawnConfig: MonsterSpawnConfig,
    characterPool: string[],
    initialRoundMonsters?: RoundMonsterItem[]
  ) {
    this.config = config;
    this.adaptiveConfig = adaptiveConfig;
    this.spawnConfig = spawnConfig;
    this.characterPool = characterPool;

    this.state = 'READY';
    this.monsters = [];
    this.player = {
      x: config.canvasWidth / 2,
      y: config.playerY,
      isAlive: true,
    };
    this.stats = this.initStats();

    this.generator = new MonsterGenerator(spawnConfig, characterPool);
    this.inputHandler = new InputHandler(this.handleGameEvent.bind(this));
    this.adaptiveSystem = new AdaptiveSystem(
      adaptiveConfig,
      config.baseSpeed,
      config.maxMonsters,
      this.handleGameEvent.bind(this)
    );

    this.lastFrameTime = 0;
    this.lastSpawnTime = 0;
    this.animationFrameId = null;
    this.eventHandlers = [];
    if (initialRoundMonsters != null && initialRoundMonsters.length > 0) {
      this.roundMonstersQueue = [...initialRoundMonsters];
      this.roundMonsterTotal = initialRoundMonsters.length;
    }
  }

  /** 设置下一轮怪物列表（词源界定模式在 ROUND_COMPLETE 后由 GamePage 调用） */
  setRoundMonsters(monsters: RoundMonsterItem[]): void {
    this.roundMonstersQueue = monsters != null ? [...monsters] : [];
    if (this.roundMonstersQueue.length > 0) {
      this.roundMonsterTotal = this.roundMonstersQueue.length;
    }
  }

  // 初始化统计
  private initStats(): GameStats {
    return {
      mode: this.config.mode,
      score: 0,
      round: 1,
      totalChars: 0,
      correctChars: 0,
      errorChars: 0,
      accuracy: 100,
      speed: 0,
      startTime: Date.now(),
      currentTime: Date.now(),
      monstersKilled: 0,
      longestStreak: 0,
      currentStreak: 0,
      timeRemaining: this.config.timeLimitSeconds,
      survivalTime: 0,
    };
  }

  // 开始游戏
  start(): void {
    if (this.state !== 'READY' && this.state !== 'PAUSED') return;

    this.state = 'PLAYING';
    this.stats.startTime = Date.now();
    this.lastFrameTime = performance.now();
    this.lastSpawnTime = Date.now();
    this.roundMonsterTotal = this.config.maxMonsters;
    this.roundMonstersSpawned = 0;

    // 开始游戏循环
    this.gameLoop();

    // 生成初始怪兽
    this.spawnInitialMonsters();
  }

  // 暂停游戏
  pause(): void {
    if (this.state !== 'PLAYING') return;
    this.state = 'PAUSED';
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }
  }

  // 恢复游戏
  resume(): void {
    if (this.state !== 'PAUSED') return;
    this.state = 'PLAYING';
    this.lastFrameTime = performance.now();
    this.gameLoop();
  }

  // 游戏循环
  private gameLoop = (): void => {
    if (this.state !== 'PLAYING') return;

    const currentTime = performance.now();
    const deltaTime = currentTime - this.lastFrameTime;
    this.lastFrameTime = currentTime;

    // 更新游戏
    this.update(deltaTime);

    // 继续循环
    this.animationFrameId = requestAnimationFrame(this.gameLoop);
  };

  // 更新游戏
  private update(deltaTime: number): void {
    this.stats.currentTime = Date.now();

    // 处理游戏模式逻辑
    this.handleGameMode();

    // 更新怪兽
    this.updateMonsters(deltaTime);

    // 生成新怪兽
    this.spawnMonsters();

    // 检查碰撞
    this.checkCollisions();

    // 更新统计
    this.updateStats();

    // 检查轮次结束
    this.checkRoundComplete();
  }

  // 处理游戏模式逻辑
  private handleGameMode(): void {
    const elapsed = Date.now() - this.stats.startTime;

    switch (this.config.mode) {
      case 'TIMED':
        // 限时模式：倒计时
        if (this.config.timeLimitSeconds) {
          const remaining = (this.config.timeLimitSeconds * 1000) - elapsed;
          this.stats.timeRemaining = Math.max(0, remaining / 1000);
          
          // 时间到，游戏结束
          if (remaining <= 0) {
            this.gameOver();
          }
        }
        break;

      case 'SURVIVAL':
        // 生存模式：速度递增
        const survivalTime = elapsed / 1000;
        this.stats.survivalTime = survivalTime;
        
        // 每30秒增加10%速度
        const speedMultiplier = 1 + (Math.floor(survivalTime / 30) * (this.config.survivalSpeedIncrease || 0.1));
        this.adaptiveSystem.setSpeedMultiplier(speedMultiplier);
        break;

      case 'ENDLESS':
        // 无尽模式：保持现有逻辑
        break;
    }
  }

  // 更新怪兽
  private updateMonsters(deltaTime: number): void {
    for (const monster of this.monsters) {
      if (!monster.isAlive) continue;

      // 更新位置 - 向玩家移动
      monster.update(deltaTime, this.player.x, this.player.y);

      // 检查ATTACKER类型的特殊行为
      if (monster.type === 'ATTACKER') {
        // 生成小怪
        if (monster.shouldSpawnMinion()) {
          this.spawnMinion(monster);
        }

        // 爆发生成多个小怪
        if (monster.shouldBurst()) {
          this.spawnBurst(monster);
        }
      }

      // 检查SMART类型的特殊行为
      if (monster.type === 'SMART') {
        this.attractNearbyMonsters(monster);
      }
    }

    // 处理怪兽间碰撞
    this.handleMonsterCollisions();

    // 移除死亡的怪兽（立即移除）
    this.monsters = this.monsters.filter(m => m.isAlive);
  }

  // 处理怪兽间碰撞
  private handleMonsterCollisions(): void {
    const aliveMonsters = this.monsters.filter(m => m.isAlive);
    
    for (let i = 0; i < aliveMonsters.length; i++) {
      for (let j = i + 1; j < aliveMonsters.length; j++) {
        const m1 = aliveMonsters[i];
        const m2 = aliveMonsters[j];
        
        if (m1.isCollidingWithMonster(m2)) {
          // 获取推开向量
          const repulsion1 = m1.getRepulsionVector(m2);
          const repulsion2 = m2.getRepulsionVector(m1);
          
          // 推开力度（较小的值，避免过度推开）
          const pushForce = 2;
          
          // 应用推开力
          m1.x += repulsion1.x * pushForce;
          m1.y += repulsion1.y * pushForce;
          m2.x += repulsion2.x * pushForce;
          m2.y += repulsion2.y * pushForce;
          
          // 确保怪兽不会被推出边界
          m1.x = Math.max(50, Math.min(this.config.canvasWidth - 50, m1.x));
          m1.y = Math.max(50, Math.min(this.config.canvasHeight - 50, m1.y));
          m2.x = Math.max(50, Math.min(this.config.canvasWidth - 50, m2.x));
          m2.y = Math.max(50, Math.min(this.config.canvasHeight - 50, m2.y));
        }
      }
    }
  }

  // 生成怪兽
  private spawnMonsters(): void {
    const now = Date.now();
    const timeSinceLastSpawn = now - this.lastSpawnTime;

    // 检查是否需要生成
    if (timeSinceLastSpawn < this.config.spawnInterval) return;

    // 检查数量限制
    const aliveMonsters = this.monsters.filter(m => m.isAlive).length;
    const maxMonsters = this.adaptiveSystem.getCurrentMaxMonsters();
    if (aliveMonsters >= maxMonsters) return;

    // 无尽/词源模式：本轮总数由模型固定，达到后不再生成，等全部消灭后进入下一轮
    if (this.config.mode === 'ENDLESS' && this.roundMonstersSpawned >= this.roundMonsterTotal) return;

    const monster = this.spawnOneMonster();
    if (!monster) return;

    // 检查位置冲突，最多尝试5次
    let attempts = 0;
    while (attempts < 5 && this.hasPositionConflict(monster)) {
      monster.x = Math.random() * (this.config.canvasWidth - 100) + 50;
      attempts++;
    }

    this.monsters.push(monster);
    this.lastSpawnTime = now;
    if (this.config.mode === 'ENDLESS') this.roundMonstersSpawned++;

    // 发送事件
    this.emitEvent({
      type: 'MONSTER_SPAWNED',
      monster: monster.toJSON(),
    });
  }

  // 检查位置是否与现有怪兽冲突
  private hasPositionConflict(newMonster: MonsterEntity): boolean {
    for (const monster of this.monsters) {
      if (!monster.isAlive) continue;
      if (newMonster.isCollidingWithMonster(monster)) {
        return true;
      }
    }
    return false;
  }

  // 生成初始怪兽
  private spawnInitialMonsters(): void {
    const count = Math.min(3, this.config.maxMonsters);
    for (let i = 0; i < count; i++) {
      const monster = this.spawnOneMonster();
      if (monster) {
        monster.y = -100 - i * 100; // 错开位置
        this.monsters.push(monster);
      }
    }
    if (this.config.mode === 'ENDLESS') this.roundMonstersSpawned += count;
  }

  /** 生成一只怪兽：有后端下发的本轮列表则用其 targetStr/速度/类型，否则用生成器 */
  private spawnOneMonster(): MonsterEntity | null {
    if (this.roundMonstersQueue.length > 0) {
      const item = this.roundMonstersQueue.shift()!;
      const text = (item.targetStr != null && item.targetStr.trim() !== '') ? item.targetStr.trim() : null;
      if (text == null) return this.generator.generate(this.config.canvasWidth, this.adaptiveSystem.getCurrentSpeed());
      const type: MonsterType = (item.monsterType as MonsterType) || 'NORMAL';
      const speed = item.speed != null ? item.speed : this.adaptiveSystem.getCurrentSpeed();
      const size = this.generator.calculateSize(type);
      const x = Math.random() * (this.config.canvasWidth - 100) + 50;
      const y = -50;
      const monster = new MonsterEntity(
        `monster-${this.monsterIdCounter++}`,
        text,
        type,
        x,
        y,
        speed,
        size
      );
      return monster;
    }
    return this.generator.generate(this.config.canvasWidth, this.adaptiveSystem.getCurrentSpeed());
  }

  // 生成小怪（ATTACKER类型）
  private spawnMinion(parent: MonsterEntity): void {
    if (this.spawnedMinionsThisRound >= GameEngine.MAX_MINIONS_PER_ROUND) return;
    const speed = this.adaptiveSystem.getCurrentSpeed();
    const minion = this.generator.generate(this.config.canvasWidth, speed);
    
    // 设置为普通类型
    minion.type = 'NORMAL';
    minion.size = 0.8;
    
    // 在父怪兽位置生成
    minion.x = parent.x;
    minion.y = parent.y;
    
    // 随机生成一个向外的激射方向
    const angle = Math.random() * Math.PI * 2;
    const ejectionDirection = {
      x: Math.cos(angle),
      y: Math.sin(angle)
    };
    
    // 设置激射效果：向外激射120-150像素
    const ejectionDistance = 120 + Math.random() * 30;
    minion.setEjection(ejectionDirection, ejectionDistance);
    minion.isRoundMonster = false;

    this.monsters.push(minion);
    this.spawnedMinionsThisRound++;
  }

  // 爆发生成多个小怪（ATTACKER类型）
  private spawnBurst(parent: MonsterEntity): void {
    const count = Math.min(8, GameEngine.MAX_MINIONS_PER_ROUND - this.spawnedMinionsThisRound);
    for (let i = 0; i < count; i++) {
      const char = this.characterPool[Math.floor(Math.random() * this.characterPool.length)] || 'a';
      const speed = this.adaptiveSystem.getCurrentSpeed();
      
      const minion = new MonsterEntity(
        `minion-${Date.now()}-${i}`,
        char,
        'NORMAL',
        parent.x, // 从母体位置开始
        parent.y,
        speed * 1.2,
        0.6
      );
      
      // 均匀分布的激射方向（360度分成8份）
      const angle = (Math.PI * 2 * i) / count;
      const ejectionDirection = {
        x: Math.cos(angle),
        y: Math.sin(angle)
      };
      
      // 设置激射效果：向外激射100-130像素
      const ejectionDistance = 100 + Math.random() * 30;
      minion.setEjection(ejectionDirection, ejectionDistance);
      minion.isRoundMonster = false;

      this.monsters.push(minion);
      this.spawnedMinionsThisRound++;
    }
  }

  // 吸引附近怪兽（SMART类型）
  private attractNearbyMonsters(smart: MonsterEntity): void {
    const attractRadius = 100;
    
    for (const monster of this.monsters) {
      if (monster === smart || !monster.isAlive) continue;
      if (monster.type !== 'NORMAL') continue;

      const dx = smart.x - monster.x;
      const dy = smart.y - monster.y;
      const distance = Math.sqrt(dx * dx + dy * dy);

      if (distance < attractRadius) {
        // 水平偏移
        const offset = dx > 0 ? 1 : -1;
        monster.x += offset;
      }
    }
  }

  // 检查碰撞 - 检测怪兽是否触碰玩家
  private checkCollisions(): void {
    for (const monster of this.monsters) {
      if (!monster.isAlive) continue;

      // 检查是否与玩家碰撞
      if (monster.isCollidingWithPlayer(this.player.x, this.player.y)) {
        this.gameOver();
        return;
      }
    }
  }

  // 更新统计
  private updateStats(): void {
    // 计算准确率
    const total = this.stats.correctChars + this.stats.errorChars;
    this.stats.accuracy = total > 0 ? (this.stats.correctChars / total) * 100 : 100;

    // 计算速度（字符/分钟）
    const elapsedMinutes = (this.stats.currentTime - this.stats.startTime) / 60000;
    this.stats.speed = elapsedMinutes > 0 ? this.stats.correctChars / elapsedMinutes : 0;
  }

  // 检查轮次结束
  private checkRoundComplete(): void {
    if (this.config.mode === 'ENDLESS') {
      // 无尽/词源模式：本轮固定数量怪物全部消灭后进入下一轮，不用时间界定
      const roundMonstersAlive = this.monsters.filter(m => m.isAlive && m.isRoundMonster).length;
      if (this.roundMonstersSpawned >= this.roundMonsterTotal && roundMonstersAlive === 0) {
        this.monsters = this.monsters.filter(m => m.isRoundMonster);
        this.completeRound();
      }
      return;
    }
    // 限时/生存等模式：保留时间判定（若需要）
    const elapsedTime = this.stats.currentTime - this.stats.startTime;
    const roundDuration = this.config.roundDuration;
    if (elapsedTime >= roundDuration * this.stats.round) {
      this.completeRound();
    }
  }

  // 完成轮次
  private completeRound(): void {
    // 发送事件（GamePage 会拉取下一轮参数并 setRoundMonsterTotal / applyAdjustParam）
    this.emitEvent({
      type: 'ROUND_COMPLETE',
      round: this.stats.round,
      stats: { ...this.stats },
    });

    // 进入下一轮；下一轮总数由 applyAdjustParam / setRoundMonsterTotal 设置
    this.stats.round++;
    this.roundMonstersSpawned = 0;
    this.spawnedMinionsThisRound = 0;
  }

  // 游戏结束
  private gameOver(): void {
    this.state = 'GAME_OVER';
    this.player.isAlive = false;

    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }

    // 发送事件
    this.emitEvent({
      type: 'GAME_OVER',
      stats: { ...this.stats },
    });
  }

  // 处理键盘输入
  handleKeyDown(event: KeyboardEvent): void {
    if (this.state !== 'PLAYING') return;

    const success = this.inputHandler.handleKeyDown(event, this.monsters);
    
    // 更新统计
    if (success) {
      this.stats.correctChars++;
      this.stats.currentStreak++;
      this.stats.longestStreak = Math.max(this.stats.longestStreak, this.stats.currentStreak);
      
      // 触发正确输入事件
      this.emitEvent({
        type: 'INPUT_CORRECT',
        char: event.key,
      });
    } else {
      this.stats.errorChars++;
      this.stats.currentStreak = 0;
    }
    
    this.stats.totalChars++;
  }

  // 处理键盘释放
  handleKeyUp(event: KeyboardEvent): void {
    this.inputHandler.handleKeyUp(event);
  }

  // 处理游戏事件
  private handleGameEvent(event: GameEvent): void {
    // 更新自适应系统
    if (event.type === 'MONSTER_HIT') {
      this.adaptiveSystem.recordCharacterInput(event.char, true);
    } else if (event.type === 'INPUT_ERROR') {
      // 记录目标字符（易错字统计应为“该打对却打错的字”；无目标时不记录）
      if (event.expected != null && event.expected !== '') {
        this.adaptiveSystem.recordCharacterInput(event.expected, false);
      }
    } else if (event.type === 'MONSTER_KILLED') {
      this.stats.score += event.score;
      this.stats.monstersKilled++;
      this.recordMonster(event.monster, 'KILLED', event.score);
    } else if (event.type === 'MONSTER_EVADED') {
      this.recordMonster(event.monster, 'EVADED', 0);
    }

    // 转发事件
    this.emitEvent(event);
  }

  // 记录怪物数据
  private recordMonster(monster: any, outcome: 'KILLED' | 'EVADED', score: number): void {
    const survivalTime = Date.now() - monster.createdAt;
    
    this.monsterHistory.push({
      monsterType: monster.type,
      targetStr: monster.text,
      speed: monster.speed,
      survivalTime: survivalTime,
      destroyCostTime: outcome === 'KILLED' ? survivalTime : null,
      // 输入总次数包含正确输入和重复错误输入，避免错误率被低估。
      totalCount: monster.text.length + (monster.errorCount || 0),
      inputErrorCount: monster.errorCount || 0,
      isTrueMean: 0, // 暂时默认为0，后续可根据词库判断
      score: score,
      difficultyLevel: 1, // 暂时默认为1
      spawnWave: this.stats.round,
       isParent: false, // 暂时默认为false
       result: outcome
    });
  }

  // 获取怪物历史记录
  getMonsterHistory(): any[] {
    return [...this.monsterHistory];
  }

  // 添加事件处理器
  addEventListener(handler: GameEventHandler): void {
    this.eventHandlers.push(handler);
  }

  // 移除事件处理器
  removeEventListener(handler: GameEventHandler): void {
    this.eventHandlers = this.eventHandlers.filter(h => h !== handler);
  }

  // 发送事件
  private emitEvent(event: GameEvent): void {
    for (const handler of this.eventHandlers) {
      handler(event);
    }
  }

  // 获取游戏状态
  getState(): GameState {
    return this.state;
  }

  // 获取怪兽列表
  getMonsters(): MonsterEntity[] {
    return this.monsters;
  }

  // 获取玩家状态
  getPlayer(): PlayerState {
    return this.player;
  }

  // 获取统计数据
  getStats(): GameStats {
    return { ...this.stats };
  }

  // 获取输入处理器
  getInputHandler(): InputHandler {
    return this.inputHandler;
  }

  // 获取自适应系统
  getAdaptiveSystem(): AdaptiveSystem {
    return this.adaptiveSystem;
  }

  /** 应用下一轮参数（由 next-round 接口返回），用于每轮难度串联 */
  applyAdjustParam(speed: number, maxMonsters: number): void {
    this.adaptiveSystem.setCurrentSpeed(speed);
    this.adaptiveSystem.setCurrentMaxMonsters(maxMonsters);
    if (this.roundMonstersQueue.length === 0) {
      this.roundMonsterTotal = maxMonsters;
    }
  }

  // 获取字符统计
  getCharacterStats(): CharacterStat[] {
    return this.adaptiveSystem.getAllCharacterStats();
  }

  // 销毁
  destroy(): void {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }
    this.monsters = [];
    this.eventHandlers = [];
  }
}
