import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
import { loadRecentActivity } from '@/workbench/activity';
import { enabledApps } from '@/workbench/registry';
import type { ActivityItem } from '@/workbench/types';

export default function LauncherPage() {
  const { user, isAdmin } = useAuth();
  const ctx = { user, isAdmin };
  const apps = enabledApps(ctx);
  const [activity, setActivity] = useState<ActivityItem[]>([]);

  useEffect(() => {
    let cancelled = false;
    loadRecentActivity(ctx).then((items) => {
      if (!cancelled) setActivity(items);
    });
    return () => {
      cancelled = true;
    };
  }, [user?.userId, isAdmin]);

  return (
    <PageBody>
      <PageHeader
        eyebrow="工作台 · WORKBENCH"
        title="实验台"
        description="选择一个应用进入。首页只做启动与最近活动，不承担单一产品的营销页。"
      />

      <section className="mt-10">
        <div className="flex items-baseline justify-between border-b border-border pb-2">
          <h2 className="font-serif text-lg">应用</h2>
          <span className="label-mono text-[9px]">{apps.length} apps</span>
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {apps.map((app) => (
            <Link
              key={app.id}
              to={app.homePath}
              className="group border border-border bg-card p-4 transition-colors hover:border-accent"
            >
              <div className="flex items-baseline justify-between gap-2">
                <h3 className="font-serif text-[17px] leading-none group-hover:text-accent">{app.title}</h3>
                <span className="label-mono text-[9px] text-muted-foreground">{app.id}</span>
              </div>
              <p className="mt-3 text-[12.5px] leading-5 text-muted-foreground">{app.blurb}</p>
            </Link>
          ))}
        </div>
      </section>

      <section className="mt-12">
        <div className="flex items-baseline justify-between border-b border-border pb-2">
          <h2 className="font-serif text-lg">最近活动</h2>
          <span className="label-mono text-[9px]">cross-app</span>
        </div>
        {activity.length === 0 ? (
          <p className="mt-6 text-[13px] text-muted-foreground">
            {user ? '暂无跨应用动态。去打一局或保存一次 Lab 实验吧。' : '登录后可聚合对局与实验快照。'}
          </p>
        ) : (
          <ul className="mt-2 divide-y divide-border border-t border-border">
            {activity.map((item) => (
              <li key={item.id}>
                <Link to={item.href} className="flex items-baseline justify-between gap-4 py-3 hover:text-accent">
                  <div className="min-w-0">
                    <p className="truncate text-[14px]">{item.title}</p>
                    {item.subtitle && (
                      <p className="mono-num mt-1 truncate text-[10px] text-muted-foreground">{item.subtitle}</p>
                    )}
                  </div>
                  <span className="label-mono shrink-0 text-[9px] text-muted-foreground">{item.appId}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </PageBody>
  );
}
