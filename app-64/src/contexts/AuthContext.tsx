// 认证上下文
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { Profile } from '@/types/database';
import { getProfile } from '@/db/api';
import type { ApiError } from '@/db/client';
import {
  fetchCurrentUser,
  resolveUserId,
  signInRequest,
  signOutRequest,
  signUpRequest,
  type AuthSession,
} from '@/services/auth';

interface AuthContextType {
  user: Profile | null;
  profile: Profile | null;
  loading: boolean;
  signIn: (username: string, password: string) => Promise<void>;
  signUp: (username: string, password: string, nickname?: string) => Promise<void>;
  signOut: () => Promise<void>;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Profile | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void checkSession();
  }, []);

  async function checkSession() {
    const token = localStorage.getItem('auth_token');
    if (token) {
      try {
        const account = await fetchCurrentUser();
        const initialProfile = account as unknown as Profile;
        setUser(initialProfile);
        setProfile(initialProfile);

        const userId = resolveUserId(account);
        if (userId) {
          await loadProfile(userId);
        } else {
          console.error('账号数据缺少 userId:', account);
        }
      } catch (error) {
        const apiError = error as ApiError;
        console.error('会话检查失败:', apiError.message);
        if (apiError.code === 401) {
          localStorage.removeItem('auth_token');
        }
        setUser(null);
        setProfile(null);
      }
    }
    setLoading(false);
  }

  async function loadProfile(userId: number) {
    try {
      const profileData = await getProfile(userId);
      if (profileData) {
        setProfile(profileData);
        // 更新 user 以保持一致
        setUser(profileData);
      }
    } catch (error) {
      console.error('加载用户信息失败:', error);
      // 注意：这里不应该把 user/profile 置空，因为我们已经有基本信息了
    }
  }

  /** 写入会话并返回账号 id（token 的存储只在这里做）。 */
  function applySession(session: AuthSession): number | undefined {
    localStorage.setItem('auth_token', session.accessToken);
    if (session.refreshToken) {
      localStorage.setItem('refresh_token', session.refreshToken);
    }
    const initialProfile = session.user as unknown as Profile;
    setUser(initialProfile);
    setProfile(initialProfile);
    return resolveUserId(session.user);
  }

  async function signIn(username: string, password: string) {
    try {
      const session = await signInRequest(username, password);
      const userId = applySession(session);
      if (userId) {
        await loadProfile(userId);
      }
    } catch (error) {
      const apiError = error as ApiError;
      throw new Error(apiError.message || '登录失败');
    }
  }

  async function signUp(username: string, password: string, nickname?: string) {
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
      throw new Error('用户名只能包含字母、数字和下划线');
    }

    try {
      const session = await signUpRequest(username, password, nickname);
      const userId = applySession(session);
      if (userId) {
        await loadProfile(userId);
      }
    } catch (error) {
      const apiError = error as ApiError;
      throw new Error(apiError.message || '注册失败');
    }
  }

  async function signOut() {
    const refreshToken = localStorage.getItem('refresh_token');
    try {
      await signOutRequest(refreshToken);
    } finally {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('refresh_token');
      setUser(null);
      setProfile(null);
    }
  }

  const isAdmin = profile?.role === 'admin';

  return (
    <AuthContext.Provider
      value={{
        user,
        profile,
        loading,
        signIn,
        signUp,
        signOut,
        isAdmin,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth必须在AuthProvider内部使用');
  }
  return context;
}
