import type { ReactNode } from 'react';

interface PageHeaderProps {
  /** 小标签，例如「训练 · TRAINING」 */
  eyebrow: string;
  title: string;
  description?: string;
  /** 右侧操作区（按钮等） */
  actions?: ReactNode;
  /** 标题下方额外内容（筛选器、状态条） */
  children?: ReactNode;
}

/**
 * 页面标题区：报头式排版。
 * 一个页面只出现一次，放在 container 内的最上方。
 */
export function PageHeader({ eyebrow, title, description, actions, children }: PageHeaderProps) {
  return (
    <header className="border-b border-border pb-5">
      <div className="flex items-center gap-3">
        <span className="status-light mt-0.5" />
        <span className="label-mono text-muted-foreground">{eyebrow}</span>
      </div>
      <div className="mt-3 flex flex-wrap items-end justify-between gap-x-8 gap-y-4">
        <div className="min-w-0">
          <h1 className="font-serif text-[30px] leading-none md:text-[36px]">{title}</h1>
          {description ? (
            <p className="mt-3 max-w-2xl text-[13px] leading-6 text-muted-foreground">{description}</p>
          ) : null}
        </div>
        {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
      </div>
      {children ? <div className="mt-4">{children}</div> : null}
    </header>
  );
}

/** 页面主容器：统一留白与最大宽度 */
export function PageBody({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`container max-w-6xl py-10 ${className}`}>{children}</div>;
}
