import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import {
  adminGetGameRule,
  adminGetGameRuleVersions,
  adminPublishGameRule,
  adminSaveGameRuleDraft,
  adminSimulateGameRule,
} from '@/services/admin';
import {
  difficultyFieldGroups,
  difficultyRuleSchema,
  geneticRuleSchema,
  type DifficultyRuleForm,
  type GeneticRuleForm,
} from '@/features/admin/ruleSchema';

export default function GameRuleManagement() {
  const { toast } = useToast();
  const [versions, setVersions] = useState<Array<{ version: number; status: string }>>([]);
  const [curve, setCurve] = useState<Array<Record<string, number>>>([]);
  const [accuracies, setAccuracies] = useState('0.95,0.9,0.8,0.7');
  const [speeds, setSpeeds] = useState('140,120,90,60');
  const difficultyForm = useForm<DifficultyRuleForm>({ resolver: zodResolver(difficultyRuleSchema) });
  const geneticForm = useForm<GeneticRuleForm>({ resolver: zodResolver(geneticRuleSchema) });

  useEffect(() => {
    void reload();
  }, []);

  async function reload() {
    try {
      const [difficulty, genetic, history] = await Promise.all([
        adminGetGameRule('difficulty'),
        adminGetGameRule('genetic'),
        adminGetGameRuleVersions('difficulty'),
      ]);
      difficultyForm.reset(difficulty.payload as DifficultyRuleForm);
      geneticForm.reset(genetic.payload as GeneticRuleForm);
      setVersions(history.map((row) => ({ version: row.version, status: row.status })));
    } catch (error) {
      toast({ title: '规则加载失败', description: error instanceof Error ? error.message : '未知错误', variant: 'destructive' });
    }
  }

  async function saveDifficulty(values: DifficultyRuleForm) {
    const saved = await adminSaveGameRuleDraft('difficulty', values);
    toast({ title: '已保存草稿', description: `难度规则版本 ${saved.version}` });
    await reload();
  }

  async function saveGenetic(values: GeneticRuleForm) {
    const saved = await adminSaveGameRuleDraft('genetic', values);
    toast({ title: '已保存草稿', description: `遗传规则版本 ${saved.version}` });
  }

  async function publish(version: number) {
    await adminPublishGameRule('difficulty', version);
    toast({ title: '已发布', description: `难度规则 v${version} 已生效` });
    await reload();
  }

  async function simulate() {
    const accuracyList = accuracies.split(',').map((item) => Number(item.trim())).filter((item) => !Number.isNaN(item));
    const speedList = speeds.split(',').map((item) => Number(item.trim())).filter((item) => !Number.isNaN(item));
    const result = await adminSimulateGameRule(accuracyList, speedList, Math.max(accuracyList.length, 4));
    setCurve(result.curve);
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>难度规则</CardTitle>
          <p className="text-sm text-muted-foreground">
            全部初始与固定参数均可配置；保存草稿时后端会与当前生效配置合并，避免漏传字段冲掉已有值。
          </p>
        </CardHeader>
        <CardContent>
          <form className="space-y-6" onSubmit={difficultyForm.handleSubmit(saveDifficulty)}>
            {difficultyFieldGroups.map((group) => (
              <div key={group.title} className="space-y-3">
                <h3 className="text-sm font-medium">{group.title}</h3>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {group.fields.map((field) => (
                    <label key={field.key} className="space-y-1 text-sm">
                      <span>{field.label}</span>
                      <Input
                        type="number"
                        step={field.step ?? '0.01'}
                        {...difficultyForm.register(field.key, { valueAsNumber: true })}
                      />
                    </label>
                  ))}
                </div>
              </div>
            ))}
            <div className="text-sm text-destructive">
              {difficultyForm.formState.errors.root?.message || Object.values(difficultyForm.formState.errors)[0]?.message}
            </div>
            <Button type="submit">保存草稿</Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>遗传规则</CardTitle></CardHeader>
        <CardContent>
          <form className="grid gap-3 sm:grid-cols-2" onSubmit={geneticForm.handleSubmit(saveGenetic)}>
            <label className="space-y-1 text-sm">局内父本上限<Input type="number" {...geneticForm.register('inRoundParentLimit', { valueAsNumber: true })} /></label>
            <label className="space-y-1 text-sm">跨局父本上限<Input type="number" {...geneticForm.register('crossRoundParentLimit', { valueAsNumber: true })} /></label>
            <label className="space-y-1 text-sm">局内变异率<Input type="number" step="0.01" {...geneticForm.register('inRoundMutationRate', { valueAsNumber: true })} /></label>
            <label className="space-y-1 text-sm">跨局变异率<Input type="number" step="0.01" {...geneticForm.register('crossRoundMutationRate', { valueAsNumber: true })} /></label>
            <label className="space-y-1 text-sm">交叉方式<Input {...geneticForm.register('crossover')} /></label>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...geneticForm.register('preferWeakChars')} />优先弱项字符</label>
            <Button type="submit">保存草稿</Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>版本与回滚</CardTitle></CardHeader>
        <CardContent className="space-y-2">
          {versions.map((version) => (
            <div key={version.version} className="flex items-center justify-between gap-2 rounded border px-3 py-2">
              <span>v{version.version} · {version.status}</span>
              <Button size="sm" variant="outline" onClick={() => void publish(version.version)}>发布此版本</Button>
            </div>
          ))}
          {versions.length === 0 && <p className="text-sm text-muted-foreground">还没有已保存的版本。未发布时游戏使用代码默认值。</p>}
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>仿真预览</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <div className="grid gap-3 sm:grid-cols-2">
            <div><Label>准确率序列</Label><Input value={accuracies} onChange={(event) => setAccuracies(event.target.value)} /></div>
            <div><Label>速度序列</Label><Input value={speeds} onChange={(event) => setSpeeds(event.target.value)} /></div>
          </div>
          <Button type="button" onClick={() => void simulate()}>生成曲线</Button>
          {curve.length > 0 && (
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={curve}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="round" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="difficulty" stroke="#0f766e" name="难度" />
                  <Line type="monotone" dataKey="attackRatio" stroke="#be185d" name="攻击型" />
                  <Line type="monotone" dataKey="smartRatio" stroke="#b45309" name="智慧型" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
