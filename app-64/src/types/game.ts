// 游戏相关类型定义

export type MonsterType = 'NORMAL' | 'ATTACKER' | 'EVASIVE' | 'SMART';

export interface Monster {
  id: string;
  text: string; // 字符串内容
  type: MonsterType;
  x: number; // 横坐标
  y: number; // 纵坐标
  vx: number; // x方向速度
  vy: number; // y方向速度
  speed: number; // 移动速度
  radius: number; // 碰撞半径
  health: number; // 当前生命值（剩余字符数）
  maxHealth: number; // 最大生命值
  currentIndex: number; // 当前需要输入的字符索引
  isAlive: boolean;
  spawnTime: number; // 生成时间
  spawnDelay: number; // 生成延迟（毫秒）
  isSpawned: boolean; // 是否已经生成（延迟后才显示）
  killTime?: number; // 消灭时间
  errorCount: number; // 输入错误次数
  lastAttackTime?: number; // 上次攻击时间（用于ATTACKER类型）
}

export interface GameConfig {
  // 字符类型配置
  enableLowercase: boolean; // 小写字母
  enableUppercase: boolean; // 大写字母
  enableNumbers: boolean; // 数字
  enablePunctuation: boolean; // 标点符号
  enableOperators: boolean; // 运算符号
  enableSpecialChars: boolean; // 特殊字符
  enableChinese: boolean; // 中文字符
  
  // 游戏模式（与后端一致：endless 无尽适应 / dictionary 词源界定 / limit 限时冲刺；已移除 survival）
  gameMode: 'endless' | 'dictionary' | 'limit';
  
  // 限时模式配置
  timeLimitSeconds?: number; // 限时模式的时间限制（秒）
  survivalSpeedIncrease?: number; // 生存/限时类模式速度增长率（如每 30 秒 +10%）

  // 字符类型选择摘要（上传记录用，如 "lower,number"）
  selectedChars?: string;

  // 词库（词源界定模式）：词条用于本局生成；ID 用于 next-round 接口
  customDictionary?: string[];
  /** 选中的自定义词库 ID 列表，调用 getNextRound 时传入 */
  customDictIds?: number[];
  /** 选中的系统词库 ID 列表，调用 getNextRound 时传入 */
  systemDictIds?: number[];
  
  // 音效
  soundEnabled: boolean;
}

export interface GameStats {
  score: number;
  rounds: number;
  totalCharacters: number;
  correctCharacters: number;
  errorCharacters: number;
  startTime: number;
  currentSpeed: number; // 当前打字速度（字符/分钟）
  currentAccuracy: number; // 当前准确率
  peakSpeed: number;
  peakAccuracy: number;
  consecutiveCorrect: number; // 连续正确数
  maxConsecutiveCorrect: number; // 最大连续正确数
}

export interface CharacterInput {
  character: string;
  isCorrect: boolean;
  timestamp: number;
}

export interface RoundData {
  roundNumber: number;
  monsters: Monster[];
  startTime: number;
  endTime?: number;
  accuracy: number;
  speed: number;
}

export type InputMode = 'english' | 'chinese';

export interface ChineseInputBuffer {
  buffer: string; // 拼音缓冲区
  candidates: string[]; // 候选字
}

export type GameState = 'menu' | 'prepare' | 'playing' | 'paused' | 'gameover';

export interface PlayerState {
  x: number; // 玩家位置（固定在底部中央）
  y: number;
  lockedMonster: Monster | null; // 当前锁定的怪物
  inputMode: InputMode;
  chineseBuffer?: ChineseInputBuffer;
  capsLock: boolean; // 大小写锁定状态
}
