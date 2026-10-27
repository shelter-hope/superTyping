// 错误反馈：一张批在校样上的红字条
import { useEffect, useState } from 'react';

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

  useEffect(() => {
    if (lastError) {
      setShowError(true);
      const timer = setTimeout(() => setShowError(false), 2000);
      return () => clearTimeout(timer);
    }
  }, [lastError]);

  useEffect(() => {
    if (lastCorrect) {
      setShowCorrect(true);
      const timer = setTimeout(() => setShowCorrect(false), 500);
      return () => clearTimeout(timer);
    }
  }, [lastCorrect]);

  const display = (value: string) => (value === ' ' ? '空格' : value);

  return (
    <div className="pointer-events-none fixed inset-x-0 top-24 z-50 flex flex-col items-center gap-3">
      {showError && lastError && (
        <div className="animate-shake pointer-events-auto bg-foreground px-5 py-3 text-background">
          <div className="label-mono text-background/55">校样 · 批注</div>
          <div className="mt-1 flex items-baseline gap-3">
            <span className="font-mono text-2xl leading-none text-accent line-through decoration-2">{display(lastError.input)}</span>
            <span className="label-mono text-background/45">应为</span>
            <span className="font-mono text-2xl leading-none">{display(lastError.expected)}</span>
          </div>
        </div>
      )}

      {showCorrect && lastCorrect && (
        <div className="animate-ping-once border border-accent/70 bg-background/80 px-3 py-1">
          <span className="label-mono text-accent">排字无误</span>
        </div>
      )}
    </div>
  );
}
