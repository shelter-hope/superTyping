/**
 * 好友管理页面
 * 
 * @description 管理好友列表，添加好友
 * 
 * @features
 * - 好友列表展示
 * - 搜索用户
 * - 发送好友请求
 * - 删除好友
 * - 跳转到聊天
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Users, UserPlus, MessageCircle, Trash2, Search, Loader2 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import type { FriendResponseDTO } from '@/types/database';
import { 
  getFriendList, 
  searchUsers, 
  sendFriendRequest, 
  deleteFriend 
} from '@/db/api';

export default function FriendsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();
  // friendList returns FriendResponseDTO which is close to Friend but flatter?
  // Backend returns FriendResponseDTO: { id (friendshipId), userId, friendId, friendName, friendAvatar, ... }
  // Frontend Friend type: { id, userId, friendId, status, friend: Profile ... }
  // We need to adapt or just use FriendResponseDTO if api.ts returns it.
  // api.ts says: getFriendList(): Promise<FriendResponseDTO[]>
  const [friends, setFriends] = useState<FriendResponseDTO[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<FriendResponseDTO[]>([]); // searchUsers returns FriendResponseDTO list (users found)
  const [searching, setSearching] = useState(false);
  const [loading, setLoading] = useState(true);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [requestMessage, setRequestMessage] = useState('');

  useEffect(() => {
    if (user) {
      loadFriends();
    }
  }, [user]);

  async function loadFriends() {
    setLoading(true);
    try {
      const data = await getFriendList();
      setFriends(data);
    } catch (error) {
      console.error('加载好友列表失败:', error);
    } finally {
      setLoading(false);
    }
  }

  async function handleSearch() {
    if (!searchQuery.trim()) return;

    setSearching(true);
    try {
      // Backend search should exclude self and existing friends if possible, or we filter client side
      const data = await searchUsers(searchQuery);
      // Filter out self and existing friends if backend doesn't
      // Assuming backend returns simple user info wrapped in DTO
      // FriendResponseDTO has friendId, friendName etc.
      
      setSearchResults(data);
    } catch (error) {
      console.error('搜索用户失败:', error);
      toast({
        title: '搜索失败',
        description: '无法搜索用户',
        variant: 'destructive',
      });
    } finally {
      setSearching(false);
    }
  }

  async function onSendRequest(toUserId: number) {
    try {
      // Backend should handle duplicate checks
      await sendFriendRequest(toUserId);

      toast({
        title: '好友请求已发送',
        description: '等待对方确认',
      });

      setAddDialogOpen(false);
      setSearchQuery('');
      setSearchResults([]);
      setRequestMessage('');
    } catch (error) {
      const message = error instanceof Error ? error.message : '无法发送好友请求';
      console.error('发送好友请求失败:', error);
      // Check if error message indicates duplicate
      if (message.includes('exist')) {
        toast({
            title: '已发送过请求',
            description: '请等待对方回应',
          });
      } else {
        toast({
            title: '发送失败',
            description: message,
            variant: 'destructive',
          });
      }
    }
  }

  async function onDeleteFriend(friendId: number) {
    if (!confirm('确定要删除这个好友吗？')) return;

    try {
      await deleteFriend(friendId);

      toast({
        title: '已删除好友',
      });

      loadFriends();
    } catch (error) {
      console.error('删除好友失败:', error);
      toast({
        title: '删除失败',
        description: '无法删除好友',
        variant: 'destructive',
      });
    }
  }

  function handleOpenChat(friendId: number) {
    navigate(`/chat/${friendId}`);
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="w-8 h-8 animate-spin" />
      </div>
    );
  }

  return (
    <div className="container max-w-4xl py-8">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Users className="w-5 h-5" />
                好友管理
              </CardTitle>
              <CardDescription>
                共 {friends.length} 位好友
              </CardDescription>
            </div>
            <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
              <DialogTrigger asChild>
                <Button>
                  <UserPlus className="w-4 h-4 mr-2" />
                  添加好友
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>添加好友</DialogTitle>
                  <DialogDescription>
                    搜索用户名或昵称来添加好友
                  </DialogDescription>
                </DialogHeader>
                <div className="space-y-4">
                  <div className="flex gap-2">
                    <Input
                      placeholder="输入用户名或昵称"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                    />
                    <Button onClick={handleSearch} disabled={searching}>
                      {searching ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : (
                        <Search className="w-4 h-4" />
                      )}
                    </Button>
                  </div>

                  <ScrollArea className="h-[300px]">
                    {searchResults.length === 0 ? (
                      <div className="text-center text-muted-foreground py-8">
                        {searchQuery ? '未找到用户' : '输入关键词搜索用户'}
                      </div>
                    ) : (
                      <div className="space-y-2">
                        {searchResults.map((user) => (
                          <div
                            key={user.userId ?? user.id}
                            className="flex items-center gap-3 p-3 rounded-lg border"
                          >
                            <Avatar>
                              <AvatarFallback>
                                {user.username?.[0]?.toUpperCase() || '?'}
                              </AvatarFallback>
                            </Avatar>
                            <div className="flex-1 min-w-0">
                              <p className="font-medium">{user.username}</p>
                              <p className="text-sm text-muted-foreground truncate">
                                用户ID: {user.userId ?? user.id}
                              </p>
                            </div>
                            <Button
                              size="sm"
                              onClick={() => onSendRequest(user.userId ?? user.id)}
                            >
                              <UserPlus className="w-4 h-4 mr-1" />
                              添加
                            </Button>
                          </div>
                        ))}
                      </div>
                    )}
                  </ScrollArea>

                  <div>
                    <label className="text-sm font-medium mb-2 block">
                      附加消息（可选）
                    </label>
                    <Textarea
                      placeholder="向对方介绍一下自己..."
                      value={requestMessage}
                      onChange={(e) => setRequestMessage(e.target.value)}
                      rows={3}
                    />
                  </div>
                </div>
              </DialogContent>
            </Dialog>
          </div>
        </CardHeader>

        <CardContent>
          <ScrollArea className="h-[600px]">
            {friends.length === 0 ? (
              <div className="text-center text-muted-foreground py-12">
                <Users className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>还没有好友</p>
                <p className="text-sm mt-2">点击"添加好友"按钮开始添加</p>
              </div>
            ) : (
              <div className="space-y-2">
                {friends.map((friend) => (
                  <div
                    key={friend.id ?? friend.userId}
                    className="flex items-center gap-3 p-4 rounded-lg border hover:bg-muted transition-colors"
                  >
                    <Avatar className="w-12 h-12">
                      <AvatarFallback>
                        {friend.username?.[0]?.toUpperCase() || '?'}
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium">{friend.username}</p>
                      <p className="text-sm text-muted-foreground truncate">
                        好友ID: {friend.userId ?? friend.id}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        onClick={() => handleOpenChat(friend.userId ?? friend.id)}
                      >
                        <MessageCircle className="w-4 h-4 mr-1" />
                        聊天
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => onDeleteFriend(friend.userId ?? friend.id)}
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </ScrollArea>
        </CardContent>
      </Card>
    </div>
  );
}
