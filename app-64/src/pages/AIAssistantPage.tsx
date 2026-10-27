// AI助手页面
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { AIChatInterface } from '@/components/ai/AIChatInterface';
import { PageBody, PageHeader } from '@/components/common/PageHeader';

export default function AIAssistantPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!user) {
      navigate('/login');
    }
  }, [user, navigate]);

  if (!user) {
    return (
      <div className="container flex min-h-[calc(100vh-4rem)] items-center justify-center">
        <span className="label-mono text-muted-foreground">载入 · LOADING</span>
      </div>
    );
  }

  return (
    <PageBody className="max-w-4xl">
      <PageHeader
        eyebrow="助手 · ASSISTANT"
        title="问问这一局怎么打"
        description="模型读得到你的历史记录与错字分布，可以帮你判断该练哪类字符，而不是猜。"
      />
      <div className="mt-8">
        <AIChatInterface />
      </div>
    </PageBody>
  );
}
