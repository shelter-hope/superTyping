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
 *
 * 版式：纸与墨。左栏名册（发丝线分隔的行），右栏申请 / 黑名单 / 说明。
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { MessageCircle, Trash2, Search, Loader2, Ban, UserPlus } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { useImSocket } from '@/hooks/use-im-socket';
import type { FriendRequestDTO, FriendResponseDTO } from '@/types/database';
import {
  getFriendList,
  searchUsers,
  sendFriendRequest,
  deleteFriend,
  getFriendRequests,
  handleFriendRequest,
  blockFriend,
  unblockFriend,
  getBlockedFriends,
} from '@/db/api';

/** 列表里的方框头像：姓名单字，发丝线边框，不用圆形徽章 */
function SquareAvatar({
  name,
  size = 'md',
}: {
  name?: string;
  size?: 'md' | 'sm';
}) {
  const box = size === 'md' ? 'h-10 w-10 text-[14px]' : 'h-8 w-8 text-[12px]';
  return (
    <span
      aria-hidden
      className={`flex shrink-0 items-center justify-center border border-border bg-muted font-serif text-foreground ${box}`}
    >
      {name?.[0]?.toUpperCase() || '?'}
    </span>
  );
}

/** 日期：月/日 时:分，等宽数字 */
function formatMoment(value?: string) {
  if (!value) return '';
  return new Date(value).toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

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
  const [requests, setRequests] = useState<FriendRequestDTO[]>([]);
  const [blocked, setBlocked] = useState<FriendResponseDTO[]>([]);

  useImSocket((event) => {
    if (event.type === 'PRESENCE') {
      setFriends((prev) => prev.map((friend) =>
        friend.userId === event.userId ? { ...friend, isOnline: event.online } : friend
      ));
    }
    if (event.type === 'FRIEND_REQUEST') {
      loadRequests();
    }
  }, !!user);

  useEffect(() => {
    if (user) {
      loadFriends();
      loadRequests();
      loadBlocked();
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

  async function loadRequests() {
    try {
      setRequests(await getFriendRequests());
    } catch (error) {
      console.error('加载好友申请失败:', error);
    }
  }

  async function loadBlocked() {
    try {
      setBlocked(await getBlockedFriends());
    } catch (error) {
      console.error('加载拉黑列表失败:', error);
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
      await sendFriendRequest(toUserId, requestMessage);

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

  async function onDecide(requestId: number, status: 'accepted' | 'rejected') {
    try {
      await handleFriendRequest(requestId, status);
      toast({ title: status === 'accepted' ? '已同意' : '已拒绝' });
      loadRequests();
      loadFriends();
    } catch (error) {
      toast({
        title: '操作失败',
        description: error instanceof Error ? error.message : '无法处理申请',
        variant: 'destructive',
      });
    }
  }

  async function onBlock(friendId: number) {
    if (!confirm('拉黑后对方不能再给你发消息。确定拉黑？')) return;
    try {
      await blockFriend(friendId);
      toast({ title: '已拉黑' });
      loadFriends();
      loadBlocked();
    } catch (error) {
      toast({
        title: '拉黑失败',
        description: error instanceof Error ? error.message : '无法拉黑',
        variant: 'destructive',
      });
    }
  }

  async function onUnblock(friendId: number) {
    try {
      await unblockFriend(friendId);
      toast({ title: '已解除拉黑' });
      loadFriends();
      loadBlocked();
    } catch (error) {
      toast({
        title: '解除失败',
        description: error instanceof Error ? error.message : '无法解除拉黑',
        variant: 'destructive',
      });
    }
  }

  function handleOpenChat(friendId: number) {
    navigate(`/chat/${friendId}`);
  }

  if (loading) {
    return (
      <PageBody>
        <PageHeader
          eyebrow="关系 · ROSTER"
          title="好友"
          description="正在读取好友名单、待处理申请与黑名单。"
        />
        <div className="mt-10 flex items-center gap-3 border-t border-border pt-6">
          <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
          <span className="label-mono text-[10px]">载入中 · LOADING</span>
        </div>
      </PageBody>
    );
  }

  return (
    <PageBody>
      <PageHeader
        eyebrow="关系 · ROSTER"
        title="好友"
        description={`共 ${friends.length} 位好友 · ${requests.length} 条待处理申请${
          blocked.length > 0 ? ` · ${blocked.length} 位已拉黑` : ''
        }`}
        actions={
          <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <UserPlus className="h-4 w-4" />
                添加好友
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>添加好友</DialogTitle>
                <DialogDescription>搜索用户名或昵称来添加好友</DialogDescription>
              </DialogHeader>
              <div className="space-y-5">
                <div className="flex gap-2">
                  <Input
                    placeholder="输入用户名或昵称"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                  />
                  <Button variant="outline" onClick={handleSearch} disabled={searching}>
                    {searching ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Search className="h-4 w-4" />
                    )}
                    搜索
                  </Button>
                </div>

                <div>
                  <p className="label-mono text-[10px]">结果 · RESULTS</p>
                  {searchResults.length === 0 ? (
                    <p className="mt-2 border-y border-border py-8 text-[13px] text-muted-foreground">
                      {searchQuery ? '没有找到匹配的用户。' : '输入关键词后回车搜索。'}
                    </p>
                  ) : (
                    <ScrollArea className="mt-2 h-[240px] border-y border-border">
                      <ul className="divide-y divide-border">
                        {searchResults.map((result) => (
                          <li
                            key={result.userId ?? result.id}
                            className="flex items-center gap-3 py-3"
                          >
                            <SquareAvatar name={result.username} size="sm" />
                            <div className="min-w-0 flex-1">
                              <p className="truncate font-serif text-[14px]">{result.username}</p>
                              <p className="mono-num mt-0.5 text-[10px] text-muted-foreground">
                                UID {result.userId ?? result.id}
                              </p>
                            </div>
                            <Button
                              size="sm"
                              onClick={() => onSendRequest(result.userId ?? result.id)}
                            >
                              <UserPlus className="h-3.5 w-3.5" />
                              添加
                            </Button>
                          </li>
                        ))}
                      </ul>
                    </ScrollArea>
                  )}
                </div>

                <div>
                  <label
                    htmlFor="friend-request-message"
                    className="label-mono mb-2 block text-[10px]"
                  >
                    附加消息 · OPTIONAL
                  </label>
                  <Textarea
                    id="friend-request-message"
                    className="min-h-[72px] resize-none rounded-none border-border bg-transparent text-[13px]"
                    placeholder="向对方介绍一下自己…"
                    value={requestMessage}
                    maxLength={200}
                    onChange={(e) => setRequestMessage(e.target.value)}
                    rows={3}
                  />
                  <p className="mono-num mt-1 text-right text-[10px] text-muted-foreground">
                    {requestMessage.length} / 200
                  </p>
                </div>
              </div>
            </DialogContent>
          </Dialog>
        }
      />

      <div className="mt-10 grid gap-10 lg:grid-cols-12 lg:gap-0">
        {/* ─── 左栏：名册 ─── */}
        <section className="lg:col-span-8 lg:pr-10">
          <div className="flex flex-wrap items-baseline justify-between gap-2 border-t border-border pt-4">
            <div>
              <p className="label-mono text-[10px]">名单 · ROSTER</p>
              <h2 className="mt-1 font-serif text-[17px] leading-none">全部好友</h2>
            </div>
            <span className="mono-num text-[11px] text-muted-foreground">
              {friends.length} 位
            </span>
          </div>

          {friends.length === 0 ? (
            <p className="mt-4 border-t border-border pt-8 text-[13px] text-muted-foreground">
              还没有好友。用右上角的「添加好友」搜索用户名。
            </p>
          ) : (
            <ul className="mt-3 divide-y divide-border border-t border-border">
              {friends.map((friend) => (
                <li key={friend.id ?? friend.userId} className="flex items-center gap-4 py-4">
                  <SquareAvatar name={friend.username} />
                  <div className="min-w-0 flex-1">
                    <p className="flex flex-wrap items-baseline gap-x-2 gap-y-1 font-serif text-[15px] leading-none">
                      <span className="truncate">{friend.username}</span>
                      <span
                        className={`label-mono text-[9px] ${
                          friend.isOnline ? 'text-success' : 'text-muted-foreground/70'
                        }`}
                      >
                        {friend.isOnline ? '● 在线' : '○ 离线'}
                      </span>
                    </p>
                    <p className="mt-1.5 truncate text-[12px] leading-5 text-muted-foreground">
                      {friend.bio || `UID ${friend.userId} · 还没有个性签名`}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-1">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => handleOpenChat(friend.userId ?? friend.id)}
                    >
                      <MessageCircle className="h-3.5 w-3.5" />
                      聊天
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      className="h-8 w-8"
                      aria-label={`拉黑 ${friend.username}`}
                      title="拉黑"
                      onClick={() => onBlock(friend.userId ?? friend.id)}
                    >
                      <Ban className="h-3.5 w-3.5" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      className="h-8 w-8 text-muted-foreground hover:text-accent"
                      aria-label={`删除好友 ${friend.username}`}
                      title="删除好友"
                      onClick={() => onDeleteFriend(friend.userId ?? friend.id)}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* ─── 右栏：申请 / 黑名单 / 说明 ─── */}
        <aside className="lg:col-span-4 lg:border-l lg:border-border lg:pl-8">
          <section>
            <div className="flex items-baseline justify-between gap-2 border-t border-border pt-4">
              <div>
                <p className="label-mono text-[10px]">请求 · REQUESTS</p>
                <h2 className="mt-1 font-serif text-[17px] leading-none">好友请求</h2>
              </div>
              <span className="mono-num text-[11px] text-muted-foreground">
                {requests.length} 条
              </span>
            </div>

            {requests.length === 0 ? (
              <p className="mt-4 border-t border-border pt-5 text-[12.5px] text-muted-foreground">
                没有待处理的申请。
              </p>
            ) : (
              <ul className="mt-3 divide-y divide-border border-t border-border">
                {requests.map((request) => (
                  <li key={request.id} className="py-4">
                    <div className="flex items-start gap-3">
                      <SquareAvatar name={request.senderName} size="sm" />
                      <div className="min-w-0 flex-1">
                        <p className="truncate font-serif text-[14px] leading-none">
                          {request.senderName}
                        </p>
                        <p className="mt-1.5 truncate text-[12px] text-muted-foreground">
                          {request.applyMessage || '请求添加你为好友'}
                        </p>
                        <p className="label-mono mt-1.5 text-[9px]">
                          {formatMoment(request.sendTime)}
                        </p>
                      </div>
                    </div>
                    <div className="mt-2.5 flex justify-end gap-2">
                      <Button size="sm" onClick={() => onDecide(request.id, 'accepted')}>
                        同意
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => onDecide(request.id, 'rejected')}
                      >
                        拒绝
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {blocked.length > 0 && (
            <section className="mt-8">
              <div className="flex items-baseline justify-between gap-2 border-t border-border pt-4">
                <div>
                  <p className="label-mono text-[10px]">黑名单 · BLOCKED</p>
                  <h2 className="mt-1 font-serif text-[17px] leading-none">已拉黑</h2>
                </div>
                <span className="mono-num text-[11px] text-muted-foreground">
                  {blocked.length} 位
                </span>
              </div>
              <ul className="mt-3 divide-y divide-border border-t border-border">
                {blocked.map((friend) => (
                  <li
                    key={friend.userId}
                    className="flex items-center justify-between gap-3 py-2.5"
                  >
                    <span className="truncate text-[13px] text-muted-foreground">
                      {friend.username}
                    </span>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => onUnblock(friend.userId)}
                    >
                      解除拉黑
                    </Button>
                  </li>
                ))}
              </ul>
            </section>
          )}

          <p className="pull-quote mt-8 text-[11.5px] leading-[1.9] text-muted-foreground">
            好友申请会实时推送。拉黑后对方发来的消息会被拒收，但不会通知对方。
          </p>
        </aside>
      </div>
    </PageBody>
  );
}
