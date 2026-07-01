// 怪兽类
import type { Monster, MonsterType } from './types';

export class MonsterEntity implements Monster {
  id: string;
  text: string;
  type: MonsterType;
  x: number;
  y: number;
  speed: number;
  size: number;
  currentIndex: number;
  isAlive: boolean;
  isLocked: boolean;
  createdAt: number;
  eliminatedAt?: number;
  errorCount: number;
  lastSpawnTime?: number;
  lastBurstTime?: number;
  evadeChance: number;
  
  // 激射相关属性
  isEjecting: boolean;
  ejectionDirection: { x: number; y: number } | null;
  ejectionDistance: number;
  ejectionTargetDistance: number;

  /** 是否计入本轮数量（本轮固定数量全部消灭后进入下一轮；ATTACKER 小怪不计入） */
  isRoundMonster: boolean;

  constructor(
    id: string,
    text: string,
    type: MonsterType,
    x: number,
    y: number,
    speed: number,
    size: number = 1
  ) {
    this.id = id;
    this.text = text;
    this.type = type;
    this.x = x;
    this.y = y;
    this.speed = speed;
    this.size = size;
    this.currentIndex = 0;
    this.isAlive = true;
    this.isLocked = false;
    this.createdAt = Date.now();
    this.errorCount = 0;
    this.evadeChance = type === 'EVASIVE' ? 0.3 : 0;
    this.isRoundMonster = true;

    // 初始化激射状态
    this.isEjecting = false;
    this.ejectionDirection = null;
    this.ejectionDistance = 0;
    this.ejectionTargetDistance = 0;
    
    if (type === 'ATTACKER') {
      this.lastSpawnTime = Date.now();
      this.lastBurstTime = Date.now();
    }
  }

  // 更新位置 - 向玩家位置移动
  update(deltaTime: number, playerX: number, playerY: number): void {
    if (!this.isAlive) return;
    
    // 如果处于激射阶段
    if (this.isEjecting && this.ejectionDirection) {
      // 激射速度是正常速度的3倍
      const ejectionSpeed = this.speed * 3 * deltaTime / 16.67;
      
      // 沿着激射方向移动
      this.x += this.ejectionDirection.x * ejectionSpeed;
      this.y += this.ejectionDirection.y * ejectionSpeed;
      
      // 更新已激射距离
      this.ejectionDistance += ejectionSpeed;
      
      // 检查是否完成激射
      if (this.ejectionDistance >= this.ejectionTargetDistance) {
        this.isEjecting = false;
        this.ejectionDirection = null;
      }
      
      return;
    }
    
    // 正常移动：向玩家位置移动
    const dx = playerX - this.x;
    const dy = playerY - this.y;
    const distance = Math.sqrt(dx * dx + dy * dy);

    // 如果距离很近，不再移动（避免抖动）
    if (distance < 5) return;

    // 归一化方向向量
    const dirX = dx / distance;
    const dirY = dy / distance;

    // 沿着方向移动
    const moveSpeed = this.speed * deltaTime / 16.67; // 标准化到60fps
    this.x += dirX * moveSpeed;
    this.y += dirY * moveSpeed;
  }

  // 设置激射效果
  setEjection(direction: { x: number; y: number }, distance: number): void {
    this.isEjecting = true;
    this.ejectionDirection = direction;
    this.ejectionDistance = 0;
    this.ejectionTargetDistance = distance;
  }

  // 处理输入
  handleInput(char: string): boolean {
    if (!this.isAlive || !this.isLocked) return false;

    const expectedChar = this.text[this.currentIndex];
    
    // EVASIVE类型有概率回避
    if (this.type === 'EVASIVE' && Math.random() < this.evadeChance) {
      return false; // 回避成功
    }

    if (char === expectedChar) {
      this.currentIndex++;
      
      // 检查是否消灭
      if (this.currentIndex >= this.text.length) {
        this.isAlive = false;
        this.eliminatedAt = Date.now();
      }
      
      return true;
    } else {
      this.errorCount++;
      return false;
    }
  }

  // 获取剩余字符
  getRemainingText(): string {
    return this.text.substring(this.currentIndex);
  }

  // 获取已输入字符
  getTypedText(): string {
    return this.text.substring(0, this.currentIndex);
  }

