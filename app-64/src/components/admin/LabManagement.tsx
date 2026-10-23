import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { apiClient } from '@/db/client';

interface AlgorithmRow {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
  sortOrder: number;
  defaultParams: Record<string, number>;
  schemaVersion: number;
}

interface PresetRow {
  id: number;
  name: string;
  algorithmId: string;
  params: Record<string, number>;
}

export default function LabManagement() {
  const [algorithms, setAlgorithms] = useState<AlgorithmRow[]>([]);
  const [presets, setPresets] = useState<PresetRow[]>([]);
  const [name, setName] = useState('新系统预设');
  const [message, setMessage] = useState('');

  async function load() {
    const [algorithmResponse, presetResponse] = await Promise.all([
      apiClient.get('/admin/lab/algorithms'),
      apiClient.get('/admin/lab/presets'),
    ]);
    setAlgorithms(algorithmResponse.data.data);
    setPresets(presetResponse.data.data);
  }

  useEffect(() => {
    load().catch(() => setMessage('实验室服务不可用'));
  }, []);

  async function saveAlgorithm(row: AlgorithmRow) {
    await apiClient.put(`/admin/lab/algorithms/${row.id}`, {
      name: row.name,
      description: row.description,
      enabled: row.enabled,
      sortOrder: row.sortOrder,
      defaultParams: row.defaultParams,
      schemaVersion: row.schemaVersion,
    });
    setMessage('算法已更新');
    await load();
  }

  async function addPreset(algorithmId: string, params: Record<string, number>) {
    await apiClient.post('/admin/lab/presets', { algorithmId, name, params, isPublic: true });
    setMessage('系统预设已创建');
    await load();
  }

  async function removePreset(id: number) {
    await apiClient.delete(`/admin/lab/presets/${id}`);
    setMessage('系统预设已删除');
    await load();
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-3xl font-bold">实验室管理</h1>
        <p className="text-muted-foreground">启停算法、调整排序和默认参数，维护系统预设。</p>
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
            <Button onClick={() => void saveAlgorithm(row).catch(() => setMessage('保存失败，检查参数区间'))}>保存算法</Button>
            <Button variant="outline" onClick={() => void addPreset(row.id, row.defaultParams).catch(() => setMessage('预设创建失败'))}>用默认参数新建系统预设</Button>
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
    </div>
  );

  function update(id: string, patch: Partial<AlgorithmRow>) {
    setAlgorithms((rows) => rows.map((row) => (row.id === id ? { ...row, ...patch } : row)));
  }
}
