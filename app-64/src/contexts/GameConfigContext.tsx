// 游戏配置上下文
import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import type { GameConfig } from '@/types/game';

interface GameConfigContextType {
  config: GameConfig;
  updateConfig: (updates: Partial<GameConfig>) => void;
  resetConfig: () => void;
}

const defaultConfig: GameConfig = {
  enableLowercase: true,
  enableUppercase: false,
  enableNumbers: false,
  enablePunctuation: false,
  enableOperators: false,
  enableSpecialChars: false,
  enableChinese: false,
  gameMode: 'endless',
  soundEnabled: true,
};

const GameConfigContext = createContext<GameConfigContextType | undefined>(undefined);

export function GameConfigProvider({ children }: { children: ReactNode }) {
  const [config, setConfig] = useState<GameConfig>(() => {
    // 从localStorage加载配置；兼容旧版 timed/survival -> limit
    const saved = localStorage.getItem('gameConfig');
    if (saved) {
      try {
        const parsed = { ...defaultConfig, ...JSON.parse(saved) };
        if (parsed.gameMode === 'timed' || parsed.gameMode === 'survival') {
          parsed.gameMode = 'limit';
        }
        if (parsed.gameMode !== 'endless' && parsed.gameMode !== 'dictionary' && parsed.gameMode !== 'limit') {
          parsed.gameMode = 'endless';
        }
        return parsed;
      } catch {
        return defaultConfig;
      }
    }
    return defaultConfig;
  });

  const updateConfig = useCallback((updates: Partial<GameConfig>) => {
    setConfig(prev => {
      const newConfig = { ...prev, ...updates };
      localStorage.setItem('gameConfig', JSON.stringify(newConfig));
      return newConfig;
    });
  }, []);

  const resetConfig = useCallback(() => {
    setConfig(defaultConfig);
    localStorage.setItem('gameConfig', JSON.stringify(defaultConfig));
  }, []);

  return (
    <GameConfigContext.Provider value={{ config, updateConfig, resetConfig }}>
      {children}
    </GameConfigContext.Provider>
  );
}

export function useGameConfig() {
  const context = useContext(GameConfigContext);
  if (context === undefined) {
    throw new Error('useGameConfig必须在GameConfigProvider内部使用');
  }
  return context;
}
