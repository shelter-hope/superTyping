// 游戏页面
import { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { useGameConfig } from '@/contexts/GameConfigContext';
import { GameEngine } from '@/game/GameEngine';
import { SoundSystem } from '@/game/SoundSystem';
import GameCanvas from '@/components/game/GameCanvas';
import GameUI from '@/components/game/GameUI';
import ErrorFeedback from '@/components/game/ErrorFeedback';
import ErrorDistributionChart from '@/components/stats/ErrorDistributionChart';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { createGameRecord, getNextRound, type GameRecordUploadResult, type MonsterDataDTO } from '@/db/api';
import { useToast } from '@/hooks/use-toast';
import type { GameEvent, GameStats } from '@/game/types';

export default function GamePage() {
  const { user } = useAuth();
  const { config } = useGameConfig();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [gameEngine, setGameEngine] = useState<GameEngine | null>(null);
  const [soundSystem, setSoundSystem] = useState<SoundSystem | null>(null);
  const [gameState, setGameState] = useState<'PLAYING' | 'PAUSED' | 'GAME_OVER'>('PLAYING');
  const [stats, setStats] = useState<GameStats | null>(null);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showDetailedResult, setShowDetailedResult] = useState(false);
  /** 上传记录后的结算摘要（含 Dₜ），用于结算页展示 */
  const [uploadResult, setUploadResult] = useState<GameRecordUploadResult | null>(null);
  /** 首轮参数（来自 getNextRound），null 表示尚未拉取完成；词源模式含首轮怪物列表 */
  const [startParams, setStartParams] = useState<{ baseSpeed: number; maxMonsters: number; monsters?: MonsterDataDTO[] } | null>(null);
  
  // 错误和正确输入反馈
  const [lastError, setLastError] = useState<{ input: string; expected: string; timestamp: number } | null>(null);
  const [lastCorrect, setLastCorrect] = useState<{ char: string; timestamp: number } | null>(null);

  const gameEngineRef = useRef<GameEngine | null>(null);
  const soundSystemRef = useRef<SoundSystem | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // 拉取首轮参数（getNextRound），用于初始化引擎速度与怪物数量；词源模式传词库 ID
  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    (async () => {
      try {
        const res = await getNextRound(1, config.gameMode, config.customDictIds, config.systemDictIds);
        if (cancelled) return;
        const p = res?.adjustParam ?? {};
        setStartParams({
          baseSpeed: p.monsterSpeed != null ? Number(p.monsterSpeed) : 0.8,
          maxMonsters: p.monsterCount ?? 8,
          monsters: res?.monsters,
        });
      } catch {
        if (!cancelled) setStartParams({ baseSpeed: 0.8, maxMonsters: 8 });
      }
    })();
    return () => { cancelled = true; };
  }, [user, config.gameMode, config.customDictIds, config.systemDictIds]);

  // 初始化游戏并自动开始（依赖首轮参数）
  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (startParams === null) return;

    const { baseSpeed, maxMonsters } = startParams;
    const characterPool = createCharacterPool();
    const engineMode: 'ENDLESS' | 'TIMED' | 'SURVIVAL' = config.gameMode === 'limit' ? 'TIMED' : 'ENDLESS';

    const gameConfig: import('@/game/types').GameConfig = {
      mode: engineMode,
      canvasWidth: 1200,
      canvasHeight: 800,
      playerY: 750,
      baseSpeed,
      spawnInterval: 2500,
      maxMonsters,
      roundDuration: 60000,
      enableSound: soundEnabled,
      timeLimitSeconds: config.timeLimitSeconds || 60,
      survivalSpeedIncrease: config.survivalSpeedIncrease || 0.1,
    };

    const adaptiveConfig = {
      targetAccuracy: 80,
      speedAdjustThreshold: 0.15,
      difficultyAdjustInterval: 15000,
      minSpeed: 0.5,
      maxSpeed: 2.5,
      minMonsters: Math.min(3, maxMonsters),
      maxMonsters: Math.max(12, maxMonsters),
    };

    const spawnConfig = {
      normalWeight: 60,
      attackerWeight: 15,
      evasiveWeight: 15,
      smartWeight: 10,
      minLength: 3,
      maxLength: 10,
      preferredLength: 5,
    };

    const initialRoundMonsters =
      config.gameMode === 'dictionary' && startParams.monsters?.length
        ? startParams.monsters.map((m) => ({
            targetStr: m.targetStr,
            speed: m.speed != null ? Number(m.speed) : undefined,
            monsterType: m.monsterType,
          }))
        : undefined;
    const engine = new GameEngine(gameConfig, adaptiveConfig, spawnConfig, characterPool, initialRoundMonsters);
    const sound = new SoundSystem(soundEnabled);
    engine.addEventListener(handleGameEvent);

    gameEngineRef.current = engine;
    soundSystemRef.current = sound;
    setGameEngine(engine);
    setSoundSystem(sound);

    setTimeout(() => {
      engine.start();
      setGameState('PLAYING');
    }, 100);

    return () => {
      engine.destroy();
      sound.destroy();
    };
  }, [user, navigate, startParams]);

  // 创建字符池
  function createCharacterPool(): string[] {
    const pool: string[] = [];

    if (config.enableLowercase) {
      for (let i = 97; i <= 122; i++) {
        pool.push(String.fromCharCode(i));
      }
    }

    if (config.enableUppercase) {
      for (let i = 65; i <= 90; i++) {
        pool.push(String.fromCharCode(i));
      }
    }

    if (config.enableNumbers) {
      for (let i = 48; i <= 57; i++) {
        pool.push(String.fromCharCode(i));
      }
    }

    if (config.enablePunctuation) {
      pool.push(',', '.', '!', '?', ';', ':', "'", '"');
    }

    if (config.enableOperators) {
      pool.push('+', '-', '*', '/', '=', '<', '>');
    }

    if (config.enableSpecialChars) {
      pool.push('@', '#', '$', '%', '^', '&', '*');
    }

    // 如果是词源界定模式且有自定义词库
    if (config.gameMode === 'dictionary' && config.customDictionary && config.customDictionary.length > 0) {
      return config.customDictionary;
    }

    return pool.length > 0 ? pool : ['a', 'b', 'c', 'd', 'e'];
  }

  // 处理游戏事件
  const handleGameEvent = useCallback((event: GameEvent) => {
    const sound = soundSystemRef.current;

    switch (event.type) {
      case 'MONSTER_HIT':
        sound?.playHit();
        break;

      case 'MONSTER_KILLED':
        sound?.playKill();
        toast({
          title: '消灭怪兽！',
          description: `获得 ${event.score} 分`,
        });
        break;

      case 'MONSTER_EVADED':
        sound?.playEvade();
        toast({
          title: '怪兽回避了！',
          variant: 'destructive',
        });
        break;

      case 'INPUT_ERROR':
        sound?.playError();
        setLastError({
          input: event.char,
          expected: event.expected,
          timestamp: Date.now(),
        });
        break;

      case 'INPUT_CORRECT':
        setLastCorrect({
          char: event.char,
          timestamp: Date.now(),
        });
        break;

      case 'ROUND_COMPLETE': {
        sound?.playRoundComplete();
        toast({
          title: `第 ${event.round} 轮完成！`,
          description: `得分: ${event.stats.score}, 准确率: ${event.stats.accuracy.toFixed(1)}%`,
        });
        // 每轮串联：拉取下一轮参数并应用到引擎
        const nextRoundNum = event.round + 1;
        getNextRound(nextRoundNum, config.gameMode, config.customDictIds, config.systemDictIds)
          .then(res => {
            const p = res?.adjustParam ?? {};
            const speed = p.monsterSpeed != null ? Number(p.monsterSpeed) : undefined;
            const count = p.monsterCount;
            const engine = gameEngineRef.current;
            if (engine) {
              if (speed != null || count != null) {
                engine.applyAdjustParam(
                  speed ?? engine.getAdaptiveSystem().getCurrentSpeed(),
                  count ?? engine.getAdaptiveSystem().getCurrentMaxMonsters()
                );
              }
              if (config.gameMode === 'dictionary' && res?.monsters?.length) {
                engine.setRoundMonsters(
                  res.monsters.map((m) => ({
                    targetStr: m.targetStr,
                    speed: m.speed != null ? Number(m.speed) : undefined,
                    monsterType: m.monsterType,
                  }))
                );
              }
            }
          })
          .catch(() => {});
        break;
      }

      case 'GAME_OVER':
        sound?.playGameOver();
        handleGameOver(event.stats);
        break;

      case 'DIFFICULTY_ADJUSTED':
        toast({
          title: '难度调整',
          description: `速度: ${event.newSpeed.toFixed(1)}, 怪兽数: ${event.newMaxMonsters}`,
        });
        break;
    }
  }, [toast, config.gameMode, config.customDictIds, config.systemDictIds]);

  // 处理游戏结束
  async function handleGameOver(finalStats: GameStats) {
    setGameState('GAME_OVER');
    setStats(finalStats);

    if (!user) return;

    try {
      // 获取怪物历史记录与字符统计
      const monsterHistory = gameEngineRef.current?.getMonsterHistory() || [];
      const charStats = gameEngineRef.current?.getCharacterStats() || [];

      // 按字符展开为本局每条输入（正确/错误），用于更新 character_stats
      const characterStatsUpdates: Array<{ charValue: string; isCorrect: boolean; charType?: string }> = [];
      for (const stat of charStats) {
        for (let i = 0; i < stat.correctCount; i++) {
          characterStatsUpdates.push({ charValue: stat.char, isCorrect: true });
        }
        for (let i = 0; i < stat.errorCount; i++) {
          characterStatsUpdates.push({ charValue: stat.char, isCorrect: false });
        }
      }

      // 游戏记录与字符统计在一次请求中上传；gameMode 仅传 endless | dictionary | limit
      const result = await createGameRecord({
        gameMode: config.gameMode,
        selectedChars: config.selectedChars,
        roundNum: finalStats.round,
        totalScore: finalStats.score,
        totalChars: finalStats.totalChars,
        correctChars: finalStats.correctChars,
        avgSpeed: finalStats.speed,
        peakSpeed: finalStats.speed,
        avgAccuracy: finalStats.accuracy,
        peakAccuracy: finalStats.accuracy,
        gameDuration: Math.floor((finalStats.currentTime - finalStats.startTime) / 1000),
        monsters: monsterHistory,
        characterStatsUpdates: characterStatsUpdates.length > 0 ? characterStatsUpdates : undefined,
      });
      if (result) setUploadResult(result);

      toast({
        title: '游戏记录已保存',
        description: '数据已同步到服务器',
      });
    } catch (error) {
      console.error('保存游戏记录失败:', error);
      toast({
        title: '保存失败',
        description: '游戏记录保存失败，请检查网络连接',
        variant: 'destructive',
      });
    }
  }

  // 暂停游戏
  function handlePause() {
    if (!gameEngine) return;
    gameEngine.pause();
    setGameState('PAUSED');
  }

  // 继续游戏
  function handleResume() {
    if (!gameEngine) return;
    gameEngine.resume();
    setGameState('PLAYING');
  }

  // 切换音效
  function handleToggleSound() {
    const newEnabled = !soundEnabled;
    setSoundEnabled(newEnabled);
    soundSystem?.setEnabled(newEnabled);
  }

  // 切换全屏
  async function handleToggleFullscreen() {
    if (!containerRef.current) {
      toast({
        title: '全屏失败',
        description: '游戏容器未加载',
        variant: 'destructive',
      });
      return;
    }

    try {
      const elem = containerRef.current;
      
      // 检查是否已经全屏
      const isCurrentlyFullscreen = !!(
        document.fullscreenElement ||
        (document as any).webkitFullscreenElement ||
        (document as any).mozFullScreenElement ||
        (document as any).msFullscreenElement
      );

      if (!isCurrentlyFullscreen) {
        // 进入全屏 - 支持多种浏览器API
        if (elem.requestFullscreen) {
          await elem.requestFullscreen();
        } else if ((elem as any).webkitRequestFullscreen) {
          await (elem as any).webkitRequestFullscreen();
        } else if ((elem as any).mozRequestFullScreen) {
          await (elem as any).mozRequestFullScreen();
        } else if ((elem as any).msRequestFullscreen) {
          await (elem as any).msRequestFullscreen();
        } else {
          throw new Error('浏览器不支持全屏API');
        }
      } else {
        // 退出全屏 - 支持多种浏览器API
        if (document.exitFullscreen) {
          await document.exitFullscreen();
        } else if ((document as any).webkitExitFullscreen) {
          await (document as any).webkitExitFullscreen();
        } else if ((document as any).mozCancelFullScreen) {
          await (document as any).mozCancelFullScreen();
        } else if ((document as any).msExitFullscreen) {
          await (document as any).msExitFullscreen();
        }
      }
    } catch (err: any) {
      console.error('全屏切换失败:', err);
      toast({
        title: '全屏失败',
        description: err.message || '无法切换全屏模式，请检查浏览器设置',
        variant: 'destructive',
      });
    }
  }

  // 监听全屏状态变化
  useEffect(() => {
    function handleFullscreenChange() {
      const isFullscreen = !!(
        document.fullscreenElement ||
        (document as any).webkitFullscreenElement ||
        (document as any).mozFullScreenElement ||
        (document as any).msFullscreenElement
      );
      setIsFullscreen(isFullscreen);
    }

    // 监听多种浏览器的全屏事件
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
    document.addEventListener('mozfullscreenchange', handleFullscreenChange);
    document.addEventListener('MSFullscreenChange', handleFullscreenChange);

    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
      document.removeEventListener('mozfullscreenchange', handleFullscreenChange);
      document.removeEventListener('MSFullscreenChange', handleFullscreenChange);
    };
  }, []);

  // 返回配置页面
  function handleBack() {
    navigate('/prepare');
  }

  // 查看结果
  function handleViewResult() {
    setShowDetailedResult(true);
  }

  // 返回简要结果
  function handleBackToSummary() {
    setShowDetailedResult(false);
  }

  // 重新开始
  function handleRestart() {
    window.location.reload();
  }

  // 键盘事件处理
  useEffect(() => {
    if (!gameEngine || gameState !== 'PLAYING') return;

    function handleKeyDown(e: KeyboardEvent) {
      gameEngine?.handleKeyDown(e);
    }

    function handleKeyUp(e: KeyboardEvent) {
      gameEngine?.handleKeyUp(e);
    }

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [gameEngine, gameState]);

  // 更新统计
  useEffect(() => {
    if (!gameEngine || gameState !== 'PLAYING') return;

    const interval = setInterval(() => {
      setStats(gameEngine.getStats());
    }, 100);

    return () => clearInterval(interval);
  }, [gameEngine, gameState]);

  // 等待首轮参数时显示加载
  if (user && startParams === null && gameState !== 'GAME_OVER') {
    return (
      <div className="container max-w-4xl py-16 flex flex-col items-center justify-center gap-4">
        <p className="text-muted-foreground">正在加载关卡参数…</p>
      </div>
    );
  }

  // 游戏结束界面
  if (gameState === 'GAME_OVER' && stats) {
    // 获取字符统计数据
    const characterStats = gameEngine?.getCharacterStats() || [];
    
    // 按错误次数排序
    const errorRanking = [...characterStats]
      .filter(stat => stat.errorCount > 0)
      .sort((a, b) => b.errorCount - a.errorCount)
      .slice(0, 10);
    
    // 按准确率排序（只显示输入次数>=5的）
    const accuracyRanking = [...characterStats]
      .filter(stat => (stat.correctCount + stat.errorCount) >= 5)
      .sort((a, b) => a.accuracy - b.accuracy)
      .slice(0, 10);

    // 显示详细结果
    if (showDetailedResult) {
      return (
        <div className="container max-w-6xl py-8">
          <div className="space-y-6">
            <Card className="p-8">
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <h1 className="text-3xl font-bold">详细结果</h1>
                  <Button variant="outline" onClick={handleBackToSummary}>返回</Button>
                </div>

                {/* 总体统计 */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm text-muted-foreground">最终得分</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-3xl font-bold">{stats.score}</div>
                    </CardContent>
                  </Card>
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm text-muted-foreground">准确率</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-3xl font-bold">{stats.accuracy.toFixed(1)}%</div>
                    </CardContent>
                  </Card>
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm text-muted-foreground">打字速度</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-3xl font-bold">{Math.floor(stats.speed)}</div>
                      <div className="text-xs text-muted-foreground">字符/分钟</div>
                    </CardContent>
                  </Card>
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm text-muted-foreground">最长连击</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-3xl font-bold">{stats.longestStreak}</div>
                    </CardContent>
                  </Card>
                </div>

                {/* 游戏数据 */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div>
                    <div className="text-sm text-muted-foreground">轮次</div>
                    <div className="text-2xl font-bold">{stats.round}</div>
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">消灭怪兽</div>
                    <div className="text-2xl font-bold">{stats.monstersKilled}</div>
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">总字符数</div>
                    <div className="text-2xl font-bold">{stats.totalChars}</div>
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">错误字符</div>
                    <div className="text-2xl font-bold text-red-500">{stats.errorChars}</div>
                  </div>
                  {uploadResult?.currentDifficulty != null && (
                    <div>
                      <div className="text-sm text-muted-foreground">当前难度 Dₜ</div>
                      <div className="text-2xl font-bold">{Number(uploadResult.currentDifficulty).toFixed(2)}</div>
                    </div>
                  )}
                </div>

                {/* 字符错误排行 */}
                <Card>
                  <CardHeader>
                    <CardTitle>易错字符排行 Top 10</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {errorRanking.length > 0 ? (
                      <div className="space-y-2">
                        {errorRanking.map((stat, index) => {
                          const totalCount = stat.correctCount + stat.errorCount;
                          return (
                            <div key={stat.char} className="flex items-center justify-between p-3 bg-muted rounded-lg">
                              <div className="flex items-center gap-4">
                                <div className="text-lg font-bold text-muted-foreground w-8">#{index + 1}</div>
                                <div className="text-2xl font-mono font-bold">{stat.char}</div>
                              </div>
                              <div className="flex items-center gap-6">
                                <div className="text-right">
                                  <div className="text-sm text-muted-foreground">错误次数</div>
                                  <div className="text-xl font-bold text-red-500">{stat.errorCount}</div>
                                </div>
                                <div className="text-right">
                                  <div className="text-sm text-muted-foreground">总次数</div>
                                  <div className="text-xl font-bold">{totalCount}</div>
                                </div>
                                <div className="text-right">
                                  <div className="text-sm text-muted-foreground">准确率</div>
                                  <div className="text-xl font-bold">
                                    {(stat.accuracy * 100).toFixed(1)}%
                                  </div>
                                </div>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    ) : (
                      <div className="text-center text-muted-foreground py-8">
                        太棒了！没有错误字符 🎉
                      </div>
                    )}
                  </CardContent>
                </Card>

                {/* 准确率最低排行 */}
                <Card>
                  <CardHeader>
                    <CardTitle>准确率最低字符 Top 10</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {accuracyRanking.length > 0 ? (
                      <div className="space-y-2">
                        {accuracyRanking.map((stat, index) => {
                          const totalCount = stat.correctCount + stat.errorCount;
                          return (
                            <div key={stat.char} className="flex items-center justify-between p-3 bg-muted rounded-lg">
                              <div className="flex items-center gap-4">
                                <div className="text-lg font-bold text-muted-foreground w-8">#{index + 1}</div>
                                <div className="text-2xl font-mono font-bold">{stat.char}</div>
                              </div>
                              <div className="flex items-center gap-6">
                                <div className="text-right">
                                  <div className="text-sm text-muted-foreground">准确率</div>
                                  <div className={`text-xl font-bold ${stat.accuracy * 100 < 50 ? 'text-red-500' : stat.accuracy * 100 < 80 ? 'text-orange-500' : ''}`}>
                                    {(stat.accuracy * 100).toFixed(1)}%
                                  </div>
                                </div>
                                <div className="text-right">
                                  <div className="text-sm text-muted-foreground">正确/总数</div>
                                  <div className="text-xl font-bold">{stat.correctCount}/{totalCount}</div>
                                </div>
                                <div className="text-right">
                                  <div className="text-sm text-muted-foreground">错误次数</div>
                                  <div className="text-xl font-bold text-red-500">{stat.errorCount}</div>
                                </div>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    ) : (
                      <div className="text-center text-muted-foreground py-8">
                        暂无足够数据（需要至少输入5次）
                      </div>
                    )}
                  </CardContent>
                </Card>

                {/* 字符错误分布图表 */}
                <Card>
                  <CardHeader>
                    <CardTitle>字符错误分布图</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <ErrorDistributionChart characterStats={characterStats} />
                  </CardContent>
                </Card>

                {/* 操作按钮 */}
                <div className="flex gap-4 justify-center">
                  <Button onClick={handleRestart}>重新开始</Button>
                  <Button variant="outline" onClick={handleBack}>返回配置</Button>
                </div>
              </div>
            </Card>
          </div>
        </div>
      );
    }

    // 显示简要结果
    return (
      <div className="container max-w-4xl py-8">
        <div className="space-y-6">
          <Card className="p-8">
            <div className="text-center space-y-6">
              <h1 className="text-3xl font-bold text-red-500">游戏结束</h1>
              
              <div className="grid grid-cols-2 gap-4 text-left">
                <div>
                  <div className="text-sm text-muted-foreground">最终得分</div>
                  <div className="text-2xl font-bold">{stats.score}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">轮次</div>
                  <div className="text-2xl font-bold">{stats.round}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">准确率</div>
                  <div className="text-2xl font-bold">{stats.accuracy.toFixed(1)}%</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">速度</div>
                  <div className="text-2xl font-bold">{Math.floor(stats.speed)}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">消灭怪兽</div>
                  <div className="text-2xl font-bold">{stats.monstersKilled}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">最长连击</div>
                  <div className="text-2xl font-bold">{stats.longestStreak}</div>
                </div>
                {uploadResult?.currentDifficulty != null && (
                  <div>
                    <div className="text-sm text-muted-foreground">当前难度 Dₜ</div>
                    <div className="text-2xl font-bold">{Number(uploadResult.currentDifficulty).toFixed(2)}</div>
                  </div>
                )}
              </div>

              <div className="flex gap-4 justify-center">
                <Button onClick={handleViewResult}>查看详细结果</Button>
                <Button variant="outline" onClick={handleRestart}>重新开始</Button>
                <Button variant="ghost" onClick={handleBack}>返回配置</Button>
              </div>
            </div>
          </Card>
        </div>
      </div>
    );
  }

  // 游戏进行中界面
  return (
    <div 
      ref={containerRef}
      className={`${isFullscreen ? 'fixed inset-0 z-50 bg-background' : 'container max-w-[1400px]'} py-8`}
    >
      <div className="grid grid-cols-1 xl:grid-cols-4 gap-6 h-full">
        {/* 游戏画布 */}
        <div className="xl:col-span-3">
          <Card className="p-4 h-full flex items-center justify-center">
            {gameEngine && (
              <>
                <GameCanvas
                  width={1200}
                  height={800}
                  monsters={gameEngine.getMonsters()}
                  player={gameEngine.getPlayer()}
                  inputMode={gameEngine.getInputHandler().getInputMode()}
                  chineseBuffer={gameEngine.getInputHandler().getChineseBuffer()}
                  lockedMonsterId={gameEngine.getInputHandler().getLockedMonster()?.id || null}
                />
                {/* 错误反馈组件 */}
                <ErrorFeedback lastError={lastError} lastCorrect={lastCorrect} />
              </>
            )}
          </Card>
        </div>

        {/* 游戏UI */}
        <div className="xl:col-span-1">
          {stats && (
            <GameUI
              stats={stats}
              gameState={gameState}
              soundEnabled={soundEnabled}
              isFullscreen={isFullscreen}
              onPause={handlePause}
              onResume={handleResume}
              onToggleSound={handleToggleSound}
              onToggleFullscreen={handleToggleFullscreen}
            />
          )}
        </div>
      </div>
    </div>
  );
}
