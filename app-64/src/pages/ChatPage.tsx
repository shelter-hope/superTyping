/**
 * 一对一聊天页面壳。会话编排在 features/im/useChatSession。
 */

import { useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import { ArrowLeft, FileText, Loader2, Paperclip, Send, Smile, Undo2 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useState } from 'react';
import { downloadAttachmentAsFile, useAttachmentObjectUrl } from '@/features/im/attachmentBlob';
import { canRecall } from '@/features/im/messageMerge';
import { useChatSession } from '@/features/im/useChatSession';
import { isSticker, stickerSrc, STICKERS } from '@/features/im/stickers';

const STATUS_TEXT = {
  idle: '好友',
  connecting: '连接中…',
  open: '实时连接',
  closed: '连接断开，轮询中',
} as const;

function ChatImage({ attachmentId }: { attachmentId: string }) {
  const url = useAttachmentObjectUrl(attachmentId);
  if (!url) return <span className="text-sm">图片加载中</span>;
  return <img src={url} alt="聊天图片" className="max-h-48 max-w-full rounded-md" />;
}

export default function ChatPage() {
  const { friendId } = useParams<{ friendId: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const fId = friendId ? parseInt(friendId, 10) : 0;
  const {
    friend,
    messages,
    input,
    setInput,
    loading,
    sending,
    friendTyping,
    imStatus,
    notifyTyping,
    handleSend,
    handleSticker,
    handleFile,
    handleRecall,
    toast,
  } = useChatSession(user, fId);

  const [stickerOpen, setStickerOpen] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);

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

  async function saveFile(message: { content: string; attachmentName?: string }) {
    try {
      await downloadAttachmentAsFile(message.content, message.attachmentName || 'file');
    } catch {
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
                    onClick={() => {
                      setStickerOpen(false);
                      handleSticker(sticker.id);
                    }}
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
