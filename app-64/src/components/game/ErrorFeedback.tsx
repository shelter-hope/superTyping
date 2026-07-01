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
        <Card className="p-6 bg-red-500/20 border-red-500 animate-shake pointer-events-auto">
          <div className="flex items-center gap-6">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-red-500/30">
              <X className="w-8 h-8 text-red-500" />
            </div>
            <div className="flex items-center gap-4">
              <div className="text-center">
                <div className="text-sm text-muted-foreground mb-1">您输入了</div>
                <div className="text-4xl font-mono font-bold text-red-500">
                  {lastError.input === ' ' ? '空格' : lastError.input}
                </div>
              </div>
              <div className="text-3xl text-muted-foreground">→</div>
              <div className="text-center">
                <div className="text-sm text-muted-foreground mb-1">应该是</div>
                <div className="text-4xl font-mono font-bold text-green-500">
                  {lastError.expected === ' ' ? '空格' : lastError.expected}
                </div>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* 正确提示 */}
      {showCorrect && lastCorrect && (
        <div className="flex items-center justify-center w-20 h-20 rounded-full bg-green-500/30 animate-ping-once">
          <Check className="w-12 h-12 text-green-500" />
        </div>
      )}
    </div>
  );
}
