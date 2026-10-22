import { useEffect, useMemo, useRef, useState, type PointerEvent } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { Application, Graphics } from 'pixi.js';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { deleteLabPreset, fetchLabPresets, fetchMyLabPresets, saveLabPreset, type LabPresetDto } from '@/features/lab/api';
import { labRegistry } from '@/features/lab/registry';
import type { LabPlugin, ParamField, PointerForce } from '@/features/lab/plugin';

const WIDTH = 800;
const HEIGHT = 480;
let bootQueue: Promise<void> = Promise.resolve();

export default function LabPage() {
  const { algorithmId = 'boids' } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const algorithm = labRegistry.find((item) => item.id === algorithmId) ?? labRegistry[0];
  const [params, setParams] = useState<Record<string, number>>(algorithm.defaults);
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
    sim: algorithm.create(algorithm.defaults, 7, WIDTH, HEIGHT) as unknown,
  });
  const parsed = useMemo(() => algorithm.schema.safeParse(params), [algorithm, params]);
  stateRef.current.plugin = algorithm as LabPlugin<Record<string, number>, unknown>;
  stateRef.current.params = params;
  stateRef.current.paused = paused;
  stateRef.current.seed = seed;
  stateRef.current.valid = parsed.success;

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

  return (
    <div className="container space-y-4 py-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold">群体智能实验室</h1>
          <p className="text-muted-foreground">{algorithm.description}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {labRegistry.map((item) => (
            <Button key={item.id} asChild variant={item.id === algorithm.id ? 'default' : 'outline'} size="sm">
              <Link to={`/lab/${item.id}`}>{item.name}</Link>
            </Button>
          ))}
        </div>
      </div>
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_280px]">
        <Card>
          <CardContent className="space-y-3 p-4">
            <canvas
              ref={canvasRef}
              width={WIDTH}
              height={HEIGHT}
              className="h-auto w-full rounded-[1.5rem] border border-primary/30"
              onContextMenu={(event) => event.preventDefault()}
              onPointerDown={(event) => stateRef.current.plugin.onPointer?.(stateRef.current.sim, pointAt(event))}
              onPointerMove={(event) => {
                if (event.buttons === 0) return;
                stateRef.current.plugin.onPointer?.(stateRef.current.sim, pointAt(event));
              }}
              onPointerUp={() => stateRef.current.plugin.onPointer?.(stateRef.current.sim, null)}
              onPointerLeave={() => stateRef.current.plugin.onPointer?.(stateRef.current.sim, null)}
            />
            <div className="flex flex-wrap gap-2">
              <Button onClick={() => setPaused((value) => !value)}>{paused ? '继续' : '暂停'}</Button>
              <Button variant="outline" onClick={() => {
                stateRef.current.paused = true;
                setPaused(true);
                const nextMetrics = stateRef.current.plugin.step(stateRef.current.sim, params, WIDTH, HEIGHT);
                if (metricsNode.current) metricsNode.current.textContent = formatMetrics(algorithm as LabPlugin<Record<string, number>, unknown>, seed, nextMetrics);
              }}>单步</Button>
              <Button variant="outline" onClick={() => reset()}>重置</Button>
              <Button variant="outline" onClick={() => reset(Math.floor(Math.random() * 10000))}>随机种子</Button>
              <Button variant="outline" onClick={() => canvasRef.current?.requestFullscreen()}>全屏</Button>
            </div>
            <p ref={metricsNode} className="text-sm">{formatMetrics(algorithm as LabPlugin<Record<string, number>, unknown>, seed, {})}</p>
            {algorithm.pointerHint && <p className="text-sm text-muted-foreground">{algorithm.pointerHint}</p>}
            {!parsed.success && <p className="text-sm text-destructive">参数超出范围</p>}
            {notice && <p className="text-sm text-primary">{notice}</p>}
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>参数</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            {algorithm.fields.map((field) => (
              <label key={field.key} className="block text-sm">
                <span className="mb-1 flex justify-between"><span>{field.label}</span><span>{params[field.key]}</span></span>
                <input
                  className="w-full accent-primary"
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
              </label>
            ))}
            <Input value={presetName} onChange={(event) => setPresetName(event.target.value)} placeholder="预设名称" />
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={sharePublic} onChange={(event) => setSharePublic(event.target.checked)} />
              公开分享
            </label>
            <Button className="w-full" onClick={() => void savePreset()}>保存预设</Button>
            <div className="space-y-2">
              {presets.map((preset) => (
                <div key={preset.id} className="flex items-center gap-2">
                  <Button variant="outline" size="sm" className="flex-1" onClick={() => {
                    applyPreset(preset.params);
                    setSearchParams({ preset: String(preset.id) });
                  }}>{preset.name}</Button>
                  {!preset.system && <Button variant="ghost" size="sm" onClick={() => void removePreset(preset)}>删除</Button>}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
      <Card>
        <CardHeader><CardTitle>{algorithm.name}</CardTitle></CardHeader>
        <CardContent className="grid gap-3 text-sm md:grid-cols-3">
          {algorithm.explain.map((line) => <p key={line}>{line}</p>)}
        </CardContent>
      </Card>
    </div>
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
