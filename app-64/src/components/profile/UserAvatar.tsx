// 用户头像组件
import { getDefaultAvatar } from '@/lib/avatars';
import { resolveBackendAssetUrl } from '@/db/client';
import type { Profile } from '@/types/database';

interface UserAvatarProps {
  profile: Profile | null;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const sizeClasses = {
  sm: 'w-8 h-8',
  md: 'w-12 h-12',
  lg: 'w-16 h-16',
  xl: 'w-24 h-24',
};

export default function UserAvatar({ profile, size = 'md', className = '' }: UserAvatarProps) {
  if (!profile) return null;

  const rawAvatar = profile.avatar || getDefaultAvatar(profile.username);
  const avatarUrl = rawAvatar ? resolveBackendAssetUrl(rawAvatar) : getDefaultAvatar(profile.username);
  const sizeClass = sizeClasses[size];

  return (
    <img
      src={avatarUrl}
      alt={profile.nickname || profile.username}
      className={`${sizeClass} rounded-full border-2 border-primary object-cover ${className}`}
    />
  );
}
