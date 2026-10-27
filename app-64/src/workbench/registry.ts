import { adminApp } from './apps/admin';
import { assistantApp } from './apps/assistant';
import { labApp } from './apps/lab';
import { settingsApp } from './apps/settings';
import { socialApp } from './apps/social';
import { typingApp } from './apps/typing';
import type { WorkbenchAppManifest, WorkbenchUserCtx } from './types';

export const workbenchApps: WorkbenchAppManifest[] = [
  typingApp,
  labApp,
  socialApp,
  assistantApp,
  settingsApp,
  adminApp,
].sort((a, b) => a.order - b.order);

export function enabledApps(ctx: WorkbenchUserCtx): WorkbenchAppManifest[] {
  return workbenchApps.filter((app) => (typeof app.enabled === 'function' ? app.enabled(ctx) : app.enabled));
}

export function findAppByPath(pathname: string): WorkbenchAppManifest | undefined {
  return workbenchApps.find(
    (app) => pathname === app.basePath || pathname.startsWith(`${app.basePath}/`)
  );
}