  // 获取得分
  getScore(): number {
    const baseScore = this.text.length;
    const typeMultiplier = this.getTypeMultiplier();
    const timeBonus = this.getTimeBonus();
    const accuracyBonus = this.getAccuracyBonus();
    
    return Math.floor(baseScore * typeMultiplier * timeBonus * accuracyBonus);
  }

  // 获取类型倍数
  private getTypeMultiplier(): number {
    switch (this.type) {
      case 'NORMAL': return 1;
      case 'ATTACKER': return 3;
      case 'EVASIVE': return 2;
      case 'SMART': return 3;
      default: return 1;
    }
  }

  // 获取时间奖励
  private getTimeBonus(): number {
    if (!this.eliminatedAt) return 1;
    
    const survivalTime = this.eliminatedAt - this.createdAt;
    const maxTime = 10000; // 10秒
    
    // 越快消灭奖励越高
    return Math.max(1, 2 - survivalTime / maxTime);
  }

  // 获取准确率奖励
  private getAccuracyBonus(): number {
    const totalInputs = this.currentIndex + this.errorCount;
    if (totalInputs === 0) return 1;
    
    const accuracy = this.currentIndex / totalInputs;
    return accuracy;
  }

  // 检查是否与玩家碰撞
  isCollidingWithPlayer(playerX: number, playerY: number, collisionRadius: number = 30): boolean {
    const dx = this.x - playerX;
    const dy = this.y - playerY;
    const distance = Math.sqrt(dx * dx + dy * dy);
    
    return distance < collisionRadius + this.size * 20;
  }

  // 检查是否与其他怪物碰撞
  isCollidingWithMonster(other: MonsterEntity): boolean {
    const dx = this.x - other.x;
    const dy = this.y - other.y;
    const distance = Math.sqrt(dx * dx + dy * dy);
    
    // 两个怪物的碰撞半径之和
    const minDistance = (this.size * 20) + (other.size * 20) + 10; // 额外10px间距
    
    return distance < minDistance;
  }

  // 获取与其他怪物的推开向量
  getRepulsionVector(other: MonsterEntity): { x: number; y: number } {
    const dx = this.x - other.x;
    const dy = this.y - other.y;
    const distance = Math.sqrt(dx * dx + dy * dy);
    
    if (distance === 0) {
      // 完全重叠，随机方向
      const angle = Math.random() * Math.PI * 2;
      return { x: Math.cos(angle), y: Math.sin(angle) };
    }
    
    // 归一化方向向量
    return { x: dx / distance, y: dy / distance };
  }

  // 检查是否需要生成小怪（ATTACKER类型）
  shouldSpawnMinion(): boolean {
    if (this.type !== 'ATTACKER' || !this.lastSpawnTime) return false;
    
    const now = Date.now();
    const timeSinceLastSpawn = now - this.lastSpawnTime;
    
    if (timeSinceLastSpawn >= 5000) { // 每5秒
      this.lastSpawnTime = now;
      return true;
    }
    
    return false;
  }

  // 检查是否需要爆发（ATTACKER类型）
  shouldBurst(): boolean {
    if (this.type !== 'ATTACKER' || !this.lastBurstTime) return false;
    
    const now = Date.now();
    const timeSinceLastBurst = now - this.lastBurstTime;
    
    if (timeSinceLastBurst >= 15000) { // 每15秒
      this.lastBurstTime = now;
      return true;
    }
    
    return false;
  }

  // 转换为普通对象
  toJSON(): Monster {
    return {
      id: this.id,
      text: this.text,
      type: this.type,
      x: this.x,
      y: this.y,
      speed: this.speed,
      size: this.size,
      currentIndex: this.currentIndex,
      isAlive: this.isAlive,
      isLocked: this.isLocked,
      createdAt: this.createdAt,
      eliminatedAt: this.eliminatedAt,
      errorCount: this.errorCount,
      lastSpawnTime: this.lastSpawnTime,
      lastBurstTime: this.lastBurstTime,
      evadeChance: this.evadeChance,
      isEjecting: this.isEjecting,
      ejectionDirection: this.ejectionDirection,
      ejectionDistance: this.ejectionDistance,
      ejectionTargetDistance: this.ejectionTargetDistance,
    };
  }
}
