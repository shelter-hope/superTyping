import { useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import ErrorFeedback from '@/components/game/ErrorFeedback';
import GameCanvas from '@/components/game/GameCanvas';
import GameUI from '@/components/game/GameUI';
import { Card } from '@/components/ui/card';
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
    navigate('/prepare');
  }, [navigate]);

  if (user && session.startParams === null && session.gameState !== 'GAME_OVER') {
    return (
      <div className="container flex max-w-4xl flex-col items-center justify-center gap-4 py-16">
        <p className="text-muted-foreground">正在加载关卡参数...</p>
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

  return (
    <div
      ref={containerRef}
      className={`${isFullscreen ? 'fixed inset-0 z-50 bg-background' : 'container max-w-[1400px]'} py-8`}
    >
      <div className="grid h-full grid-cols-1 gap-6 xl:grid-cols-4">
        <div className="xl:col-span-3">
          <Card className="flex h-full items-center justify-center p-4">
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
          </Card>
        </div>

        <div className="xl:col-span-1">
          {session.stats && (
            <GameUI
              stats={session.stats}
              gameState={session.gameState}
              soundEnabled={session.soundEnabled}
              isFullscreen={isFullscreen}
              onPause={session.pause}
              onResume={session.resume}
              onToggleSound={session.toggleSound}
              onToggleFullscreen={handleToggleFullscreen}
            />
          )}
        </div>
      </div>
    </div>
  );
}
