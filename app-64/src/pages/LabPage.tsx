import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { Application, Graphics } from 'pixi.js';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { deleteLabPreset, fetchLabPresets, fetchMyLabPresets, saveLabPreset, type LabPresetDto } from '@/features/lab/api';
import { boidsSchema, labRegistry } from '@/features/lab/registry';
import {
  createField,
  createFlock,
  stepBoids,
  type Boid,
  type BoidField,
  type BoidMetrics,
  type BoidParams,
} from '@/features/lab/boids';

const WIDTH = 800;
const HEIGHT = 480;

export default function LabPage() {
  const { algorithmId = 'boids' } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const algorithm = labRegistry.find((item) => item.id === algorithmId) ?? labRegistry[0];
  const [params, setParams] = useState<BoidParams>(algorithm.defaults);
  const [seed, setSeed] = useState(7);
  const [paused, setPaused] = useState(false);
  const [presetName, setPresetName] = useState('我的鸟群');
  const [sharePublic, setSharePublic] = useState(true);
  const [notice, setNotice] = useState('');
  const [presets, setPresets] = useState<LabPresetDto[]>([]);
  const [metrics, setMetrics] = useState<BoidMetrics>({ averageSpeed: 0, polarization: 0, clusters: 0 });
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef({
    params,
    paused,
    flock: [] as Boid[],
    field: createField(7, WIDTH, HEIGHT),
  });
  stateRef.current.params = params;
  stateRef.current.paused = paused;
  const parsed = useMemo(() => boidsSchema.safeParse(params), [params]);

  useEffect(() => {
    let cancelled = false;
    Promise.allSettled([fetchLabPresets(algorithm.id), fetchMyLabPresets(algorithm.id)]).then((results) => {
      if (cancelled) return;
      const visible = results[0].status === 'fulfilled' ? results[0].value : [];
      const mine = results[1].status === 'fulfilled' ? results[1].value : loadLocalPresets(algorithm.id);
      const merged = new Map<number, LabPresetDto>();
      [...visible, ...mine].forEach((preset) => merged.set(preset.id, preset));
      setPresets([...merged.values()]);
      const requested = Number(searchParams.get('preset'));
      const match = merged.get(requested);
      if (match) applyPreset(match.params);
    });
    return () => {
      cancelled = true;
    };
  }, [algorithm.id, searchParams]);

  useEffect(() => {
    if (stateRef.current.flock.length === params.count) return;
    stateRef.current.flock = createFlock(params, seed, WIDTH, HEIGHT);
  }, [params, params.count, seed]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let stopped = false;
    let app: Application | null = null;
    const graphics = new Graphics();
    stateRef.current.flock = createFlock(stateRef.current.params, seed, WIDTH, HEIGHT);
    stateRef.current.field = createField(seed, WIDTH, HEIGHT);
    const boot = async () => {
      const view = new Application();
      await view.init({ canvas, width: WIDTH, height: HEIGHT, background: 0x071612, antialias: true });
      if (stopped) {
        view.destroy();
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
        if (!current.paused && step > 0 && boidsSchema.safeParse(current.params).success) {
          const nextMetrics = stepBoids(current.flock, current.params, WIDTH, HEIGHT, current.field);
          if (now - reported > 250) {
            reported = now;
            setMetrics(nextMetrics);
          }
        }
        draw(graphics, current.flock, current.field);
        requestAnimationFrame(frame);
      };
      requestAnimationFrame(frame);
    };
    void boot();
    return () => {
      stopped = true;
      app?.destroy();
    };
  }, [seed]);

  function applyPreset(next: BoidParams) {
    const checked = boidsSchema.safeParse(next);
    if (!checked.success) {
      setNotice('预设参数超出范围');
      return;
    }
    setParams(checked.data);
    stateRef.current.params = checked.data;
  }

  function reset(nextSeed = seed) {
    setSeed(nextSeed);
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
    if (preset.id < 0) {
      dropLocalPreset(algorithm.id, preset.id);
    } else {
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
        <div className="flex gap-2">
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
              className="w-full rounded-[1.5rem] border border-primary/30"
              onContextMenu={(event) => event.preventDefault()}
              onPointerDown={(event) => {
                const point = canvasPoint(event.currentTarget, event.clientX, event.clientY);
                stateRef.current.field.pointer = { ...point, mode: event.button === 2 ? 'repel' : 'attract' };
              }}
              onPointerMove={(event) => {
                if (!stateRef.current.field.pointer) return;
                const point = canvasPoint(event.currentTarget, event.clientX, event.clientY);
                stateRef.current.field.pointer = { ...point, mode: stateRef.current.field.pointer.mode };
              }}
              onPointerUp={() => {
                stateRef.current.field.pointer = null;
              }}
              onPointerLeave={() => {
                stateRef.current.field.pointer = null;
              }}
            />
            <div className="flex flex-wrap gap-2">
              <Button onClick={() => setPaused((value) => !value)}>{paused ? '继续' : '暂停'}</Button>
              <Button variant="outline" onClick={() => {
                stateRef.current.paused = true;
                setPaused(true);
                setMetrics(stepBoids(stateRef.current.flock, params, WIDTH, HEIGHT, stateRef.current.field));
              }}>单步</Button>
              <Button variant="outline" onClick={() => reset()}>重置</Button>
              <Button variant="outline" onClick={() => reset(Math.floor(Math.random() * 10000))}>随机种子</Button>
              <Button variant="outline" onClick={() => canvasRef.current?.requestFullscreen()}>全屏</Button>
            </div>
            <p className="text-sm">种子 {seed} · 平均速度 {metrics.averageSpeed.toFixed(2)} · 极化度 {metrics.polarization.toFixed(2)} · 聚簇 {metrics.clusters}</p>
            <p className="text-sm text-muted-foreground">左键吸引，右键驱散。青圈是障碍，洋红点是捕食者。</p>
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
                  value={params[field.key]}
                  onChange={(event) => {
                    const value = Number(event.target.value);
                    setParams((current) => ({ ...current, [field.key]: field.key === 'count' ? Math.round(value) : value }));
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
        <CardHeader><CardTitle>局部规则，全局涌现</CardTitle></CardHeader>
        <CardContent className="grid gap-3 text-sm md:grid-cols-3">
          <p>分离让个体躲开过近的邻居。</p>
          <p>对齐让个体朝邻居的平均方向转。</p>
          <p>凝聚让个体靠近邻居的中心。没有一只鸟知道群体的形状。</p>
        </CardContent>
      </Card>
    </div>
  );
}

function draw(graphics: Graphics, flock: Boid[], field: BoidField) {
  graphics.clear();
  for (const obstacle of field.obstacles) {
    graphics.circle(obstacle.x, obstacle.y, obstacle.radius).stroke({ color: 0x3dffe0, width: 2, alpha: 0.7 });
  }
  for (const predator of field.predators) {
    graphics.circle(predator.x, predator.y, 6).fill({ color: 0xff4fd8 });
  }
  for (const boid of flock) {
    graphics.circle(boid.x, boid.y, 2.5).fill({ color: 0x3dffe0 });
  }
}

function canvasPoint(canvas: HTMLCanvasElement, clientX: number, clientY: number) {
  const rect = canvas.getBoundingClientRect();
  return {
    x: ((clientX - rect.left) / rect.width) * WIDTH,
    y: ((clientY - rect.top) / rect.height) * HEIGHT,
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

function saveLocalPreset(algorithmId: string, name: string, params: BoidParams): LabPresetDto {
  const preset: LabPresetDto = {
    id: -Date.now(),
    userId: null,
    algorithmId,
    name,
    params,
    isPublic: false,
    system: false,
  };
  const next = [...loadLocalPresets(algorithmId), preset];
  localStorage.setItem(localKey(algorithmId), JSON.stringify(next));
  return preset;
}

function dropLocalPreset(algorithmId: string, id: number) {
  const next = loadLocalPresets(algorithmId).filter((preset) => preset.id !== id);
  localStorage.setItem(localKey(algorithmId), JSON.stringify(next));
}
