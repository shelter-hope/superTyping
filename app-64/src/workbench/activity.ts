import type { ActivityItem, WorkbenchUserCtx } from './types';
import { workbenchApps } from './registry';

/** 聚合各应用最近活动，按时间倒序截断。 */
export async function loadRecentActivity(ctx: WorkbenchUserCtx, limit = 12): Promise<ActivityItem[]> {
  const enabled = workbenchApps.filter((app) =>
    typeof app.enabled === 'function' ? app.enabled(ctx) : app.enabled
  );
  const batches = await Promise.all(
    enabled.map(async (app) => {
      if (!app.activityProvider) return [] as ActivityItem[];
      try {
        return await app.activityProvider(ctx);
      } catch {
        return [];
      }
    })
  );
  return batches
    .flat()
    .sort((a, b) => new Date(b.at).getTime() - new Date(a.at).getTime())
    .slice(0, limit);
}
