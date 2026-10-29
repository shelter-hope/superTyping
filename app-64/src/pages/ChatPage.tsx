/**
 * 一对一聊天页面壳。会话编排在 features/im/useChatSession。
 *
 * 版式：纸与墨。顶部身份条 + 发丝线对话记录 + 底部固定写信栏。
 */

import { useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
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
  if (!url) return <span className="text-sm text-muted-foreground">图片加载中</span>;
  return <img src={url} alt="聊天图片" className="max-h-48 max-w-full rounded-[2px]" />;
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
      <PageBody>
        <PageHeader
          eyebrow="对话 · THREAD"
          title="单聊"
          description="正在接入会话与历史消息。"
        />
        <div className="mt-10 flex items-center gap-3 border-t border-border pt-6">
          <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
          <span className="label-mono text-[10px]">载入中 · LOADING</span>
        </div>
      </PageBody>
    );
  }

  if (!friend) {
    return (
      <PageBody>
        <PageHeader eyebrow="对话 · THREAD" title="单聊" description="未找到该用户。" />
        <div className="mt-10 border-t border-border pt-8">
          <p className="text-[13px] text-muted-foreground">这位用户可能已经不存在，或已被移除好友。</p>
          <Button className="mt-5" onClick={() => navigate('/apps/social/friends')}>
            返回好友列表
          </Button>
        </div>
      </PageBody>
    );
  }

  return (
    <PageBody className="flex h-[calc(100dvh-3.75rem)] min-h-[420px] flex-col">
      <PageHeader
        eyebrow="对话 · THREAD"
        title={friend.username}
        description={STATUS_TEXT[imStatus]}
        actions={
          <>
            <Button variant="outline" onClick={() => navigate('/apps/social/messages')}>
              <ArrowLeft className="h-4 w-4" />
              消息中心
            </Button>
            <Button variant="ghost" onClick={() => navigate('/apps/social/friends')}>
              好友列表
            </Button>
          </>
        }
      />

      <div className="mt-6 flex min-h-0 flex-1 flex-col border border-border bg-background">
        {/* 身份 / 连接状态条 */}
        <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-3">
          <div className="flex items-center gap-2">
            <span
              aria-hidden
              className={`h-1.5 w-1.5 rounded-full ${
                imStatus === 'open' ? 'bg-accent' : 'bg-muted-foreground/40'
              }`}
            />
            <span className="label-mono text-[10px]">对话记录 · TRANSCRIPT</span>
          </div>
          <div className="flex items-center gap-4">
            {friendTyping && (
              <span className="label-mono text-[9px] text-accent">正在输入…</span>
            )}
            <span className="mono-num text-[10px] text-muted-foreground">
              共 {messages.length} 条
            </span>
          </div>
        </div>

        {/* 消息区 */}
        <ScrollArea ref={scrollRef} className="min-h-0 flex-1">
          <div className="mx-auto w-full max-w-3xl px-5 py-6">
            {messages.length === 0 ? (
              <div className="border-y border-border py-10 text-center">
                <p className="label-mono text-[10px]">还没有消息 · NO MESSAGE</p>
                <p className="mt-2 text-[13px] text-muted-foreground">
                  发送第一条消息开始聊天吧。
                </p>
              </div>
            ) : (
              <div className="space-y-6">
                {messages.map((message) => {
                  const isMe = message.senderId === user!.userId;
                  return (
                    <div
                      key={message.clientMsgId || message.id}
                      className={`flex gap-3 ${isMe ? 'flex-row-reverse' : 'flex-row'}`}
                    >
                      <Avatar className="h-8 w-8 shrink-0">
                        <AvatarFallback
                          className={
                            isMe ? 'bg-foreground text-[11px] text-background' : 'bg-muted text-[11px]'
                          }
                        >
                          {isMe
                            ? (user?.username?.[0]?.toUpperCase() || '我')
                            : (friend.username?.[0]?.toUpperCase() || '?')}
                        </AvatarFallback>
                      </Avatar>
                      <div
                        className={`flex max-w-[75%] flex-col gap-1.5 ${
                          isMe ? 'items-end' : 'items-start'
                        }`}
                      >
                        <div
                          className={`rounded-[2px] px-4 py-3 ${
                            isMe ? 'bg-foreground text-background' : 'bg-muted text-foreground'
                          }`}
                        >
                          {message.recalled ? (
                            <p className="text-[13px] italic opacity-70">已撤回</p>
                          ) : message.type === 'emoji' && isSticker(message.content) ? (
                            <img
                              src={stickerSrc(message.content)}
                              alt={message.content}
                              className="h-16 w-16"
                            />
                          ) : message.type === 'image' ? (
                            <ChatImage attachmentId={message.content} />
                          ) : message.type === 'file' ? (
                            <button
                              type="button"
                              className="flex items-center gap-2 text-[13px] underline"
                              onClick={() => saveFile(message)}
                            >
                              <FileText className="h-4 w-4" />
                              <span>{message.attachmentName || '文件'}</span>
                            </button>
                          ) : (
                            <p className="whitespace-pre-wrap break-words text-[13.5px] leading-relaxed">
                              {message.content}
                            </p>
                          )}
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="label-mono text-[9px]">
                            {new Date(message.sendTime).toLocaleTimeString('zh-CN', {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </span>
                          {isMe && !message.recalled && (
                            <span
                              className={`label-mono text-[9px] ${
                                message.isRead ? 'text-muted-foreground' : 'text-accent'
                              }`}
                            >
                              {message.isRead ? '已读' : '未读'}
                            </span>
                          )}
                          {user && canRecall(message, user.userId) && (
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-6 px-1.5 text-[10px] text-muted-foreground"
                              onClick={() => handleRecall(message)}
                            >
                              <Undo2 className="mr-0.5 h-3 w-3" />
                              撤回
                            </Button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </ScrollArea>

        {/* 写信栏 */}
        <div className="border-t border-border bg-card p-4">
          <div className="mb-2 flex items-center justify-between gap-3">
            <span className="label-mono text-[10px]">输入 · MESSAGE</span>
            <div className="flex items-center gap-1">
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                aria-label="贴纸"
                title="贴纸"
                onClick={() => setStickerOpen((open) => !open)}
                disabled={sending}
              >
                <Smile className="h-4 w-4" />
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                aria-label="附件"
                title="附件"
                onClick={() => fileRef.current?.click()}
                disabled={sending}
              >
                <Paperclip className="h-4 w-4" />
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
          </div>

          {stickerOpen && (
            <div className="mb-3 flex gap-2 border-t border-border pt-3">
              {STICKERS.map((sticker) => (
                <button
                  key={sticker.id}
                  type="button"
                  title={sticker.label}
                  className="rounded-[2px] border border-border bg-background p-1 hover:bg-muted"
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

          <div className="flex items-end gap-3">
            <Textarea
              ref={textareaRef}
              value={input}
              onChange={(e) => {
                setInput(e.target.value);
                notifyTyping(e.target.value);
              }}
              onKeyDown={handleKeyDown}
              placeholder="输入消息... (Shift+Enter换行，Enter发送)"
              className="max-h-[140px] min-h-[64px] flex-1 resize-none rounded-none border-border bg-background text-[13px] shadow-none"
              disabled={sending}
            />
            <Button
              onClick={handleSend}
              disabled={!input.trim() || sending}
              className="h-10 w-24 shrink-0"
            >
              {sending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <>
                  <Send className="h-4 w-4" />
                  发送
                </>
              )}
            </Button>
          </div>
          <p className="label-mono mt-2 text-[9px]">ENTER 发送 · SHIFT+ENTER 换行</p>
        </div>
      </div>
    </PageBody>
  );
}
