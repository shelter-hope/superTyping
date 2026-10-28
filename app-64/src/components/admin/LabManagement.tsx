import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  createSystemLabPreset,
  deleteSystemLabPreset,
  fetchAdminLabAlgorithms,
  fetchAdminLabPresets,
  fetchAdminLabRuns,
  updateLabAlgorithm,
  type LabAlgorithmDto,
  type LabPresetDto,
  type LabRunDto,
} from '@/services/lab';

export default function LabManagement() {
  const [algorithms, setAlgorithms] = useState<LabAlgorithmDto[]>([]);
  const [presets, setPresets] = useState<LabPresetDto[]>([]);
  const [runs, setRuns] = useState<LabRunDto[]>([]);
  const [runUserId, setRunUserId] = useState('');
  const [runAlgorithmId, setRunAlgorithmId] = useState('');
  const [name, setName] = useState('新系统预设');
  const [message, setMessage] = useState('');

  async function load() {
    const [algorithmRows, presetRows] = await Promise.all([
      fetchAdminLabAlgorithms(),
      fetchAdminLabPresets(),
    ]);
    setAlgorithms(algorithmRows);
    setPresets(presetRows);
  }

  useEffect(() => {
    load().catch(() => setMessage('实验室服务不可用'));
  }, []);

  async function saveAlgorithm(row: LabAlgorithmDto) {
    try {
      await updateLabAlgorithm(row.id, {
        name: row.name,
        description: row.description,
        enabled: row.enabled,
        sortOrder: row.sortOrder,
        defaultParams: row.defaultParams,
        schemaVersion: row.schemaVersion,
      });
      setMessage('算法已更新');
      await load();
    } catch {
      setMessage('保存失败，检查参数区间');
    }
  }

  async function addPreset(algorithmId: string, params: Record<string, number>) {
    try {
      await createSystemLabPreset({ algorithmId, name, params, isPublic: true });
      setMessage('系统预设已创建');
      await load();
    } catch {
      setMessage('预设创建失败');
    }
  }

  async function removePreset(id: number) {
    try {
      await deleteSystemLabPreset(id);
      setMessage('系统预设已删除');
      await load();
    } catch {
      setMessage('预设删除失败');
    }
  }

  async function searchRuns() {
    try {
      const page = await fetchAdminLabRuns({
        userId: runUserId.trim() ? Number(runUserId) : undefined,
        algorithmId: runAlgorithmId.trim() || undefined,
        page: 0,
        size: 30,
      });
      setRuns(page.content || []);
      setMessage(`检索到 ${page.totalElements ?? page.content?.length ?? 0} 条实验`);
    } catch {
      setMessage('实验检索失败（检查 lab-svc）');
    }
  }

  function update(id: string, patch: Partial<LabAlgorithmDto>) {
    setAlgorithms((rows) => rows.map((row) => (row.id === id ? { ...row, ...patch } : row)));
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-3xl font-bold">实验室管理</h1>
        <p className="text-muted-foreground">启停算法、调整排序和默认参数，维护系统预设；只读检索实验快照。</p>
      </div>
      {message && <p className="text-sm text-primary">{message}</p>}
      {algorithms.map((row) => (
        <Card key={row.id}>
          <CardHeader><CardTitle>{row.id}</CardTitle></CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-2">
            <Input value={row.name} onChange={(event) => update(row.id, { name: event.target.value })} />
            <Input value={row.description} onChange={(event) => update(row.id, { description: event.target.value })} />
            <label className="text-sm">排序
              <Input type="number" value={row.sortOrder} onChange={(event) => update(row.id, { sortOrder: Number(event.target.value) })} />
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={row.enabled} onChange={(event) => update(row.id, { enabled: event.target.checked })} />
              启用
            </label>
            <Button onClick={() => void saveAlgorithm(row)}>保存算法</Button>
            <Button variant="outline" onClick={() => void addPreset(row.id, row.defaultParams)}>用默认参数新建系统预设</Button>
          </CardContent>
        </Card>
      ))}
      <Card>
        <CardHeader><CardTitle>系统预设</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="新预设名称" />
          {presets.map((preset) => (
            <div key={preset.id} className="flex items-center justify-between gap-2 text-sm">
              <span>{preset.name} · {preset.algorithmId}</span>
              <Button variant="ghost" size="sm" onClick={() => void removePreset(preset.id)}>删除</Button>
            </div>
          ))}
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>实验快照检索</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <div className="grid gap-3 md:grid-cols-3">
            <Input
              value={runUserId}
              onChange={(event) => setRunUserId(event.target.value)}
              placeholder="userId"
              inputMode="numeric"
            />
            <Input
              value={runAlgorithmId}
              onChange={(event) => setRunAlgorithmId(event.target.value)}
              placeholder="algorithmId，如 pso"
            />
            <Button onClick={() => void searchRuns()}>检索</Button>
          </div>
          {runs.length === 0 ? (
            <p className="text-sm text-muted-foreground">暂无结果。按用户 / 算法筛选后检索。</p>
          ) : (
            <div className="divide-y text-sm">
              {runs.map((run) => (
                <div key={run.id} className="flex flex-wrap items-baseline justify-between gap-2 py-2">
                  <span>
                    #{run.id} · u{run.userId} · {run.algorithmId} · seed {run.seed}
                    {run.title ? ` · ${run.title}` : ''}
                  </span>
                  <span className="text-muted-foreground">{run.createdAt || run.status}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
