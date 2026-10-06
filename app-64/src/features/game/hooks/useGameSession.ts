import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { completeGameRound, createGameRecord, getNextRound, createGameSession, type GameRecordUploadResult, type MonsterDataDTO } from '@/db/api';
import { GameEngine, type RoundMonsterItem } from '@/game/GameEngine';
import { SoundSystem } from '@/game/SoundSystem';
import type { CharacterStat, GameEvent, GameState, GameStats } from '@/game/types';
import type { Profile } from '@/types/database';
import type { GameConfig as AppGameConfig } from '@/types/game';
import { useToast } from '@/hooks/use-toast';
import {
  createAdaptiveConfig,
  createCharacterPool,
  createEngineConfig,
  createSpawnConfig,
  type StartRoundParams,
} from '../utils/gameRuntime';

interface LoadedStartRoundParams extends StartRoundParams {
  monsters?: MonsterDataDTO[];
  sessionId?: number;
  planVersion?: number;
}

interface InputFeedback {
  input: string;
  expected: string;
  timestamp: number;
}

interface CorrectFeedback {
  char: string;
  timestamp: number;
}

interface UseGameSessionOptions {
  user: Profile | null;
  config: AppGameConfig;
  onAuthRequired: () => void;
}

function toRoundMonsters(monsters: MonsterDataDTO[] | undefined): RoundMonsterItem[] | undefined {
  if (!monsters?.length) return undefined;
  return monsters.map((monster) => ({
    targetStr: monster.targetStr,
    speed: monster.speed != null ? Number(monster.speed) : undefined,
    monsterType: monster.monsterType,
  }));
}

function buildCharacterStatsUpdates(characterStats: CharacterStat[]) {
  const updates: Array<{ charValue: string; isCorrect: boolean; charType?: string }> = [];
  for (const stat of characterStats) {
    for (let i = 0; i < stat.correctCount; i += 1) {
      updates.push({ charValue: stat.char, isCorrect: true });
    }
    for (let i = 0; i < stat.errorCount; i += 1) {
      updates.push({ charValue: stat.char, isCorrect: false });
    }
  }
  return updates;
}

