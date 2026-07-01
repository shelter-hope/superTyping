// 游戏类型定义

// 怪兽类型
export type MonsterType = 'NORMAL' | 'ATTACKER' | 'EVASIVE' | 'SMART';

// 游戏状态
export type GameState = 'READY' | 'PLAYING' | 'PAUSED' | 'GAME_OVER';

// 输入模式
export type InputMode = 'ENGLISH' | 'CHINESE';

// 游戏模式
export type GameMode = 'ENDLESS' | 'TIMED' | 'SURVIVAL';

// 怪兽接口
export interface Monster {
  id: string;
  text: string;                    // 字符串内容
  type: MonsterType;               // 怪兽类型
  x: number;                       // X坐标
  y: number;                       // Y坐标
  speed: number;                   // 移动速度
  size: number;                    // 大小
  currentIndex: number;            // 当前输入到第几个字符
  isAlive: boolean;                // 是否存活
  isLocked: boolean;               // 是否被锁定
  createdAt: number;               // 创建时间
  eliminatedAt?: number;           // 消灭时间
  errorCount: number;              // 输入错误次数
  lastSpawnTime?: number;          // 上次生成小怪时间（ATTACKER类型）
  lastBurstTime?: number;          // 上次爆发时间（ATTACKER类型）
  evadeChance: number;             // 回避概率（EVASIVE类型）
  isEjecting: boolean;             // 是否处于激射状态
  ejectionDirection: { x: number; y: number } | null;  // 激射方向
  ejectionDistance: number;        // 已激射距离
  ejectionTargetDistance: number;  // 目标激射距离
  /** 是否计入本轮数量（ATTACKER 产生的小怪不计入，用于“本轮全部消灭后进入下一轮”判定） */
  isRoundMonster?: boolean;
}

// 玩家状态
export interface PlayerState {
  x: number;                       // X坐标
  y: number;                       // Y坐标
  isAlive: boolean;                // 是否存活
}

// 游戏统计
export interface GameStats {
  mode: GameMode;                  // 游戏模式
  score: number;                   // 得分
  round: number;                   // 当前轮数
  totalChars: number;              // 总字符数
  correctChars: number;            // 正确字符数
  errorChars: number;              // 错误字符数
  accuracy: number;                // 准确率
  speed: number;                   // 打字速度（字符/分钟）
  startTime: number;               // 开始时间
  currentTime: number;             // 当前时间
  monstersKilled: number;          // 消灭的怪兽数量
  longestStreak: number;           // 最长连击
  currentStreak: number;           // 当前连击
  timeRemaining?: number;          // 剩余时间（限时模式）
  survivalTime?: number;           // 生存时间（生存模式，秒）
}

// 字符统计
export interface CharacterStat {
  char: string;                    // 字符
  correctCount: number;            // 正确次数
  errorCount: number;              // 错误次数
  accuracy: number;                // 准确率
  lastUsed: number;                // 最后使用时间
}

// 游戏配置
export interface GameConfig {
  mode: GameMode;                  // 游戏模式
  canvasWidth: number;             // 画布宽度
  canvasHeight: number;            // 画布高度
  playerY: number;                 // 玩家Y坐标
  baseSpeed: number;               // 基础速度
  spawnInterval: number;           // 生成间隔
  maxMonsters: number;             // 最大怪兽数量
  roundDuration: number;           // 每轮持续时间（毫秒）
  enableSound: boolean;            // 是否启用音效
  timeLimitSeconds?: number;       // 限时模式的时间限制（秒）
  survivalSpeedIncrease?: number;  // 生存模式的速度增长率
}

// 自适应配置
export interface AdaptiveConfig {
  targetAccuracy: number;          // 目标准确率
  speedAdjustThreshold: number;    // 速度调整阈值
  difficultyAdjustInterval: number; // 难度调整间隔
  minSpeed: number;                // 最小速度
  maxSpeed: number;                // 最大速度
  minMonsters: number;             // 最小怪兽数量
  maxMonsters: number;             // 最大怪兽数量
}

// 怪兽生成配置
export interface MonsterSpawnConfig {
  normalWeight: number;            // 普通型权重
  attackerWeight: number;          // 攻击型权重
  evasiveWeight: number;           // 回避型权重
  smartWeight: number;             // 智慧型权重
  minLength: number;               // 最小字符串长度
  maxLength: number;               // 最大字符串长度
  preferredLength: number;         // 偏好字符串长度
}

// 进化数据
export interface EvolutionData {
  parentMonsters: Monster[];       // 父本怪兽
  weakCharacters: CharacterStat[]; // 易错字符
  averageAccuracy: number;         // 平均准确率
  averageSpeed: number;            // 平均速度
}

// 游戏事件
export type GameEvent = 
  | { type: 'MONSTER_SPAWNED'; monster: Monster }
  | { type: 'MONSTER_HIT'; monster: Monster; char: string }
  | { type: 'MONSTER_KILLED'; monster: Monster; score: number }
  | { type: 'MONSTER_EVADED'; monster: Monster }
  | { type: 'INPUT_ERROR'; char: string; expected: string }
  | { type: 'INPUT_CORRECT'; char: string }
  | { type: 'ROUND_COMPLETE'; round: number; stats: GameStats }
  | { type: 'GAME_OVER'; stats: GameStats }
  | { type: 'DIFFICULTY_ADJUSTED'; newSpeed: number; newMaxMonsters: number };

// 游戏事件处理器
export type GameEventHandler = (event: GameEvent) => void;
