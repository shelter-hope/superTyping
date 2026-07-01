/**
 * AI 聊天：会话与消息由 Java 后端 `/api/v1/ai/chat` 等接口提供（见 `postAiChat`）。
 */
import { useState, useEffect, useRef } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Loader2, Send, Bot, User, Trash2, Sparkles } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { postAiChat } from '@/db/api';
import { useToast } from '@/hooks/use-toast';

type ChatBubble = {
  id: string;
  role: 'user' | 'assistant';
  message: string;
  created_at: string;
};

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

  return (
    <Card className="flex flex-col h-[calc(100vh-12rem)] max-h-[800px] shadow-lg">
      <CardHeader className="border-b bg-gradient-to-r from-primary/5 to-primary/10">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="relative">
              <Bot className="w-8 h-8 text-primary" />
              <Sparkles className="w-3 h-3 text-primary absolute -top-1 -right-1" />
            </div>
            <div>
              <CardTitle className="text-xl">AI助手</CardTitle>
              <CardDescription className="text-sm">
                通过后端服务获取打字技巧与训练建议（需后端配置 LLM）
              </CardDescription>
            </div>
          </div>
          {messages.length > 0 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={handleClearHistory}
              disabled={loading}
              className="hover:bg-destructive/10 hover:text-destructive"
            >
              <Trash2 className="w-4 h-4 mr-2" />
              清空会话
            </Button>
          )}
        </div>
      </CardHeader>

      <CardContent className="flex-1 flex flex-col gap-4 p-0 min-h-0">
        <ScrollArea
          className="flex-1 px-4 py-4"
          ref={scrollRef}
          style={{
            scrollbarWidth: 'thin',
            scrollbarColor: 'hsl(var(--primary)) transparent',
          }}
        >
          <div className="space-y-4 pr-2">
            {messages.length === 0 && (
              <div className="text-center text-muted-foreground py-12">
                <div className="relative inline-block mb-6">
                  <Bot className="w-16 h-16 mx-auto opacity-20" />
                  <Sparkles className="w-6 h-6 text-primary absolute -top-2 -right-2 animate-pulse" />
                </div>
                <p className="text-lg font-medium mb-2">开始与AI助手对话</p>
                <p className="text-sm max-w-md mx-auto leading-relaxed">您可以询问：</p>
                <div className="mt-4 space-y-2 text-sm max-w-md mx-auto">
                  <div className="bg-muted/50 rounded-lg p-3 text-left">💡 如何提高打字速度？</div>
                  <div className="bg-muted/50 rounded-lg p-3 text-left">🎯 我的准确率总是很低，怎么办？</div>
                  <div className="bg-muted/50 rounded-lg p-3 text-left">📊 根据我的数据，给我一些训练建议</div>
                </div>
              </div>
            )}

            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex gap-3 animate-in fade-in slide-in-from-bottom-2 duration-300 ${
                  message.role === 'user' ? 'flex-row-reverse' : 'flex-row'
                }`}
              >
                <Avatar
                  className={`w-9 h-9 shrink-0 ${
                    message.role === 'user' ? 'bg-primary' : 'bg-gradient-to-br from-primary/80 to-primary'
                  }`}
                >
                  <AvatarFallback className="bg-transparent">
                    {message.role === 'user' ? (
                      <User className="w-5 h-5 text-primary-foreground" />
                    ) : (
                      <Bot className="w-5 h-5 text-primary-foreground" />
                    )}
                  </AvatarFallback>
                </Avatar>

                <div
                  className={`rounded-2xl px-4 py-3 max-w-[85%] shadow-sm ${
                    message.role === 'user'
                      ? 'bg-primary text-primary-foreground rounded-tr-sm'
                      : 'bg-muted rounded-tl-sm'
                  }`}
                >
                  <p className="text-sm whitespace-pre-wrap break-words leading-relaxed">{message.message}</p>
                  <p
                    className={`text-xs mt-2 ${message.role === 'user' ? 'opacity-80' : 'opacity-60'}`}
                  >
                    {new Date(message.created_at).toLocaleTimeString('zh-CN', {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </p>
                </div>
              </div>
            ))}

            {loading && (
              <div className="flex gap-3 animate-in fade-in slide-in-from-bottom-2 duration-300">
                <Avatar className="w-9 h-9 shrink-0 bg-gradient-to-br from-primary/80 to-primary">
                  <AvatarFallback className="bg-transparent">
                    <Bot className="w-5 h-5 text-primary-foreground" />
                  </AvatarFallback>
                </Avatar>
                <div className="rounded-2xl rounded-tl-sm px-4 py-3 bg-muted shadow-sm">
                  <div className="flex items-center gap-2">
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span className="text-sm text-muted-foreground">AI正在思考...</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </ScrollArea>

        <div className="border-t bg-muted/30 p-4">
          <div className="flex gap-3">
            <Textarea
              ref={textareaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="输入您的问题... (Shift+Enter换行，Enter发送)"
              className="min-h-[80px] max-h-[160px] resize-none bg-background shadow-sm"
              disabled={loading}
            />
            <Button
              onClick={() => void handleSend()}
              disabled={!input.trim() || loading}
              size="icon"
              className="shrink-0 h-[80px] w-[80px] rounded-xl shadow-md hover:shadow-lg transition-shadow"
            >
              {loading ? <Loader2 className="w-6 h-6 animate-spin" /> : <Send className="w-6 h-6" />}
            </Button>
          </div>

          <div className="flex items-center justify-between mt-3 text-xs text-muted-foreground">
            <span>💡 提示：按 Enter 发送，Shift+Enter 换行</span>
            <span className="opacity-70">AI助手可能会出错，请谨慎参考</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
