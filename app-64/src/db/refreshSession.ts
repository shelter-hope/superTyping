export function shouldRefresh(status: number, url: string, alreadyRetried: boolean): boolean {
  if (status !== 401 || alreadyRetried) return false;
  return !['/auth/login', '/auth/signup', '/auth/refresh'].some((part) => url.includes(part));
}

export function createSingleFlight<T>(task: () => Promise<T>): () => Promise<T> {
  let pending: Promise<T> | null = null;
  return () => {
    if (!pending) {
      pending = task().finally(() => {
        pending = null;
      });
    }
    return pending;
  };
}
