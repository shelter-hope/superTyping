/**
 * 消息中心页面
 *
 * @description 显示系统消息、好友消息和好友请求
 *
 * @features
 * - 系统消息列表
 * - 好友消息列表（会话列表）
 * - 好友请求通知
 * - 未读消息提示
 * - 跳转到聊天界面
 *
 * 版式：纸与墨。信箱式分栏标签页，全部列表为发丝线横线行。
 */

import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Loader2 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { useImSocket } from '@/hooks/use-im-socket';
import type { MessageDTO, FriendRequestDTO } from '@/types/database';
import {
  getSystemMessages,
  getConversations,
  getFriendRequests,
  handleFriendRequest,
  markMessageAsRead,
  markNotificationAsRead,
} from '@/db/api';

/** 方框头像：姓名单字，发丝线边框 */
function SquareAvatar({ name, size = 'md' }: { name?: string; size?: 'md' | 'sm' }) {
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

/** 会话时间：月/日 时:分 */
function formatMoment(value?: string) {
  if (!value) return '';
  return new Date(value).toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** 标签页下的小节抬头 */
function SectionHead({
  eyebrow,
  title,
  count,
}: {
  eyebrow: string;
  title: string;
  count: string;
}) {
  return (
    <div className="mt-6 flex flex-wrap items-baseline justify-between gap-2">
      <div>
        <p className="label-mono text-[10px]">{eyebrow}</p>
        <h2 className="mt-1 font-serif text-[17px] leading-none">{title}</h2>
      </div>
      <span className="mono-num text-[11px] text-muted-foreground">{count}</span>
    </div>
  );
}

/**
 * 会话列表项：后端 getConversations 返回 MessageDTO[]，其中 senderId/senderName 为对方用户
 */
export default function MessageCenterPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { toast } = useToast();
  const [systemMessages, setSystemMessages] = useState<MessageDTO[]>([]);
  const [conversations, setConversations] = useState<MessageDTO[]>([]);
  const [friendRequests, setFriendRequests] = useState<FriendRequestDTO[]>([]);
  const [loading, setLoading] = useState(true);

  // 进入消息中心或从聊天返回时拉取数据，保证未读数是最新的
  useEffect(() => {
    const onMessagesPage = location.pathname === '/messages' || location.pathname === '/message-center';
    if (user && onMessagesPage) {
      loadData();
    }
  }, [user, location.pathname]);

  useImSocket((event) => {
    if (event.type === 'MESSAGE' || event.type === 'RECALL') loadConversations();
    if (event.type === 'FRIEND_REQUEST') loadFriendRequests();
  }, !!user);

  async function loadData() {
    setLoading(true);
    try {
      await Promise.all([loadSystemMessages(), loadConversations(), loadFriendRequests()]);
    } catch (error) {
      console.error('加载数据失败:', error);
    } finally {
      setLoading(false);
    }
  }

  async function loadSystemMessages() {
    try {
      const messages = await getSystemMessages();
      setSystemMessages(messages);
    } catch (error) {
      console.error('Failed to load system messages', error);
    }
  }

  async function loadConversations() {
    try {
      const data = await getConversations();
      setConversations(data);
    } catch (error) {
      console.error('Failed to load conversations', error);
    }
  }

  async function loadFriendRequests() {
    try {
      const requests = await getFriendRequests();
      setFriendRequests(requests);
    } catch (error) {
      console.error('Failed to load friend requests', error);
    }
  }

  async function onAcceptRequest(requestId: number, fromUserId: number) {
    try {
      await handleFriendRequest(requestId, 'accepted');
      toast({
        title: '已接受好友请求',
        description: '你们现在是好友了',
      });
      loadData();
    } catch (error) {
      console.error('接受好友请求失败:', error);
      toast({
        title: '操作失败',
        description: '无法接受好友请求',
        variant: 'destructive',
      });
    }
  }

  async function onRejectRequest(requestId: number) {
    try {
      await handleFriendRequest(requestId, 'rejected');
      toast({
        title: '已拒绝好友请求',
      });
      loadData();
    } catch (error) {
      console.error('拒绝好友请求失败:', error);
      toast({
        title: '操作失败',
        description: '无法拒绝好友请求',
        variant: 'destructive',
      });
    }
  }

  async function onMarkAsRead(message: MessageDTO) {
    try {
      if (message.type === 'notification') {
        await markNotificationAsRead(message.id);
      } else {
        await markMessageAsRead(message.id);
      }
      loadSystemMessages();
    } catch (error) {
      console.error('标记已读失败:', error);
    }
  }

  function handleOpenChat(friendId: number) {
    navigate(`/chat/${friendId}`);
  }

  const unreadSystemCount = systemMessages.filter(m => !m.isRead).length;
  const unreadConversationCount = conversations.reduce((sum, c) => sum + (c.unreadCount ?? 0), 0);
  const pendingRequestCount = friendRequests.length;

  if (loading) {
    return (
      <PageBody>
        <PageHeader
          eyebrow="信箱 · INBOX"
          title="消息中心"
          description="正在整理会话、好友请求与系统消息。"
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
        eyebrow="信箱 · INBOX"
        title="消息中心"
        description={`${conversations.length} 个会话 · ${pendingRequestCount} 条好友请求 · ${unreadSystemCount} 条未读系统消息`}
      />

      <Tabs defaultValue="conversations" className="mt-8 w-full">
        <TabsList className="flex h-auto w-full items-stretch justify-start gap-7 rounded-none border-b border-border bg-transparent p-0">
          <TabsTrigger
            value="conversations"
            className="-mb-px h-auto flex-none items-center rounded-none border-0 border-b-2 border-transparent bg-transparent px-0 pb-3 pt-1 text-[13px] font-normal text-muted-foreground shadow-none data-[state=active]:border-foreground data-[state=active]:bg-transparent data-[state=active]:text-foreground data-[state=active]:shadow-none"
          >
            好友消息
            {unreadConversationCount > 0 && (
              <span className="mono-num ml-2 text-[11px] text-accent">{unreadConversationCount}</span>
            )}
          </TabsTrigger>
          <TabsTrigger
            value="requests"
            className="-mb-px h-auto flex-none items-center rounded-none border-0 border-b-2 border-transparent bg-transparent px-0 pb-3 pt-1 text-[13px] font-normal text-muted-foreground shadow-none data-[state=active]:border-foreground data-[state=active]:bg-transparent data-[state=active]:text-foreground data-[state=active]:shadow-none"
          >
            好友请求
            {pendingRequestCount > 0 && (
              <span className="mono-num ml-2 text-[11px] text-accent">{pendingRequestCount}</span>
            )}
          </TabsTrigger>
          <TabsTrigger
            value="system"
            className="-mb-px h-auto flex-none items-center rounded-none border-0 border-b-2 border-transparent bg-transparent px-0 pb-3 pt-1 text-[13px] font-normal text-muted-foreground shadow-none data-[state=active]:border-foreground data-[state=active]:bg-transparent data-[state=active]:text-foreground data-[state=active]:shadow-none"
          >
            系统消息
            {unreadSystemCount > 0 && (
              <span className="mono-num ml-2 text-[11px] text-accent">{unreadSystemCount}</span>
            )}
          </TabsTrigger>
        </TabsList>

        {/* 好友消息 */}
        <TabsContent value="conversations" className="mt-0">
          <SectionHead
            eyebrow="会话 · CONVERSATIONS"
            title="最近联系人"
            count={`${conversations.length} 个 · ${unreadConversationCount} 条未读`}
          />
          <ScrollArea className="mt-4 h-[520px] border-y border-border">
            {conversations.length === 0 ? (
              <p className="px-1 py-10 text-[13px] text-muted-foreground">
                还没有任何会话。去好友名册找一位朋友开始聊天。
              </p>
            ) : (
              <ul className="divide-y divide-border">
                {conversations.map((conv) => (
                  <li key={conv.conversationId ?? conv.id ?? conv.senderId}>
                    <button
                      type="button"
                      onClick={() => handleOpenChat(conv.senderId)}
                      className="flex w-full items-center gap-4 px-1 py-4 text-left transition-colors hover:bg-muted/40"
                    >
                      <SquareAvatar name={conv.senderName} />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-baseline justify-between gap-3">
                          <span className="truncate font-serif text-[15px] leading-none">
                            {conv.senderName ?? '未知'}
                          </span>
                          {conv.sendTime && (
                            <span className="label-mono shrink-0 text-[9px]">
                              {formatMoment(conv.sendTime)}
                            </span>
                          )}
                        </div>
                        <div className="mt-1.5 flex items-baseline justify-between gap-3">
                          <p className="truncate text-[12.5px] leading-5 text-muted-foreground">
                            {conv.content || '暂无消息'}
                          </p>
                          {(conv.unreadCount ?? 0) > 0 && (
                            <span className="mono-num shrink-0 text-[11px] text-accent">
                              {conv.unreadCount}
                            </span>
                          )}
                        </div>
                      </div>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </ScrollArea>
        </TabsContent>

        {/* 好友请求 */}
        <TabsContent value="requests" className="mt-0">
          <SectionHead
            eyebrow="请求 · REQUESTS"
            title="好友请求"
            count={`${pendingRequestCount} 条待处理`}
          />
          <ScrollArea className="mt-4 h-[520px] border-y border-border">
            {friendRequests.length === 0 ? (
              <p className="px-1 py-10 text-[13px] text-muted-foreground">没有待处理的好友请求。</p>
            ) : (
              <ul className="divide-y divide-border">
                {friendRequests.map((request) => (
                  <li key={request.id} className="flex items-start gap-4 px-1 py-4">
                    <SquareAvatar name={request.senderName} />
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-serif text-[15px] leading-none">
                        {request.senderName}
                      </p>
                      <p className="mt-1.5 truncate text-[12.5px] leading-5 text-muted-foreground">
                        {request.applyMessage || '请求添加你为好友'}
                      </p>
                      <p className="label-mono mt-1.5 text-[9px]">
                        {new Date(request.sendTime).toLocaleString('zh-CN')}
                      </p>
                    </div>
                    <div className="flex shrink-0 gap-2">
                      <Button size="sm" onClick={() => onAcceptRequest(request.id, request.senderId)}>
                        接受
                      </Button>
                      <Button size="sm" variant="outline" onClick={() => onRejectRequest(request.id)}>
                        拒绝
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </ScrollArea>
        </TabsContent>

        {/* 系统消息 */}
        <TabsContent value="system" className="mt-0">
          <SectionHead
            eyebrow="系统消息 · SYSTEM"
            title="系统通知"
            count={`${systemMessages.length} 条 · ${unreadSystemCount} 条未读`}
          />
          <ScrollArea className="mt-4 h-[520px] border-y border-border">
            {systemMessages.length === 0 ? (
              <p className="px-1 py-10 text-[13px] text-muted-foreground">暂时没有系统消息。</p>
            ) : (
              <ul className="divide-y divide-border">
                {systemMessages.map((message) => (
                  <li
                    key={`${message.type}-${message.id}`}
                    className="flex items-start justify-between gap-4 px-1 py-4"
                  >
                    <div className="min-w-0 flex-1">
                      <p
                        className={`text-[13px] leading-6 ${
                          message.isRead ? 'text-muted-foreground' : 'text-foreground'
                        }`}
                      >
                        {message.content}
                      </p>
                      <p className="label-mono mt-1.5 text-[9px]">
                        {new Date(message.sendTime).toLocaleString('zh-CN')}
                      </p>
                    </div>
                    {!message.isRead && (
                      <div className="flex shrink-0 items-center gap-3">
                        <span className="label-mono text-[9px] text-accent">未读</span>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="h-7 px-2 text-[11px]"
                          onClick={() => onMarkAsRead(message)}
                        >
                          标记已读
                        </Button>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </ScrollArea>
        </TabsContent>
      </Tabs>
    </PageBody>
  );
}
