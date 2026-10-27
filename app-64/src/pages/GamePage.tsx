import { Maximize2, Minimize2, Pause, Play, Volume2, VolumeX } from 'lucide-react';
import { useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import ErrorFeedback from '@/components/game/ErrorFeedback';
import GameCanvas from '@/components/game/GameCanvas';
import GameUI from '@/components/game/GameUI';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { useGameConfig } from '@/contexts/GameConfigContext';
import { GameResultView } from '@/features/game/components/GameResultView';
import { useFullscreen } from '@/features/game/hooks/useFullscreen';
import { useGameSession } from '@/features/game/hooks/useGameSession';
import { GAME_CANVAS_HEIGHT, GAME_CANVAS_WIDTH } from '@/features/game/utils/gameRuntime';
import { useToast } from '@/hooks/use-toast';

export default function GamePage() {
  const { user } = useAuth();
  const { config } = useGameConfig();
  const navigate = useNavigate();
  const { toast } = useToast();
  const containerRef = useRef<HTMLDivElement>(null);
  const { isFullscreen, toggleFullscreen } = useFullscreen(containerRef);

  const handleAuthRequired = useCallback(() => {
    navigate('/login');
  }, [navigate]);

  const session = useGameSession({ user, config, onAuthRequired: handleAuthRequired });

  const handleToggleFullscreen = useCallback(async () => {
    try {
      await toggleFullscreen();
    } catch (error) {
      const message = error instanceof Error ? error.message : '无法切换全屏模式，请检查浏览器设置';
      toast({ title: '全屏失败', description: message, variant: 'destructive' });
    }
  }, [toast, toggleFullscreen]);

  const handleBackToPrepare = useCallback(() => {
    navigate('/apps/typing/prepare');
  }, [navigate]);

  if (user && session.startParams === null && session.gameState !== 'GAME_OVER') {
    return (
      <div className="container flex max-w-4xl flex-col items-center justify-center gap-4 py-24">
        <span className="label-mono text-muted-foreground">LOADING · 正在取关卡参数</span>
      </div>
    );
  }

  if (session.gameState === 'GAME_OVER' && session.stats) {
    return (
      <GameResultView
        stats={session.stats}
        characterStats={session.characterStats}
        uploadResult={session.uploadResult}
        showDetailedResult={session.showDetailedResult}
        onShowDetails={() => session.setShowDetailedResult(true)}
        onBackToSummary={() => session.setShowDetailedResult(false)}
        onRestart={session.restart}
        onBackToPrepare={handleBackToPrepare}
      />
    );
  }

  const playing = session.gameState === 'PLAYING';

  return (
    <div
      ref={containerRef}
      className={
        isFullscreen
          ? 'dark fixed inset-0 z-50 overflow-auto bg-background text-foreground'
          : 'dark min-h-[calc(100vh-4rem)] bg-background text-foreground'
      }
    >
      <div className={isFullscreen ? 'px-4 py-4' : 'container max-w-[1500px] py-6'}>
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border pb-3">
          <div className="flex items-baseline gap-3">
            <span className="status-light mt-1" />
            <span className="label-mono text-muted-foreground">STAGE · 对局台</span>
            <span className="font-serif text-lg leading-none">
              {session.gameState === 'PAUSED' ? '已暂停' : '字符正在靠近'}
            </span>
          </div>

          <div className="flex items-center gap-2">
            {playing || session.gameState === 'PAUSED' ? (
              <Button
                size="sm"
                variant={playing ? 'outline' : 'default'}
                onClick={playing ? session.pause : session.resume}
              >
                {playing ? <Pause aria-hidden /> : <Play aria-hidden />}
                {playing ? '暂停' : '继续'}
              </Button>
            ) : null}
            <Button size="sm" variant="ghost" onClick={session.toggleSound}>
              {session.soundEnabled ? <Volume2 aria-hidden /> : <VolumeX aria-hidden />}
              {session.soundEnabled ? '音效 开' : '音效 关'}
            </Button>
            <Button size="sm" variant="ghost" onClick={handleToggleFullscreen}>
              {isFullscreen ? <Minimize2 aria-hidden /> : <Maximize2 aria-hidden />}
              {isFullscreen ? '退出全屏' : '全屏'}
            </Button>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 border border-border xl:grid-cols-[minmax(0,1fr)_320px]">
          <div className="relative border-b border-border xl:border-b-0 xl:border-r">
            {session.gameEngine && (
              <>
                <GameCanvas
                  width={GAME_CANVAS_WIDTH}
                  height={GAME_CANVAS_HEIGHT}
                  monsters={session.gameEngine.getMonsters()}
                  player={session.gameEngine.getPlayer()}
                  inputMode={session.gameEngine.getInputHandler().getInputMode()}
                  chineseBuffer={session.gameEngine.getInputHandler().getChineseBuffer()}
                  lockedMonsterId={session.gameEngine.getInputHandler().getLockedMonster()?.id || null}
                  progress01={session.gameEngine.getProgress01()}
                  lastCorrect={session.lastCorrect}
                />
                <ErrorFeedback lastError={session.lastError} lastCorrect={session.lastCorrect} />
              </>
            )}
            <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 border-t border-border px-4 py-2">
              <span className="label-mono text-muted-foreground">首字符锁定 · 打完即灭 · Tab 换目标</span>
              <span className="label-mono text-muted-foreground">
                {isFullscreen ? 'FULLSCREEN' : `STAGE ${GAME_CANVAS_WIDTH}×${GAME_CANVAS_HEIGHT}`}
              </span>
            </div>
          </div>

          <aside className="bg-card">{session.stats && <GameUI stats={session.stats} gameState={session.gameState} />}</aside>
        </div>
      </div>
    </div>
  );
}
