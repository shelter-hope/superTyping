import type { GameConfig as EngineGameConfig, AdaptiveConfig, MonsterSpawnConfig } from '@/game/types';
import type { GameConfig as AppGameConfig } from '@/types/game';

const CHINESE_PRACTICE = ['的', '一', '是', '不', '了', '人', '我', '在', '有', '他', '这', '中', '大', '来', '上', '个', '国', '到', '说', '们'];

export const GAME_CANVAS_WIDTH = 1200;
export const GAME_CANVAS_HEIGHT = 800;

export interface StartRoundParams {
  baseSpeed: number;
  maxMonsters: number;
}

export function createCharacterPool(config: AppGameConfig): string[] {
  const pool: string[] = [];

  if (config.enableLowercase) {
    for (let i = 97; i <= 122; i += 1) pool.push(String.fromCharCode(i));
  }

  if (config.enableUppercase) {
    for (let i = 65; i <= 90; i += 1) pool.push(String.fromCharCode(i));
  }

  if (config.enableNumbers) {
    for (let i = 48; i <= 57; i += 1) pool.push(String.fromCharCode(i));
  }

  if (config.enablePunctuation) pool.push(',', '.', '!', '?', ';', ':', "'", '"');
  if (config.enableOperators) pool.push('+', '-', '*', '/', '=', '<', '>');
  if (config.enableSpecialChars) pool.push('@', '#', '$', '%', '^', '&', '*');
  if (config.enableChinese) pool.push(...CHINESE_PRACTICE);

  if (config.gameMode === 'dictionary' && config.customDictionary?.length) {
    return config.customDictionary;
  }

  return pool.length > 0 ? pool : ['a', 'b', 'c', 'd', 'e'];
}

export function selectedCharsLabel(config: Pick<AppGameConfig, 'enableLowercase' | 'enableUppercase' | 'enableNumbers' | 'enablePunctuation' | 'enableOperators' | 'enableSpecialChars' | 'enableChinese'>): string {
  const types: string[] = [];
  if (config.enableLowercase) types.push('lower');
  if (config.enableUppercase) types.push('upper');
  if (config.enableNumbers) types.push('number');
  if (config.enablePunctuation) types.push('punct');
  if (config.enableOperators) types.push('operator');
  if (config.enableSpecialChars) types.push('special');
  if (config.enableChinese) types.push('chinese');
  return types.join(',');
}

/** 推荐词没有词库 ID 时，用这些词替换服务端生成的目标串，避免推荐被下一轮词覆盖。 */
export function practiceWords(config: Pick<AppGameConfig, 'gameMode' | 'customDictionary' | 'customDictIds' | 'systemDictIds'>): string[] | undefined {
  if (config.gameMode !== 'dictionary') return undefined;
  if (config.customDictIds?.length || config.systemDictIds?.length) return undefined;
  const words = config.customDictionary?.map((word) => word.trim()).filter(Boolean);
  return words?.length ? words : undefined;
}

export function applyPracticeWords<T extends { targetStr?: string }>(monsters: T[] | undefined, words: string[] | undefined): T[] | undefined {
  if (!monsters?.length || !words?.length) return monsters;
  return monsters.map((monster, index) => ({ ...monster, targetStr: words[index % words.length] }));
}

export function createEngineConfig(config: AppGameConfig, startParams: StartRoundParams, soundEnabled: boolean): EngineGameConfig {
  return {
    mode: config.gameMode === 'limit' ? 'TIMED' : 'ENDLESS',
    canvasWidth: GAME_CANVAS_WIDTH,
    canvasHeight: GAME_CANVAS_HEIGHT,
    playerY: 750,
    baseSpeed: startParams.baseSpeed,
    spawnInterval: 2500,
    maxMonsters: startParams.maxMonsters,
    roundDuration: 60000,
    enableSound: soundEnabled,
    timeLimitSeconds: config.timeLimitSeconds || 60,
    survivalSpeedIncrease: config.survivalSpeedIncrease || 0.1,
  };
}

export function createAdaptiveConfig(startParams: StartRoundParams): AdaptiveConfig {
  return {
    targetAccuracy: 80,
    speedAdjustThreshold: 0.15,
    difficultyAdjustInterval: 15000,
    minSpeed: 0.5,
    maxSpeed: 2.5,
    minMonsters: Math.min(3, startParams.maxMonsters),
    maxMonsters: Math.max(12, startParams.maxMonsters),
  };
}

export function createSpawnConfig(): MonsterSpawnConfig {
  return {
    normalWeight: 60,
    attackerWeight: 15,
    evasiveWeight: 15,
    smartWeight: 10,
    minLength: 3,
    maxLength: 10,
    preferredLength: 5,
  };
}
