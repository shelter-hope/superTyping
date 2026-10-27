import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/button';
import {
  deleteLabRun,
  fetchLabRun,
  fetchLabRuns,
  forkLabRunPreset,
  type LabRunDto,
} from '@/services/lab';

export default function LabHistoryPage() {
  const navigate = useNavigate();
  const [runs, setRuns] = useState<LabRunDto[]>([]);
  const [selected, setSelected] = useState<LabRunDto | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchLabRuns({ page: 0, size: 30 })
      .then((page) => {
        if (!cancelled) {
          setRuns(page.content || []);
          setError('');
        }
      })
      .catch(() => {
        if (!cancelled) setError('无法加载实验历史，请先登录');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function openRun(id: number) {
    try {
      const detail = await fetchLabRun(id);
      setSelected(detail);
    } catch {
      setError('加载详情失败');
    }
  }

  async function remove(id: number) {
    try {
      await deleteLabRun(id);
      setRuns((items) => items.filter((item) => item.id !== id));
      if (selected?.id === id) setSelected(null);
    } catch {
      setError('删除失败');
    }
  }

  async function fork(id: number) {
    try {
      const preset = await forkLabRunPreset(id);
      setError('');
      navigate(`/apps/lab/${preset.algorithmId}?preset=${preset.id}`);
    } catch {
      setError('生成预设失败');
    }
  }

  function rerun(run: LabRunDto) {
    const params = encodeURIComponent(JSON.stringify(run.params));
    navigate(`/apps/lab/${run.algorithmId}?seed=${run.seed}&params=${params}`);
  }

  return (
    <PageBody>
      <PageHeader
        eyebrow="实验室 · HISTORY"
        title="实验历史"
        description="客户端快照入库：参数、种子与指标曲线。可再用同一组参数开跑，或派生预设。"
      >
        <Link to="/apps/lab" className="label-mono text-[10px] text-muted-foreground hover:text-accent">
          返回实验台
        </Link>
      </PageHeader>

      {error && <p className="mt-6 text-[13px] text-accent">{error}</p>}
      {loading ? (
        <p className="mt-12 text-center text-[13px] text-muted-foreground">载入中…</p>
      ) : runs.length === 0 ? (
        <p className="mt-12 border-y border-border py-14 text-center text-[13px] text-muted-foreground">
          还没有保存过实验。在实验台点「保存本次实验」。
        </p>
      ) : (
        <div className="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
          <section className="divide-y divide-border border border-border">
            {runs.map((run) => (
              <button
                key={run.id}
                type="button"
                onClick={() => void openRun(run.id)}
                className={`flex w-full items-start justify-between gap-4 px-4 py-3 text-left hover:bg-muted/40 ${
                  selected?.id === run.id ? 'bg-muted/50' : ''
                }`}
              >
                <div className="min-w-0">
                  <p className="truncate font-serif text-[15px]">{run.title || `#${run.id}`}</p>
                  <p className="mono-num mt-1 text-[10px] text-muted-foreground">
                    {run.algorithmId} · seed {run.seed}
                    {run.schemaStale ? ' · schema stale' : ''}
                    {run.createdAt ? ` · ${run.createdAt.replace('T', ' ')}` : ''}
                  </p>
                </div>
                <span className="label-mono shrink-0 text-[9px]">{run.status}</span>
              </button>
            ))}
          </section>

          <aside className="border border-border bg-card p-4">
            {!selected ? (
              <p className="text-[12px] text-muted-foreground">选择一条记录查看曲线摘要。</p>
            ) : (
              <div className="space-y-4">
                <div>
                  <h2 className="font-serif text-[17px]">{selected.title || `实验 #${selected.id}`}</h2>
                  <p className="mono-num mt-1 text-[10px] text-muted-foreground">
                    {selected.algorithmId} · seed {selected.seed} · {selected.durationMs ?? 0} ms
                  </p>
                </div>
                <div>
                  <p className="label-mono text-[9px]">summary</p>
                  <pre className="mt-2 overflow-auto bg-muted/40 p-2 text-[11px] leading-5">
                    {JSON.stringify(selected.summary || {}, null, 2)}
                  </pre>
                </div>
                {selected.metrics && (
                  <div>
                    <p className="label-mono text-[9px]">metrics series keys</p>
                    <p className="mono-num mt-2 text-[11px] text-muted-foreground">
                      {Object.keys((selected.metrics.series as Record<string, unknown>) || {}).join(' · ') || '—'}
                    </p>
                    <MetricSpark metrics={selected.metrics} />
                  </div>
                )}
                <div className="flex flex-wrap gap-2">
                  <Button size="sm" onClick={() => rerun(selected)}>用此参数再跑</Button>
                  <Button size="sm" variant="outline" onClick={() => void fork(selected.id)}>派生预设</Button>
                  <Button size="sm" variant="ghost" onClick={() => void remove(selected.id)}>删除</Button>
                </div>
              </div>
            )}
          </aside>
        </div>
      )}
    </PageBody>
  );
}

function MetricSpark({ metrics }: { metrics: Record<string, unknown> }) {
  const series = (metrics.series || {}) as Record<string, number[]>;
  const firstKey = Object.keys(series)[0];
  const values = firstKey ? series[firstKey] || [] : [];
  if (values.length < 2) return null;
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = Math.max(1e-6, max - min);
  const width = 320;
  const height = 64;
  const points = values
    .map((value, index) => {
      const x = (index / (values.length - 1)) * width;
      const y = height - ((value - min) / span) * (height - 4) - 2;
      return `${x},${y}`;
    })
    .join(' ');
  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="mt-3 w-full border border-border bg-background" role="img">
      <polyline fill="none" stroke="currentColor" strokeWidth="1.5" points={points} className="text-accent" />
    </svg>
  );
}
