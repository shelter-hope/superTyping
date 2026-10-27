import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import RuleSimulator from '@/components/admin/RuleSimulator';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { useToast } from '@/hooks/use-toast';
import {
  adminGetGameRule,
  adminGetGameRuleVersions,
  adminPublishGameRule,
  adminSaveGameRuleDraft,
} from '@/services/admin';
import {
  difficultyFieldGroups,
  difficultyRuleSchema,
  geneticRuleSchema,
  type DifficultyRuleForm,
  type DifficultyRuleInput,
  type GeneticRuleForm,
} from '@/features/admin/ruleSchema';

const SELECT_CLASS =
  'flex h-10 w-full rounded-[2px] border border-border bg-transparent px-3 py-2 text-[14px] text-foreground outline-none transition-[border-color] hover:border-foreground/35 focus-visible:border-foreground';

export default function GameRuleManagement() {
  const { toast } = useToast();
  const [versions, setVersions] = useState<Array<{ version: number; status: string }>>([]);
  const difficultyForm = useForm<DifficultyRuleInput, unknown, DifficultyRuleForm>({
    resolver: zodResolver(difficultyRuleSchema),
  });
  const geneticForm = useForm<GeneticRuleForm>({ resolver: zodResolver(geneticRuleSchema) });

  useEffect(() => {
    void reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function reportError(title: string, error: unknown) {
    toast({ title, description: error instanceof Error ? error.message : '未知错误', variant: 'destructive' });
  }

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
      reportError('规则加载失败', error);
    }
  }

  async function saveDifficulty(values: DifficultyRuleForm) {
    try {
      const saved = await adminSaveGameRuleDraft('difficulty', values);
      toast({ title: '已保存草稿', description: `难度规则版本 ${saved.version}` });
      await reload();
    } catch (error) {
      reportError('保存失败', error);
    }
  }

  async function saveGenetic(values: GeneticRuleForm) {
    try {
      const saved = await adminSaveGameRuleDraft('genetic', values);
      toast({ title: '已保存草稿', description: `遗传规则版本 ${saved.version}` });
    } catch (error) {
      reportError('保存失败', error);
    }
  }

  async function publish(version: number) {
    try {
      await adminPublishGameRule('difficulty', version);
      toast({ title: '已发布', description: `难度规则 v${version} 已生效` });
      await reload();
    } catch (error) {
      reportError('发布失败', error);
    }
  }

  return (
    <div className="space-y-8">
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
                <h3 className="label-mono">{group.title}</h3>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {group.fields.map((field) => (
                    <label key={field.key} className="space-y-1 text-sm">
                      <span className="text-muted-foreground">{field.label}</span>
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
        <CardHeader>
          <CardTitle>遗传规则</CardTitle>
          <p className="text-sm text-muted-foreground">
            父本上限决定多样性，变异率决定探索强度；交叉方式与锦标赛规模控制子代来源。
          </p>
        </CardHeader>
        <CardContent>
          <form className="grid gap-3 sm:grid-cols-2" onSubmit={geneticForm.handleSubmit(saveGenetic)}>
            <label className="space-y-1 text-sm">
              <span className="text-muted-foreground">局内父本上限</span>
              <Input type="number" {...geneticForm.register('inRoundParentLimit', { valueAsNumber: true })} />
            </label>
            <label className="space-y-1 text-sm">
              <span className="text-muted-foreground">跨局父本上限</span>
              <Input type="number" {...geneticForm.register('crossRoundParentLimit', { valueAsNumber: true })} />
            </label>
            <label className="space-y-1 text-sm">
              <span className="text-muted-foreground">局内变异率</span>
              <Input type="number" step="0.01" {...geneticForm.register('inRoundMutationRate', { valueAsNumber: true })} />
            </label>
            <label className="space-y-1 text-sm">
              <span className="text-muted-foreground">跨局变异率</span>
              <Input
                type="number"
                step="0.01"
                {...geneticForm.register('crossRoundMutationRate', { valueAsNumber: true })}
              />
            </label>
            <label className="space-y-1 text-sm">
              <span className="text-muted-foreground">交叉方式</span>
              <select className={SELECT_CLASS} {...geneticForm.register('crossover')}>
                <option value="SINGLE_POINT">单点交叉</option>
                <option value="TWO_POINT">两点交叉</option>
                <option value="UNIFORM">均匀交叉</option>
              </select>
            </label>
            <label className="space-y-1 text-sm">
              <span className="text-muted-foreground">精英保留比例</span>
              <Input
                type="number"
                step="0.05"
                {...geneticForm.register('eliteRatio', { valueAsNumber: true })}
              />
            </label>
            <label className="space-y-1 text-sm">
              <span className="text-muted-foreground">锦标赛规模</span>
              <Input
                type="number"
                step="1"
                {...geneticForm.register('tournamentSize', { valueAsNumber: true })}
              />
            </label>
            <label className="flex items-center gap-2 self-end text-sm">
              <input type="checkbox" {...geneticForm.register('preferWeakChars')} />
              优先弱项字符
            </label>
            <div className="text-sm text-destructive sm:col-span-2">
              {geneticForm.formState.errors.root?.message || Object.values(geneticForm.formState.errors)[0]?.message}
            </div>
            <Button type="submit" className="justify-self-start">
              保存草稿
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>版本与回滚</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {versions.map((version) => (
            <div
              key={version.version}
              className="flex items-center justify-between gap-2 border-b border-border px-1 py-2 last:border-b-0"
            >
              <span className="mono-num text-sm">
                v{version.version} · {version.status}
              </span>
              <Button size="sm" variant="outline" onClick={() => void publish(version.version)}>
                发布此版本
              </Button>
            </div>
          ))}
          {versions.length === 0 && (
            <p className="text-sm text-muted-foreground">还没有已保存的版本。未发布时游戏使用代码默认值。</p>
          )}
        </CardContent>
      </Card>

      <RuleSimulator getDraftPayload={() => difficultyForm.getValues() as Record<string, unknown>} />
    </div>
  );
}
