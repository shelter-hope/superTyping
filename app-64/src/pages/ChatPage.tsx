/**
 * 一对一聊天。连接打开时经 /ws/im 发送，关闭时回退 HTTP。
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import { ArrowLeft, FileText, Loader2, Paperclip, Send, Smile, Undo2 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { useImSocket } from '@/hooks/use-im-socket';
import { imSocket, type ImEvent, type ImStatus } from '@/services/imSocket';
import type { MessageDTO, Profile } from '@/types/database';
import {
  getProfile,
  getMessagesByFriend,
  sendMessage,
  markAsReadByFriend,
  uploadAttachment,
  downloadAttachment,
  recallMessage,
} from '@/db/api';
import { isSticker, stickerSrc, STICKERS } from '@/features/im/stickers';

const POLL_INTERVAL_MS = 3000;
const RECALL_WINDOW_MS = 2 * 60 * 1000;

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
  const next = [...current];
  for (const message of incoming) {
    const byClient = message.clientMsgId
      ? next.findIndex((item) => item.clientMsgId && item.clientMsgId === message.clientMsgId)
      : -1;
    if (byClient >= 0) {
      next[byClient] = { ...next[byClient], ...message };
      continue;
    }
    const byId = next.findIndex((item) => item.id === message.id);
    if (byId >= 0) next[byId] = { ...next[byId], ...message };
    else next.push(message);
  }
  return next.sort(byTime);
}

function canRecall(message: MessageDTO, userId: number) {
  if (message.senderId !== userId || message.recalled || message.id < 0) return false;
  return Date.now() - new Date(message.sendTime).getTime() < RECALL_WINDOW_MS;
}

function ChatImage({ attachmentId }: { attachmentId: string }) {
  const [url, setUrl] = useState('');
  useEffect(() => {
    let objectUrl = '';
    let cancelled = false;
    downloadAttachment(Number(attachmentId))
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => setUrl(''));
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [attachmentId]);
  if (!url) return <span className="text-sm">图片加载中</span>;
  return <img src={url} alt="聊天图片" className="max-h-48 max-w-full rounded-md" />;
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
  const [stickerOpen, setStickerOpen] = useState(false);
  const [friendTyping, setFriendTyping] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  const typingSent = useRef(false);
  const typingStop = useRef<number>();
  const typingHide = useRef<number>();
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
      } else if (event.type === 'ACK') {
        setMessages((prev) =>
          prev.map((m) =>
            m.clientMsgId === event.clientMsgId ? { ...m, id: event.id, serverSeq: event.serverSeq } : m
          )
        );
      } else if (event.type === 'READ' && event.readerId === fId) {
        setMessages((prev) =>
          prev.map((m) => (m.senderId === user.userId && !m.isRead ? { ...m, isRead: true } : m))
        );
      } else if (event.type === 'RECALL') {
        setMessages((prev) =>
          prev.map((m) =>
            m.clientMsgId === event.clientMsgId ? { ...m, recalled: true, content: '' } : m
          )
        );
      } else if (event.type === 'TYPING' && event.userId === fId) {
        setFriendTyping(event.typing);
        window.clearTimeout(typingHide.current);
        if (event.typing) {
          typingHide.current = window.setTimeout(() => setFriendTyping(false), 3000);
        }
      } else if (event.type === 'ERROR') {
        toast({ title: '发送失败', description: event.message, variant: 'destructive' });
        if (event.clientMsgId) {
          setMessages((prev) => prev.filter((m) => m.clientMsgId !== event.clientMsgId || m.id > 0));
        }
      }
    },
    [user, fId, toast]
  );

  const imStatus = useImSocket(handleImEvent, !!user);

  useEffect(() => {
    if (user && friendId) {
      loadData();
    }
  }, [user, friendId]);

  useEffect(() => {
    if (!user || !friendId) return;
    if (imStatus === 'open') {
      loadMessages();
      return;
    }
    const interval = setInterval(loadMessages, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [user, friendId, imStatus]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  useEffect(() => {
    if (textareaRef.current && !sending) {
      textareaRef.current.focus();
    }
  }, [sending]);

  async function loadData() {
    setLoading(true);
    try {
      await loadFriend();
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
      setMessages((prev) => mergeMessages(prev.filter((m) => m.id < 0), msgs));
    } catch (error) {
      console.error('Failed to load messages', error);
    }
  }

  function notifyTyping(value: string) {
    if (imStatus !== 'open' || !fId) return;
    if (!value) {
      if (typingSent.current) {
        imSocket.send({ action: 'typing', receiverId: fId, typing: false });
        typingSent.current = false;
      }
      window.clearTimeout(typingStop.current);
      return;
    }
    if (!typingSent.current) {
      imSocket.send({ action: 'typing', receiverId: fId, typing: true });
      typingSent.current = true;
    }
    window.clearTimeout(typingStop.current);
    typingStop.current = window.setTimeout(() => {
      imSocket.send({ action: 'typing', receiverId: fId, typing: false });
      typingSent.current = false;
    }, 2000);
  }

  async function deliver(content: string, type: string, extra: Partial<MessageDTO> = {}) {
    if (!user || !fId) return;
    const clientMsgId = crypto.randomUUID();
    const optimistic: MessageDTO = {
      id: -Date.now(),
      senderId: user.userId,
      senderName: user.username || '我',
      receiverId: fId,
      content,
      sendTime: new Date().toISOString(),
      isRead: false,
      type,
      clientMsgId,
      ...extra,
    };
    setMessages((prev) => mergeMessages(prev, [optimistic]));
    const sentOnSocket = imStatus === 'open' && imSocket.send({
      action: 'chat',
      receiverId: fId,
      content,
      type,
      clientMsgId,
    });
    if (sentOnSocket) return;
    try {
      const saved = await sendMessage(fId, content, type, clientMsgId);
      setMessages((prev) => mergeMessages(prev, [saved]));
    } catch (error) {
      setMessages((prev) => prev.filter((m) => m.clientMsgId !== clientMsgId));
      throw error;
    }
  }

  async function handleSend() {
    if (!input.trim() || sending || !fId) return;
    const content = input.trim();
    setInput('');
    notifyTyping('');
    setSending(true);
    try {
      await deliver(content, 'text');
    } catch (error) {
      console.error('发送消息失败:', error);
      toast({ title: '发送失败', description: '无法发送消息', variant: 'destructive' });
      setInput(content);
    } finally {
      setSending(false);
    }
  }

  async function handleSticker(id: string) {
    setStickerOpen(false);
    setSending(true);
    try {
      await deliver(id, 'emoji');
    } catch (error) {
      toast({
        title: '发送失败',
        description: error instanceof Error ? error.message : '无法发送表情',
        variant: 'destructive',
      });
    } finally {
      setSending(false);
    }
  }

  async function handleFile(file: File) {
    setSending(true);
    try {
      const uploaded = await uploadAttachment(file);
      const type = uploaded.kind === 5 ? 'image' : 'file';
      await deliver(String(uploaded.id), type, {
        attachmentName: uploaded.originalName,
        attachmentMediaType: uploaded.mediaType,
        attachmentSize: uploaded.sizeBytes,
      });
    } catch (error) {
      toast({
        title: '上传失败',
        description: error instanceof Error ? error.message : '无法发送文件',
        variant: 'destructive',
      });
    } finally {
      setSending(false);
    }
  }

  async function handleRecall(message: MessageDTO) {
    try {
      await recallMessage(message.id);
      setMessages((prev) =>
        prev.map((item) =>
          item.id === message.id || (message.clientMsgId && item.clientMsgId === message.clientMsgId)
            ? { ...item, recalled: true, content: '' }
            : item
        )
      );
    } catch (error) {
      toast({
        title: '撤回失败',
        description: error instanceof Error ? error.message : '无法撤回',
        variant: 'destructive',
      });
    }
  }

  async function saveFile(message: MessageDTO) {
    try {
      const blob = await downloadAttachment(Number(message.content));
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = message.attachmentName || 'file';
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      toast({ title: '下载失败', description: '无法下载文件', variant: 'destructive' });
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
        <CardHeader className="border-b">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate('/messages')}>
              <ArrowLeft className="w-5 h-5" />
            </Button>
            <Avatar>
              <AvatarFallback>{friend.username?.[0]?.toUpperCase() || '?'}</AvatarFallback>
            </Avatar>
            <div>
              <h3 className="font-semibold">{friend.username}</h3>
              <p className="text-sm text-muted-foreground">
                {friendTyping ? '正在输入…' : STATUS_TEXT[imStatus]}
              </p>
            </div>
          </div>
        </CardHeader>

        <CardContent className="flex-1 flex flex-col p-0 min-h-0">
          <ScrollArea className="flex-1 px-4 py-4" ref={scrollRef}>
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
                      key={message.clientMsgId || message.id}
                      className={`flex gap-3 ${isMe ? 'flex-row-reverse' : 'flex-row'}`}
                    >
                      <Avatar className="w-9 h-9 shrink-0">
                        <AvatarFallback className={isMe ? 'bg-primary' : 'bg-muted'}>
                          {isMe
                            ? (user?.username?.[0]?.toUpperCase() || '我')
                            : (friend.username?.[0]?.toUpperCase() || '?')}
                        </AvatarFallback>
                      </Avatar>
                      <div className={`max-w-[75%] ${isMe ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
                        <div
                          className={`rounded-2xl px-4 py-3 shadow-sm ${
                            isMe
                              ? 'bg-primary text-primary-foreground rounded-tr-sm'
                              : 'bg-muted rounded-tl-sm'
                          }`}
                        >
                          {message.recalled ? (
                            <p className="text-sm italic opacity-80">已撤回</p>
                          ) : message.type === 'emoji' && isSticker(message.content) ? (
                            <img src={stickerSrc(message.content)} alt={message.content} className="h-16 w-16" />
                          ) : message.type === 'image' ? (
                            <ChatImage attachmentId={message.content} />
                          ) : message.type === 'file' ? (
                            <button type="button" className="flex items-center gap-2 text-sm underline" onClick={() => saveFile(message)}>
                              <FileText className="w-4 h-4" />
                              <span>{message.attachmentName || '文件'}</span>
                            </button>
                          ) : (
                            <p className="text-sm whitespace-pre-wrap break-words leading-relaxed">{message.content}</p>
                          )}
                          <p className={`text-xs mt-2 ${isMe ? 'opacity-80' : 'opacity-60'}`}>
                            {new Date(message.sendTime).toLocaleTimeString('zh-CN', {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                            {isMe && !message.recalled && (
                              <span className="ml-2">{message.isRead ? '已读' : '未读'}</span>
                            )}
                          </p>
                        </div>
                        {user && canRecall(message, user.userId) && (
                          <Button variant="ghost" size="sm" className="h-7 px-2" onClick={() => handleRecall(message)}>
                            <Undo2 className="w-3 h-3 mr-1" />
                            撤回
                          </Button>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </ScrollArea>

          <div className="border-t bg-muted/30 p-4">
            {stickerOpen && (
              <div className="mb-3 flex gap-2">
                {STICKERS.map((sticker) => (
                  <button
                    key={sticker.id}
                    type="button"
                    title={sticker.label}
                    className="rounded-lg border bg-background p-1 hover:bg-muted"
                    onClick={() => handleSticker(sticker.id)}
                  >
                    <img src={stickerSrc(sticker.id)} alt={sticker.label} className="h-10 w-10" />
                  </button>
                ))}
              </div>
            )}
            <div className="flex gap-3">
              <div className="flex flex-col gap-2">
                <Button type="button" variant="outline" size="icon" onClick={() => setStickerOpen((open) => !open)} disabled={sending}>
                  <Smile className="w-4 h-4" />
                </Button>
                <Button type="button" variant="outline" size="icon" onClick={() => fileRef.current?.click()} disabled={sending}>
                  <Paperclip className="w-4 h-4" />
                </Button>
                <input
                  ref={fileRef}
                  type="file"
                  accept="image/jpeg,image/png,image/gif,image/webp,.pdf,.txt,.zip"
                  className="hidden"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    e.target.value = '';
                    if (file) handleFile(file);
                  }}
                />
              </div>
              <Textarea
                ref={textareaRef}
                value={input}
                onChange={(e) => {
                  setInput(e.target.value);
                  notifyTyping(e.target.value);
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
                className="shrink-0 h-[80px] w-[80px] rounded-xl"
              >
                {sending ? <Loader2 className="w-6 h-6 animate-spin" /> : <Send className="w-6 h-6" />}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
