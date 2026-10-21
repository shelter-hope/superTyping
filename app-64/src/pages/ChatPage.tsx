/**
 * 聊天界面页面
 * 
 * @description 与好友进行一对一聊天
 * 
 * @features
 * - 消息列表展示
 * - 发送消息
 * - 自动滚动到底部
 * - 标记消息为已读
 * - 实时更新（轮询）
 */

import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import { ArrowLeft, Send, Loader2 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import type { MessageDTO, Profile } from '@/types/database';
import { 
  getProfile, 
  getMessagesByFriend, 
  sendMessage, 
  markAsReadByFriend 
} from '@/db/api';
import { sendTyping } from '@/features/im/imClient';

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

  useEffect(() => {
    if (user && friendId) {
      loadData();
      // 轮询新消息
      const interval = setInterval(loadMessages, 3000);
      return () => clearInterval(interval);
    }
  }, [user, friendId]);

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
      const fId = friendId ? parseInt(friendId, 10) : 0;
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
    if (!user || !friendId) return;
    try {
      const fId = parseInt(friendId, 10);
      const msgs = await getMessagesByFriend(fId, 0);
      // 后端按 sendTime 倒序，前端按时间正序展示
      msgs.sort((a, b) => new Date(a.sendTime).getTime() - new Date(b.sendTime).getTime());
      setMessages(msgs);
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
      const clientMsgId = crypto.randomUUID();
      await sendMessage(parseInt(friendId), content, 'text', clientMsgId);
      
      // 重新加载消息
      await loadMessages();
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
              <p className="text-sm text-muted-foreground">好友</p>
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
                onChange={(e) => {
                  setInput(e.target.value);
                  if (friendId) sendTyping(parseInt(friendId, 10), e.target.value.length > 0);
                }}
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
