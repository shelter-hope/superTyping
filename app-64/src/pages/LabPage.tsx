import { useEffect, useMemo, useRef, useState, type PointerEvent } from 'react';
import { Link, Navigate, useParams, useSearchParams } from 'react-router-dom';
import { Application, Graphics } from 'pixi.js';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
import { deleteLabPreset, fetchLabAlgorithms, fetchLabPresets, fetchMyLabPresets, saveLabPreset, type LabPresetDto } from '@/features/lab/api';
import { labRegistry, visibleLabPlugins } from '@/features/lab/registry';
import type { LabPlugin, ParamField, PointerForce } from '@/features/lab/plugin';

const WIDTH = 800;
const HEIGHT = 480;
let bootQueue: Promise<void> = Promise.resolve();

export default function LabPage() {
  const { algorithmId = 'boids' } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const [enabledIds, setEnabledIds] = useState<string[] | null>(null);
  const visiblePlugins = visibleLabPlugins(labRegistry, enabledIds);
  const algorithm = visiblePlugins.find((item) => item.id === algorithmId) ?? visiblePlugins[0] ?? labRegistry[0];
  const [boundId, setBoundId] = useState(algorithm.id);
  const [params, setParams] = useState<Record<string, number>>(algorithm.defaults);
  if (boundId !== algorithm.id) {
    setBoundId(algorithm.id);
    setParams({ ...algorithm.defaults });
  }
  const [seed, setSeed] = useState(7);
  const [paused, setPaused] = useState(false);
  const [presetName, setPresetName] = useState('我的预设');
  const [sharePublic, setSharePublic] = useState(true);
  const [notice, setNotice] = useState('');
  const [presets, setPresets] = useState<LabPresetDto[]>([]);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const metricsNode = useRef<HTMLParagraphElement>(null);
  const presetQuery = searchParams.get('preset');
  const stateRef = useRef({
    plugin: algorithm as LabPlugin<Record<string, number>, unknown>,
    params,
    paused,
    seed,
    valid: true,
    bound: algorithm.id,
    sim: algorithm.create(algorithm.defaults, 7, WIDTH, HEIGHT) as unknown,
  });
  const parsed = useMemo(() => algorithm.schema.safeParse(params), [algorithm, params]);
  stateRef.current.plugin = algorithm as LabPlugin<Record<string, number>, unknown>;
  stateRef.current.params = boundId === algorithm.id ? params : { ...algorithm.defaults };
  stateRef.current.paused = paused;
  stateRef.current.seed = seed;
  stateRef.current.valid = boundId === algorithm.id ? parsed.success : true;
  if (stateRef.current.bound !== algorithm.id) {
    stateRef.current.bound = algorithm.id;
    stateRef.current.sim = algorithm.create(stateRef.current.params, stateRef.current.seed, WIDTH, HEIGHT);
  }

  useEffect(() => {
    let cancelled = false;
    fetchLabAlgorithms()
      .then((rows) => {
        if (!cancelled) setEnabledIds(rows.filter((row) => row.enabled).map((row) => row.id));
      })
      .catch(() => {
        if (!cancelled) setEnabledIds(null);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    Promise.allSettled([fetchLabPresets(algorithm.id), fetchMyLabPresets(algorithm.id)]).then((results) => {
      if (cancelled) return;
      const visible = results[0].status === 'fulfilled' ? results[0].value : [];
      const mine = results[1].status === 'fulfilled' ? results[1].value : loadLocalPresets(algorithm.id);
      const merged = new Map<number, LabPresetDto>();
      [...visible, ...mine].forEach((preset) => merged.set(preset.id, preset));
      setPresets([...merged.values()]);
      const match = merged.get(Number(presetQuery));
      if (match) applyPreset(match.params);
    });
    return () => {
      cancelled = true;
    };
  }, [algorithm.id, presetQuery]);

  useEffect(() => {
    const plugin = stateRef.current.plugin;
    stateRef.current.sim = plugin.create(stateRef.current.params, seed, WIDTH, HEIGHT);
  }, [algorithm.id, seed, params.count, params.ants, params.population]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let stopped = false;
    let app: Application | null = null;
    let frameId = 0;
    const graphics = new Graphics();
    const boot = bootQueue.then(async () => {
      if (stopped) return;
      const view = new Application();
      await view.init({ canvas, width: WIDTH, height: HEIGHT, background: 0x071612, antialias: false, preference: 'webgl' });
      view.ticker.stop();
      if (stopped) {
        view.destroy({ removeView: false });
        return;
      }
      app = view;
      view.stage.addChild(graphics);
      let last = performance.now();
      let reported = 0;
      const frame = (now: number) => {
        if (stopped) return;
        const step = Math.min(32, now - last);
        last = now;
        const current = stateRef.current;
        let nextMetrics: Record<string, number> | null = null;
        if (!current.paused && step > 0 && current.valid) {
          nextMetrics = current.plugin.step(current.sim, current.params, WIDTH, HEIGHT);
        }
        current.plugin.draw(graphics, current.sim, current.params);
        view.render();
        if (nextMetrics && now - reported > 250 && metricsNode.current) {
          reported = now;
          metricsNode.current.textContent = formatMetrics(current.plugin, current.seed, nextMetrics);
        }
        frameId = requestAnimationFrame(frame);
      };
      frameId = requestAnimationFrame(frame);
    }).catch(() => undefined);
    bootQueue = boot;
    return () => {
      stopped = true;
      cancelAnimationFrame(frameId);
      if (app) {
        app.ticker.stop();
        app.destroy({ removeView: false });
      }
    };
  }, []);

  function applyPreset(next: Record<string, number>) {
    const checked = algorithm.schema.safeParse(next);
    if (!checked.success) {
      setNotice('预设参数超出范围');
      return;
    }
    setParams(checked.data);
    stateRef.current.params = checked.data;
  }

  function reset(nextSeed = seed) {
    setSeed(nextSeed);
    stateRef.current.seed = nextSeed;
    stateRef.current.sim = stateRef.current.plugin.create(stateRef.current.params, nextSeed, WIDTH, HEIGHT);
    if (metricsNode.current) metricsNode.current.textContent = formatMetrics(stateRef.current.plugin, nextSeed, {});
  }

  function pointAt(event: PointerEvent<HTMLCanvasElement>): PointerForce {
    const point = canvasPoint(event.currentTarget, event.clientX, event.clientY);
    return { ...point, mode: event.button === 2 ? 'repel' : 'attract' };
  }

  async function savePreset() {
    if (!parsed.success) {
      setNotice('参数超出范围，不能保存');
      return;
    }
    try {
      const saved = await saveLabPreset({
        algorithmId: algorithm.id,
        name: presetName || '未命名',
        params,
        isPublic: sharePublic,
      });
      setPresets((items) => [...items.filter((item) => item.id !== saved.id), saved]);
      setSearchParams({ preset: String(saved.id) });
      setNotice(sharePublic ? `已公开分享，链接带 preset=${saved.id}` : '已保存为私人预设');
    } catch {
      const local = saveLocalPreset(algorithm.id, presetName || '未命名', params);
      setPresets((items) => [...items, local]);
      setNotice('服务未登录或不可用，已保存到本机');
    }
  }

  async function removePreset(preset: LabPresetDto) {
    if (preset.id < 0) dropLocalPreset(algorithm.id, preset.id);
    else {
      try {
        await deleteLabPreset(preset.id);
      } catch {
        setNotice('删除失败，请确认这是你的预设');
        return;
      }
    }
    setPresets((items) => items.filter((item) => item.id !== preset.id));
  }

  if (enabledIds && visiblePlugins.length === 0) {
    return (
      <PageBody>
        <PageHeader
          eyebrow="实验室 · LAB"
          title="群体智能实验室"
          description="在浏览器里观察鸟群、粒子群、蚁群与遗传算法的演化。"
        />
        <p className="mt-12 border-y border-border py-14 text-center text-[13px] text-muted-foreground">
          实验室算法都已停用。
        </p>
      </PageBody>
    );
  }
  if (algorithm.id !== algorithmId) {
    return <Navigate to={`/lab/${algorithm.id}`} replace />;
  }

  return (
    <PageBody>
      <PageHeader
        eyebrow="实验室 · LAB"
        title="群体智能实验室"
        description="在浏览器里观察鸟群、粒子群、蚁群与遗传算法的演化。拖动参数、保存预设，再回到同一组数字。"
      >
        <nav aria-label="实验切换" className="flex flex-wrap items-center gap-x-6 gap-y-2">
          <span className="label-mono mr-1 text-[9px]">实验 · experiments</span>
          {visiblePlugins.map((item, index) => {
            const active = item.id === algorithm.id;
            return (
              <Link
                key={item.id}
                to={`/lab/${item.id}`}
                aria-current={active ? 'page' : undefined}
                className={`flex items-baseline gap-2 border-b-2 pb-1 pt-1 text-[13px] tracking-[0.02em] transition-colors ${
                  active
                    ? 'border-accent text-foreground'
                    : 'border-transparent text-muted-foreground hover:border-border hover:text-foreground'
                }`}
              >
                <span className="mono-num text-[9px] text-accent">{String(index + 1).padStart(2, '0')}</span>
                {item.name}
              </Link>
            );
          })}
        </nav>
      </PageHeader>

      <div className="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
        <section className="border border-border bg-card">
          <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border px-4 py-3">
            <div>
              <div className="flex items-center gap-2">
                <span className="status-light" />
                <h2 className="font-serif text-[17px] leading-none">{algorithm.name}</h2>
              </div>
              <p className="mt-2 max-w-xl text-[11.5px] leading-5 text-muted-foreground">{algorithm.description}</p>
            </div>
            <span className="label-mono text-[9px]">实验台 · stage</span>
          </div>

          <canvas
            ref={canvasRef}
            width={WIDTH}
            height={HEIGHT}
            className="block h-auto w-full border-b border-border"
            onContextMenu={(event) => event.preventDefault()}
            onPointerDown={(event) => stateRef.current.plugin.onPointer?.(stateRef.current.sim, pointAt(event))}
            onPointerMove={(event) => {
              if (event.buttons === 0) return;
              stateRef.current.plugin.onPointer?.(stateRef.current.sim, pointAt(event));
            }}
            onPointerUp={() => stateRef.current.plugin.onPointer?.(stateRef.current.sim, null)}
            onPointerLeave={() => stateRef.current.plugin.onPointer?.(stateRef.current.sim, null)}
          />

          <div className="flex flex-wrap items-center gap-2 border-b border-border px-4 py-3">
            <Button size="sm" onClick={() => setPaused((value) => !value)}>{paused ? '继续' : '暂停'}</Button>
            <Button size="sm" variant="outline" onClick={() => {
              stateRef.current.paused = true;
              setPaused(true);
              const nextMetrics = stateRef.current.plugin.step(stateRef.current.sim, params, WIDTH, HEIGHT);
              if (metricsNode.current) metricsNode.current.textContent = formatMetrics(algorithm as LabPlugin<Record<string, number>, unknown>, seed, nextMetrics);
            }}>单步</Button>
            <Button size="sm" variant="outline" onClick={() => reset()}>重置</Button>
            <Button size="sm" variant="outline" onClick={() => reset(Math.floor(Math.random() * 10000))}>随机种子</Button>
            <Button size="sm" variant="outline" onClick={() => canvasRef.current?.requestFullscreen()}>全屏</Button>
          </div>

          <div className="px-4 py-3">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <span className="label-mono text-[9px]">实时指标 · metrics</span>
              <span className="mono-num text-[9px] text-muted-foreground">live · {WIDTH}×{HEIGHT}</span>
            </div>
            <p ref={metricsNode} className="mono-num mt-2 text-[12px] leading-6">
              {formatMetrics(algorithm as LabPlugin<Record<string, number>, unknown>, seed, {})}
            </p>
            {algorithm.pointerHint && <p className="mt-2 text-[11.5px] leading-5 text-muted-foreground">{algorithm.pointerHint}</p>}
            {!parsed.success && <p className="mt-2 border-t border-border pt-2 text-[11.5px] text-accent">参数超出范围</p>}
            {notice && <p className="mt-2 border-t border-border pt-2 text-[11.5px] text-accent">{notice}</p>}
          </div>
        </section>

        <aside className="border border-border bg-card">
          <div className="flex items-baseline justify-between border-b border-border px-4 py-3">
            <h2 className="font-serif text-[15px] leading-none">参数</h2>
            <span className="label-mono text-[9px]">{algorithm.fields.length} params</span>
          </div>
          <div className="divide-y divide-border">
            {algorithm.fields.map((field) => (
              <label key={field.key} className="block px-4 py-3">
                <span className="flex items-baseline justify-between gap-3">
                  <span className="label-mono text-[9px]">{field.label}</span>
                  <span className="mono-num text-[12px] text-foreground">{params[field.key] ?? field.min}</span>
                </span>
                <input
                  className="mt-3 w-full accent-accent"
                  type="range"
                  min={field.min}
                  max={field.max}
                  step={field.step}
                  value={params[field.key] ?? field.min}
                  onChange={(event) => {
                    const value = snap(field, Number(event.target.value));
                    setParams((current) => ({ ...current, [field.key]: value }));
                  }}
                />
                <span className="mt-1 flex items-baseline justify-between">
                  <span className="mono-num text-[9px] text-muted-foreground">{field.min}</span>
                  <span className="mono-num text-[9px] text-muted-foreground">{field.max}</span>
                </span>
              </label>
            ))}
          </div>

          <div className="border-t border-border px-4 py-4">
            <div className="flex items-baseline justify-between">
              <span className="label-mono text-[9px]">预设 · presets</span>
              <span className="mono-num text-[10px] text-muted-foreground">{presets.length}</span>
            </div>
            <Input value={presetName} onChange={(event) => setPresetName(event.target.value)} placeholder="预设名称" className="mt-3 h-9" />
            <label className="mt-3 flex items-center gap-2 text-[12px] text-muted-foreground">
              <input
                type="checkbox"
                checked={sharePublic}
                onChange={(event) => setSharePublic(event.target.checked)}
                className="accent-accent"
              />
              公开分享
            </label>
            <Button size="sm" className="mt-3 w-full" onClick={() => void savePreset()}>保存预设</Button>
            <div className="mt-4 divide-y divide-border border-t border-border">
              {presets.length === 0 ? (
                <p className="py-3 text-[11.5px] text-muted-foreground">还没有保存过预设。</p>
              ) : (
                presets.map((preset) => (
                  <div key={preset.id} className="flex items-center gap-2 py-2">
                    <button
                      type="button"
                      className="min-w-0 flex-1 text-left transition-colors hover:text-accent"
                      onClick={() => {
                        applyPreset(preset.params);
                        setSearchParams({ preset: String(preset.id) });
                      }}
                    >
                      <span className="block truncate font-serif text-[13px] leading-5">{preset.name}</span>
                      <span className="mono-num mt-0.5 block text-[9px] text-muted-foreground">
                        {preset.isPublic ? '公开' : '私有'}
                        {preset.system ? ' · 系统' : ' · 自建'}
                      </span>
                    </button>
                    {!preset.system && <Button variant="ghost" size="sm" onClick={() => void removePreset(preset)}>删除</Button>}
                  </div>
                ))
              )}
            </div>
          </div>
        </aside>
      </div>

      <section className="mt-12 border-t border-border pt-6">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h2 className="font-serif text-xl leading-none">它怎么工作</h2>
          <span className="label-mono text-[9px]">{algorithm.name} · mechanism</span>
        </div>
        <ol className="mt-6 divide-y divide-border border-t border-border">
          {algorithm.explain.map((line, index) => (
            <li key={line} className="flex items-baseline gap-5 py-3">
              <span className="mono-num w-6 shrink-0 text-[10px] text-accent">{String(index + 1).padStart(2, '0')}</span>
              <span className="text-[13px] leading-6 text-muted-foreground">{line}</span>
            </li>
          ))}
        </ol>
      </section>
    </PageBody>
  );
}

function formatMetrics(plugin: LabPlugin<Record<string, number>, unknown>, seed: number, metrics: Record<string, number>) {
  const parts = [`种子 ${seed}`];
  for (const [key, label] of Object.entries(plugin.metricLabels)) {
    parts.push(`${label} ${(metrics[key] ?? 0).toFixed(2)}`);
  }
  return parts.join(' · ');
}

function snap(field: ParamField, value: number) {
  const steps = Math.round((value - field.min) / field.step);
  const next = field.min + steps * field.step;
  return Math.min(field.max, Math.max(field.min, Number(next.toFixed(4))));
}

function canvasPoint(canvas: HTMLCanvasElement, clientX: number, clientY: number) {
  const rect = canvas.getBoundingClientRect();
  return {
    x: ((clientX - rect.left) / Math.max(1, rect.width)) * WIDTH,
    y: ((clientY - rect.top) / Math.max(1, rect.height)) * HEIGHT,
  };
}

function localKey(algorithmId: string) {
  return `lab-presets-${algorithmId}`;
}

function loadLocalPresets(algorithmId: string): LabPresetDto[] {
  try {
    return JSON.parse(localStorage.getItem(localKey(algorithmId)) || '[]') as LabPresetDto[];
  } catch {
    return [];
  }
}

function saveLocalPreset(algorithmId: string, name: string, params: Record<string, number>): LabPresetDto {
  const preset: LabPresetDto = {
    id: -Date.now(),
    userId: null,
    algorithmId,
    name,
    params: params as LabPresetDto['params'],
    isPublic: false,
    system: false,
  };
  localStorage.setItem(localKey(algorithmId), JSON.stringify([...loadLocalPresets(algorithmId), preset]));
  return preset;
}

function dropLocalPreset(algorithmId: string, id: number) {
  localStorage.setItem(localKey(algorithmId), JSON.stringify(loadLocalPresets(algorithmId).filter((preset) => preset.id !== id)));
}
