// 认证上下文
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { Profile } from '@/types/database';
import { getProfile } from '@/db/api';
import { apiClient } from '@/db/client';

interface AuthContextType {
  user: Profile | null; // 使用 Profile 作为 User 类型，或者创建一个专门的 User 类型
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
    checkSession();
  }, []);

  async function checkSession() {
    const token = localStorage.getItem('auth_token');
    if (token) {
      try {
        const response = await apiClient.get('/auth/me');
        const userData = response.data.data;
        
        // 优先设置基本用户信息，保证页面能立即显示
        // userData 是 UserDTO，缺少 Profile 的部分字段，但包含 username/nickname/avatar
        const initialProfile = userData as unknown as Profile;
        setUser(initialProfile);
        setProfile(initialProfile); // 先用基本信息填充 profile

        // 尝试获取 userId，兼容可能的字段名差异
        const userId = userData.userId || (userData as any).id;
        
        // 然后加载完整 Profile
        if (userId) {
          await loadProfile(userId);
        } else {
          console.error('User data missing userId:', userData);
        }
      } catch (error) {
        console.error('Session check failed:', error);
        localStorage.removeItem('auth_token');
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

  async function signIn(username: string, password: string) {
    try {
      const response = await apiClient.post('/auth/login', { username, password });
      const { accessToken, user: userData } = response.data.data;
      
      localStorage.setItem('auth_token', accessToken);
      
      const initialProfile = userData as unknown as Profile;
      setUser(initialProfile);
      setProfile(initialProfile);
      
      const userId = userData.userId || (userData as any).id;

      if (userId) {
        await loadProfile(userId);
      }
    } catch (error) {
      throw error;
    }
  }

  async function signUp(username: string, password: string, nickname?: string) {
    // 验证用户名格式
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
      throw new Error('用户名只能包含字母、数字和下划线');
    }

    try {
      const response = await apiClient.post('/auth/signup', { 
        username, 
        password,
        nickname: nickname || username 
      });
      const { accessToken, user: userData } = response.data.data;
      
      localStorage.setItem('auth_token', accessToken);
      
      const initialProfile = userData as unknown as Profile;
      setUser(initialProfile);
      setProfile(initialProfile);
      
      const userId = userData.userId || (userData as any).id;

      if (userId) {
        await loadProfile(userId);
      }
    } catch (error) {
      throw error;
    }
  }

  async function signOut() {
    try {
      // await apiClient.post('/auth/logout'); // Optional
    } finally {
      localStorage.removeItem('auth_token');
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
