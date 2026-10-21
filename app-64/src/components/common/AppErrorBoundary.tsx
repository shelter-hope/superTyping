import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Button } from '@/components/ui/button';

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

export class AppErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('页面渲染失败', error, info.componentStack);
  }

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <div className="mx-auto flex min-h-screen max-w-lg flex-col items-start justify-center gap-4 px-6">
        <h1 className="text-2xl font-bold">页面出错了</h1>
        <p className="text-sm text-muted-foreground">{this.state.error.message}</p>
        <Button onClick={() => this.setState({ error: null })}>重试</Button>
      </div>
    );
  }
}
