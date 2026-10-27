/**
 * AI 聊天：会话与消息由 Java 后端 `/api/v1/ai/chat` 等接口提供（见 `postAiChat`）。
 */
import { useState, useEffect, useRef } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { ScrollArea } from '@/components/ui/scroll-area';
import { useAuth } from '@/contexts/AuthContext';
import { postAiChat } from '@/db/api';
import { useToast } from '@/hooks/use-toast';

type ChatBubble = {
  id: string;
  role: 'user' | 'assistant';
  message: string;
  created_at: string;
};

const SUGGESTIONS = [
  '我最近错得最多的字符是哪些？',
  '按我的数据，这周该练什么？',
  '准确率一直在 70% 上下，怎么过 85%？',
];

export function AIChatInterface() {
  const { user } = useAuth();
  const { toast } = useToast();
  const [messages, setMessages] = useState<ChatBubble[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [conversationId, setConversationId] = useState<number | undefined>();
  const scrollRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  useEffect(() => {
    if (textareaRef.current && !loading) {
      textareaRef.current.focus();
    }
  }, [loading]);

  async function handleSend() {
    if (!input.trim() || loading || !user?.userId) return;

    const userMessage = input.trim();
    setInput('');
    setLoading(true);

    const tempId = crypto.randomUUID();
    const tempUserMessage: ChatBubble = {
      id: tempId,
      role: 'user',
      message: userMessage,
      created_at: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, tempUserMessage]);

    try {
      const res = await postAiChat(userMessage, conversationId);
      if (res.conversationId != null) setConversationId(res.conversationId);

      const aiMessage: ChatBubble = {
        id: crypto.randomUUID(),
        role: 'assistant',
        message: res.reply,
        created_at: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, aiMessage]);
    } catch (error) {
      console.error('发送消息失败:', error);
      toast({
        title: '发送失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
      setMessages((prev) => prev.filter((m) => m.id !== tempId));
    } finally {
      setLoading(false);
    }
  }

  /** 清空当前界面会话；后端若未提供删除会话接口，仅重新开始新 conversationId */
  function handleClearHistory() {
    setMessages([]);
    setConversationId(undefined);
    toast({
      title: '已清空',
      description: '可重新开始对话（将使用新的会话）',
    });
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void handleSend();
    }
  }

  function stamp(iso: string) {
    return new Date(iso).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }

  return (
    <Card className="flex h-[calc(100vh-16rem)] max-h-[680px] min-h-[420px] flex-col overflow-hidden">
      <CardHeader className="flex-row items-center justify-between gap-4 border-b border-border py-3">
        <div className="flex min-w-0 items-center gap-3">
          <span className="status-light" />
          <CardTitle className="font-serif text-lg leading-none">AI 助手</CardTitle>
          <CardDescription className="hidden truncate text-[11px] text-muted-foreground sm:block">
            通过后端配置的模型回答，能读到你的历史记录
          </CardDescription>
        </div>
        {messages.length > 0 ? (
          <Button
            variant="ghost"
            size="sm"
            onClick={handleClearHistory}
            disabled={loading}
            className="shrink-0 text-muted-foreground hover:text-destructive"
          >
            清空会话
          </Button>
        ) : null}
      </CardHeader>

      <CardContent className="flex min-h-0 flex-1 flex-col p-0">
        <ScrollArea className="flex-1 px-4 py-5" ref={scrollRef}>
          <div className="space-y-6">
            {messages.length === 0 ? (
              <div className="py-6">
                <p className="label-mono text-muted-foreground">可以这样问 · PROMPTS</p>
                <div className="mt-3 divide-y divide-border border-t border-border">
                  {SUGGESTIONS.map((q) => (
                    <button
                      key={q}
                      type="button"
                      onClick={() => setInput(q)}
                      className="block w-full py-3 text-left text-[13px] text-muted-foreground transition-colors hover:text-foreground"
                    >
                      {q}
                    </button>
                  ))}
                </div>
              </div>
            ) : null}

            {messages.map((message) =>
              message.role === 'user' ? (
                <div key={message.id} className="flex justify-end">
                  <div className="max-w-[85%] border border-border bg-muted px-4 py-3">
                    <p className="whitespace-pre-wrap break-words text-[13px] leading-6">{message.message}</p>
                    <p className="label-mono mt-2 text-muted-foreground">{stamp(message.created_at)}</p>
                  </div>
                </div>
              ) : (
                <div key={message.id} className="flex gap-3">
                  <span className="mt-[7px] shrink-0 text-[9px] leading-none text-accent">●</span>
                  <div className="max-w-[85%]">
                    <p className="whitespace-pre-wrap break-words text-[13px] leading-7">{message.message}</p>
                    <p className="label-mono mt-2 text-muted-foreground">{stamp(message.created_at)}</p>
                  </div>
                </div>
              ),
            )}

            {loading ? (
              <div className="flex items-center gap-3">
                <span className="text-[9px] leading-none text-accent">●</span>
                <span className="label-mono text-muted-foreground">思考中 · THINKING</span>
              </div>
            ) : null}
          </div>
        </ScrollArea>

        <div className="border-t border-border p-4">
          <div className="flex items-end gap-3">
            <Textarea
              ref={textareaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="问点什么…（Enter 发送，Shift+Enter 换行）"
              className="max-h-[160px] min-h-[64px] resize-none"
              disabled={loading}
            />
            <Button
              onClick={() => void handleSend()}
              disabled={!input.trim() || loading}
              className="h-10 shrink-0 px-5"
            >
              发送
            </Button>
          </div>
          <div className="mt-2 flex items-center justify-between gap-4">
            <span className="label-mono text-muted-foreground">Enter 发送 · Shift+Enter 换行</span>
            <span className="label-mono text-muted-foreground">结论仅供参考</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
