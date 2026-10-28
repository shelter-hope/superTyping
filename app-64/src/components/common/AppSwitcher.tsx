import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { enabledApps, findAppByPath } from '@/workbench/registry';

export default function AppSwitcher() {
  const { user, isAdmin } = useAuth();
  const location = useLocation();
  const apps = enabledApps({ user, isAdmin });
  const current = findAppByPath(location.pathname);

  return (
    <nav aria-label="应用切换" className="hidden items-center gap-3 md:flex">
      {apps.map((app) => {
        const active = current?.id === app.id;
        return (
          <Link
            key={app.id}
            to={app.homePath}
            className={`border-b-2 pb-0.5 text-[12.5px] tracking-[0.02em] transition-colors ${
              active
                ? 'border-accent text-foreground'
                : 'border-transparent text-muted-foreground hover:border-border hover:text-foreground'
            }`}
          >
            {app.title}
          </Link>
        );
      })}
    </nav>
  );
}
