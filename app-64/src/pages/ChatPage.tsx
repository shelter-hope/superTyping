/**
 * 聊天界面页面
 * 
 * @description 与好友进行一对一聊天
 * 
 * @features
 * - 消息列表展示
 * - 发送消息
 * - 自动滚动到底部
 * - 标记消息为已读，展示对方已读回执
 * - 实时更新（WebSocket 推送，断线时回退为轮询）
 */

import { useCallback, useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import { ArrowLeft, Send, Loader2 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { useImSocket } from '@/hooks/use-im-socket';
import type { ImEvent, ImStatus } from '@/services/imSocket';
import type { MessageDTO, Profile } from '@/types/database';
import { 
  getProfile, 
  getMessagesByFriend, 
  sendMessage, 
  markAsReadByFriend 
} from '@/db/api';

const POLL_INTERVAL_MS = 3000;

const STATUS_TEXT: Record<ImStatus, string> = {
  idle: '好友',
  connecting: '连接中…',
  open: '实时连接',
  closed: '连接断开，轮询中',
};

function byTime(a: MessageDTO, b: MessageDTO) {
  return new Date(a.sendTime).getTime() - new Date(b.sendTime).getTime();
}

function mergeMessages(current: MessageDTO[], incoming: MessageDTO[]): MessageDTO[] {
  const map = new Map<number, MessageDTO>();
  current.forEach((m) => map.set(m.id, m));
  incoming.forEach((m) => map.set(m.id, m));
  return Array.from(map.values()).sort(byTime);
}

export default function ChatPage() {
  const { friendId } = useParams<{ friendId: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [friend, setFriend] = useState<Profile | null>(null);
  const [messages, setMessages] = useState<MessageDTO[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fId = friendId ? parseInt(friendId, 10) : 0;

  const handleImEvent = useCallback(
    (event: ImEvent) => {
      if (!user || !fId) return;
      if (event.type === 'MESSAGE') {
        const m = event.message;
        const fromFriend = m.senderId === fId && m.receiverId === user.userId;
        const fromMe = m.senderId === user.userId && m.receiverId === fId;
        if (!fromFriend && !fromMe) return;
        setMessages((prev) => mergeMessages(prev, [m]));
        if (fromFriend) {
          markAsReadByFriend(fId).catch((e) => console.error('标记已读失败:', e));
        }
      } else if (event.type === 'READ' && event.readerId === fId) {
        setMessages((prev) =>
          prev.map((m) => (m.senderId === user.userId && !m.isRead ? { ...m, isRead: true } : m))
        );
      }
    },
    [user, fId]
  );

  const imStatus = useImSocket(handleImEvent, !!user);

  useEffect(() => {
    if (user && friendId) {
      loadData();
    }
  }, [user, friendId]);

  // 长连接可用时靠推送；不可用时回退为轮询。连上后补拉一次，弥补断线期间漏掉的消息
  useEffect(() => {
    if (!user || !friendId) return;
    if (imStatus === 'open') {
      loadMessages();
      return;
    }
    const interval = setInterval(loadMessages, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [user, friendId, imStatus]);

  // 自动滚动到底部
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  // 自动聚焦输入框
  useEffect(() => {
    if (textareaRef.current && !sending) {
      textareaRef.current.focus();
    }
  }, [sending]);

  async function loadData() {
    setLoading(true);
    try {
      await loadFriend();
      // 进入聊天时先把该会话内自己收到的未读全部标记为已读（不依赖分页，避免只标第一页导致未读不消失）
      if (fId) await markAsReadByFriend(fId);
      await loadMessages();
    } catch (error) {
      console.error('加载数据失败:', error);
    } finally {
      setLoading(false);
    }
  }

  async function loadFriend() {
    if (!friendId) return;
    try {
      const data = await getProfile(Number(friendId));
      setFriend(data);
    } catch (error) {
      console.error('Failed to load friend', error);
    }
  }

  async function loadMessages() {
    if (!user || !fId) return;
    try {
      const msgs = await getMessagesByFriend(fId, 0);
      // 后端按 sendTime 倒序，前端按时间正序展示
      setMessages(msgs.sort(byTime));
      // 已读状态由进入聊天时的 markAsReadByFriend 统一处理，此处不再按条标记
    } catch (error) {
      console.error('Failed to load messages', error);
    }
  }

  async function handleSend() {
    if (!input.trim() || sending || !friendId) return;

    const content = input.trim();
    setInput('');
    setSending(true);

    try {
      const sent = await sendMessage(fId, content);
      // 推送可能先于响应到达，按 id 合并去重
      setMessages((prev) => mergeMessages(prev, [sent]));
    } catch (error) {
      console.error('发送消息失败:', error);
      toast({
        title: '发送失败',
        description: '无法发送消息',
        variant: 'destructive',
      });
      setInput(content);
    } finally {
      setSending(false);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="w-8 h-8 animate-spin" />
      </div>
    );
  }

  if (!friend) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <p className="text-muted-foreground mb-4">未找到该用户</p>
          <Button onClick={() => navigate('/friends')}>返回好友列表</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="container max-w-4xl py-8">
      <Card className="flex flex-col h-[calc(100vh-8rem)]">
        {/* 头部 */}
        <CardHeader className="border-b">
          <div className="flex items-center gap-3">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate('/messages')}
            >
              <ArrowLeft className="w-5 h-5" />
            </Button>
            <Avatar>
              <AvatarFallback>
                {friend.username?.[0]?.toUpperCase() || '?'}
              </AvatarFallback>
            </Avatar>
            <div>
              <h3 className="font-semibold">{friend.username}</h3>
              <p className="text-sm text-muted-foreground">{STATUS_TEXT[imStatus]}</p>
            </div>
          </div>
        </CardHeader>

        {/* 消息列表 */}
        <CardContent className="flex-1 flex flex-col p-0 min-h-0">
          <ScrollArea
            className="flex-1 px-4 py-4"
            ref={scrollRef}
            style={{
              scrollbarWidth: 'thin',
              scrollbarColor: 'hsl(var(--primary)) transparent',
            }}
          >
            <div className="space-y-4 pr-2">
              {messages.length === 0 ? (
                <div className="text-center text-muted-foreground py-12">
                  <p>还没有消息</p>
                  <p className="text-sm mt-2">发送第一条消息开始聊天吧</p>
                </div>
              ) : (
                messages.map((message) => {
                  const isMe = message.senderId === user!.userId;
                  return (
                    <div
                      key={message.id}
                      className={`flex gap-3 animate-in fade-in slide-in-from-bottom-2 duration-300 ${
                        isMe ? 'flex-row-reverse' : 'flex-row'
                      }`}
                    >
                      <Avatar className="w-9 h-9 shrink-0">
                        <AvatarFallback className={isMe ? 'bg-primary' : 'bg-muted'}>
                          {isMe
                            ? (user?.username?.[0]?.toUpperCase() || '我')
                            : (friend.username?.[0]?.toUpperCase() || '?')}
                        </AvatarFallback>
                      </Avatar>

                      <div
                        className={`rounded-2xl px-4 py-3 max-w-[75%] shadow-sm ${
                          isMe
                            ? 'bg-primary text-primary-foreground rounded-tr-sm'
                            : 'bg-muted rounded-tl-sm'
                        }`}
                      >
                        <p className="text-sm whitespace-pre-wrap break-words leading-relaxed">
                          {message.content}
                        </p>
                        <p
                          className={`text-xs mt-2 ${
                            isMe ? 'opacity-80' : 'opacity-60'
                          }`}
                        >
                          {new Date(message.sendTime).toLocaleTimeString('zh-CN', {
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                          {isMe && <span className="ml-2">{message.isRead ? '已读' : '未读'}</span>}
                        </p>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </ScrollArea>

          {/* 输入区域 */}
          <div className="border-t bg-muted/30 p-4">
            <div className="flex gap-3">
              <Textarea
                ref={textareaRef}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="输入消息... (Shift+Enter换行，Enter发送)"
                className="min-h-[80px] max-h-[160px] resize-none bg-background shadow-sm"
                disabled={sending}
              />
              <Button
                onClick={handleSend}
                disabled={!input.trim() || sending}
                size="icon"
                className="shrink-0 h-[80px] w-[80px] rounded-xl shadow-md hover:shadow-lg transition-shadow"
              >
                {sending ? (
                  <Loader2 className="w-6 h-6 animate-spin" />
                ) : (
                  <Send className="w-6 h-6" />
                )}
              </Button>
            </div>

            {/* 提示信息 */}
            <div className="flex items-center justify-between mt-3 text-xs text-muted-foreground">
              <span>💡 提示：按 Enter 发送，Shift+Enter 换行</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
