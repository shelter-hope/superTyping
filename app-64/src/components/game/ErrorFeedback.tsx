// 错误反馈组件
import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { X, Check } from 'lucide-react';

interface ErrorFeedbackProps {
  lastError: {
    input: string;
    expected: string;
    timestamp: number;
  } | null;
  lastCorrect: {
    char: string;
    timestamp: number;
  } | null;
}

export default function ErrorFeedback({ lastError, lastCorrect }: ErrorFeedbackProps) {
  const [showError, setShowError] = useState(false);
  const [showCorrect, setShowCorrect] = useState(false);

  // 显示错误提示
  useEffect(() => {
    if (lastError) {
      setShowError(true);
      const timer = setTimeout(() => setShowError(false), 2000);
      return () => clearTimeout(timer);
    }
  }, [lastError]);

  // 显示正确提示
  useEffect(() => {
    if (lastCorrect) {
      setShowCorrect(true);
      const timer = setTimeout(() => setShowCorrect(false), 500);
      return () => clearTimeout(timer);
    }
  }, [lastCorrect]);

  return (
    <div className="fixed top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-50 pointer-events-none">
      {/* 错误提示 */}
      {showError && lastError && (
        <Card className="pointer-events-auto animate-shake border-destructive/50 bg-destructive/15 p-6">
          <div className="flex items-center gap-6">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-destructive/30">
              <X className="h-8 w-8 text-destructive" />
            </div>
            <div className="flex items-center gap-4">
              <div className="text-center">
                <div className="mb-1 text-sm text-muted-foreground">您输入了</div>
                <div className="font-mono text-4xl font-bold text-destructive">
                  {lastError.input === ' ' ? '空格' : lastError.input}
                </div>
              </div>
              <div className="text-3xl text-muted-foreground">→</div>
              <div className="text-center">
                <div className="mb-1 text-sm text-muted-foreground">应该是</div>
                <div className="font-mono text-4xl font-bold text-primary">
                  {lastError.expected === ' ' ? '空格' : lastError.expected}
                </div>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* 正确提示 */}
      {showCorrect && lastCorrect && (
        <div className="flex h-20 w-20 items-center justify-center rounded-full bg-primary/30 animate-ping-once">
          <Check className="h-12 w-12 text-primary" />
        </div>
      )}
    </div>
  );
}