export function useGameSession({ user, config, onAuthRequired }: UseGameSessionOptions) {
  const { toast } = useToast();
  const [gameEngine, setGameEngine] = useState<GameEngine | null>(null);
  const [gameState, setGameState] = useState<GameState>('PLAYING');
  const [stats, setStats] = useState<GameStats | null>(null);
  const [soundEnabled, setSoundEnabled] = useState(config.soundEnabled !== false);
  const [showDetailedResult, setShowDetailedResult] = useState(false);
  const [uploadResult, setUploadResult] = useState<GameRecordUploadResult | null>(null);
  const [startParams, setStartParams] = useState<LoadedStartRoundParams | null>(null);
  const [sessionVersion, setSessionVersion] = useState(0);
  const [lastError, setLastError] = useState<InputFeedback | null>(null);
  const [lastCorrect, setLastCorrect] = useState<CorrectFeedback | null>(null);

  const gameEngineRef = useRef<GameEngine | null>(null);
  const soundSystemRef = useRef<SoundSystem | null>(null);
  const soundEnabledRef = useRef(soundEnabled);
  const configRef = useRef(config);
  const userRef = useRef(user);
  const gameOverHandledRef = useRef(false);
  const sessionTokenRef = useRef(0);
  const sessionIdRef = useRef<number | undefined>(undefined);
  const planVersionRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    configRef.current = config;
  }, [config]);

  useEffect(() => {
    userRef.current = user;
  }, [user]);

  useEffect(() => {
    soundEnabledRef.current = soundEnabled;
  }, [soundEnabled]);

  const dictKey = useMemo(() => {
    const custom = config.customDictIds?.join(',') ?? '';
    const system = config.systemDictIds?.join(',') ?? '';
    return `${custom}|${system}`;
  }, [config.customDictIds, config.systemDictIds]);

  useEffect(() => {
    if (!user) {
      setStartParams(null);
      onAuthRequired();
      return;
    }

    let cancelled = false;
    setStartParams(null);
    createGameSession(config.gameMode)
      .then((session) => getNextRound(1, config.gameMode, config.customDictIds, config.systemDictIds, session.sessionId)
        .then((response) => ({ response, sessionId: session.sessionId })))
      .then(({ response, sessionId }) => {
        if (cancelled) return;
        const param = response?.adjustParam ?? {};
        setStartParams({
          baseSpeed: param.monsterSpeed != null ? Number(param.monsterSpeed) : 0.8,
          maxMonsters: param.monsterCount ?? 8,
          monsters: response?.monsters,
          sessionId,
          planVersion: response?.planVersion,
        });
      })
      .catch(() => {
        if (!cancelled) setStartParams({ baseSpeed: 0.8, maxMonsters: 8 });
      });

    return () => {
      cancelled = true;
    };
  }, [config.gameMode, config.customDictIds, config.systemDictIds, dictKey, onAuthRequired, sessionVersion, user]);

  const handleGameOver = useCallback(async (finalStats: GameStats) => {
    if (gameOverHandledRef.current) return;
    gameOverHandledRef.current = true;
    setGameState('GAME_OVER');
    setStats(finalStats);

    const currentUser = userRef.current;
    const currentConfig = configRef.current;
    if (!currentUser) return;

    try {
      const engine = gameEngineRef.current;
      const monsterHistory = engine?.getMonsterHistory() ?? [];
      const characterStatsUpdates = buildCharacterStatsUpdates(engine?.getCharacterStats() ?? []);

      const result = await createGameRecord({
        gameMode: currentConfig.gameMode,
        selectedChars: currentConfig.selectedChars,
        roundNum: finalStats.round,
        totalScore: finalStats.score,
        totalChars: finalStats.totalChars,
        correctChars: finalStats.correctChars,
        avgSpeed: finalStats.speed,
        peakSpeed: finalStats.speed,
        avgAccuracy: finalStats.accuracy,
        peakAccuracy: finalStats.accuracy,
        gameDuration: Math.floor((finalStats.currentTime - finalStats.startTime) / 1000),
        sessionId: sessionIdRef.current,
        accuracyRatio: finalStats.accuracy / 100,
        gameDurationMs: finalStats.currentTime - finalStats.startTime,
        dataVersion: 1,
        monsters: monsterHistory,
        customDictIds: currentConfig.customDictIds,
        systemDictIds: currentConfig.systemDictIds,
        characterStatsUpdates: characterStatsUpdates.length > 0 ? characterStatsUpdates : undefined,
      });
      if (result) setUploadResult(result);
      toast({ title: '游戏记录已保存', description: '数据已同步到服务器' });
    } catch (error) {
      console.error('保存游戏记录失败:', error);
      toast({ title: '保存失败', description: '游戏记录保存失败，请检查网络连接', variant: 'destructive' });
    }
  }, [toast]);

  const handleGameEvent = useCallback((event: GameEvent) => {
    const sound = soundSystemRef.current;
    const currentConfig = configRef.current;

    switch (event.type) {
      case 'MONSTER_HIT':
        sound?.playHit();
        break;
      case 'MONSTER_KILLED':
        sound?.playKill();
        toast({ title: '消灭怪兽！', description: `获得 ${event.score} 分` });
        break;
      case 'MONSTER_EVADED':
        sound?.playEvade();
        toast({ title: '怪兽回避了！', variant: 'destructive' });
        break;
      case 'INPUT_ERROR':
        sound?.playError();
        setLastError({ input: event.char, expected: event.expected, timestamp: Date.now() });
        break;
      case 'INPUT_CORRECT':
        setLastCorrect({ char: event.char, timestamp: Date.now() });
        break;
      case 'ROUND_COMPLETE': {
        sound?.playRoundComplete();
        toast({
          title: `第 ${event.round} 轮完成！`,
          description: `得分: ${event.stats.score}, 准确率: ${event.stats.accuracy.toFixed(1)}%`,
        });
        const token = sessionTokenRef.current;
        const sessionId = sessionIdRef.current;
        const planVersion = planVersionRef.current;
        const currentRoundMonsters = (gameEngineRef.current?.getMonsterHistory() ?? [])
          .filter((monster) => monster.spawnWave === event.round)
          .map((monster) => ({
            ...monster,
            inputCount: monster.totalCount ?? 0,
            correctCount: Math.max(0, (monster.totalCount ?? 0) - (monster.inputErrorCount ?? 0)),
            errorCount: monster.inputErrorCount ?? 0,
            result: monster.result,
          }));
        const complete = sessionId != null && planVersion != null
          ? completeGameRound(event.round, sessionId, planVersion, currentRoundMonsters)
          : Promise.resolve(null);
        complete.then(() => getNextRound(event.round + 1, currentConfig.gameMode, currentConfig.customDictIds, currentConfig.systemDictIds, sessionId))
          .then((response) => {
            if (token !== sessionTokenRef.current) return;
            const param = response?.adjustParam ?? {};
            const speed = param.monsterSpeed != null ? Number(param.monsterSpeed) : undefined;
            const count = param.monsterCount;
            const engine = gameEngineRef.current;
            if (!engine) return;
            if (speed != null || count != null) {
              engine.applyAdjustParam(
                speed ?? engine.getAdaptiveSystem().getCurrentSpeed(),
                count ?? engine.getAdaptiveSystem().getCurrentMaxMonsters()
              );
            }
            const roundMonsters = toRoundMonsters(response?.monsters);
            planVersionRef.current = response?.planVersion;
            if (roundMonsters?.length) engine.setRoundMonsters(roundMonsters);
          })
          .catch(() => {
            toast({ title: '下一轮参数加载失败', description: '已继续使用当前难度参数', variant: 'destructive' });
          });
        break;
      }
      case 'GAME_OVER':
        sound?.playGameOver();
        void handleGameOver(event.stats);
        break;
      case 'DIFFICULTY_ADJUSTED':
        toast({ title: '难度调整', description: `速度: ${event.newSpeed.toFixed(1)}, 怪兽数: ${event.newMaxMonsters}` });
        break;
      default:
        break;
    }
  }, [handleGameOver, toast]);

  useEffect(() => {
    if (!user || startParams === null) return;

    const token = sessionTokenRef.current + 1;
        sessionTokenRef.current = token;
    gameOverHandledRef.current = false;

    const currentConfig = configRef.current;
    sessionIdRef.current = startParams.sessionId;
    planVersionRef.current = startParams.planVersion;
    const initialRoundMonsters = toRoundMonsters(startParams.monsters);
    const engine = new GameEngine(
      createEngineConfig(currentConfig, startParams, soundEnabledRef.current),
      createAdaptiveConfig(startParams),
      createSpawnConfig(),
      createCharacterPool(currentConfig),
      initialRoundMonsters
    );
    const sound = new SoundSystem(soundEnabledRef.current);
    engine.addEventListener(handleGameEvent);

    gameEngineRef.current = engine;
    soundSystemRef.current = sound;
    setGameEngine(engine);
    setStats(engine.getStats());

    const startTimer = window.setTimeout(() => {
      if (sessionTokenRef.current !== token) return;
      engine.start();
      setGameState('PLAYING');
    }, 100);

    return () => {
      window.clearTimeout(startTimer);
      engine.removeEventListener(handleGameEvent);
      engine.destroy();
      sound.destroy();
      if (sessionTokenRef.current === token) {
        gameEngineRef.current = null;
        soundSystemRef.current = null;
      }
    };
  }, [handleGameEvent, sessionVersion, startParams, user]);

  useEffect(() => {
    if (!gameEngine || gameState !== 'PLAYING') return;
    const handleKeyDown = (event: KeyboardEvent) => gameEngine.handleKeyDown(event);
    const handleKeyUp = (event: KeyboardEvent) => gameEngine.handleKeyUp(event);
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [gameEngine, gameState]);

  useEffect(() => {
    if (!gameEngine || gameState !== 'PLAYING') return;
    const interval = window.setInterval(() => setStats(gameEngine.getStats()), 100);
    return () => window.clearInterval(interval);
  }, [gameEngine, gameState]);

  const pause = useCallback(() => {
    const engine = gameEngineRef.current;
    if (!engine) return;
    engine.pause();
    setGameState('PAUSED');
  }, []);

  const resume = useCallback(() => {
    const engine = gameEngineRef.current;
    if (!engine) return;
    engine.resume();
    setGameState('PLAYING');
  }, []);

  const toggleSound = useCallback(() => {
    setSoundEnabled((enabled) => {
      const next = !enabled;
      soundSystemRef.current?.setEnabled(next);
      return next;
    });
  }, []);

  const restart = useCallback(() => {
    gameEngineRef.current?.destroy();
    soundSystemRef.current?.destroy();
    gameEngineRef.current = null;
    soundSystemRef.current = null;
    setGameEngine(null);
    setGameState('PLAYING');
    setStats(null);
    setUploadResult(null);
    setShowDetailedResult(false);
    setLastError(null);
    setLastCorrect(null);
    setSessionVersion((version) => version + 1);
  }, []);

  return {
    gameEngine,
    gameState,
    stats,
    soundEnabled,
    showDetailedResult,
    uploadResult,
    startParams,
    lastError,
    lastCorrect,
    characterStats: gameEngine?.getCharacterStats() ?? [],
    pause,
    resume,
    restart,
    toggleSound,
    setShowDetailedResult,
  };
}
