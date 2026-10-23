import { useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useGameConfig } from '@/contexts/GameConfigContext';
import { getUserConfig } from '@/services/user';

/** 登录后把服务端音效开关同步到本地配置，让设置页和对局使用同一份偏好。 */
export function SoundPreferenceSync() {
  const { user } = useAuth();
  const { updateConfig } = useGameConfig();
  const userId = user?.userId;

  useEffect(() => {
    if (!userId) return;
    let cancelled = false;
    getUserConfig()
      .then((config) => {
        if (cancelled || typeof config?.isSoundEffect !== 'boolean') return;
        updateConfig({ soundEnabled: config.isSoundEffect });
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [userId, updateConfig]);

  return null;
}
