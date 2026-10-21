// 高级配置：字符规则 / 移动规则 / 难度参数（JSON，存于 system_params），以及配置模板的保存与套用
import { useEffect, useState } from 'react';
import {
  adminDeleteConfigTemplate,
  adminGetConfigTemplate,
  adminGetConfigTemplates,
  adminGetRules,
  adminSaveConfigTemplate,
  adminSaveRules,
  type AdminRuleKey,
  type ConfigTemplateSummary,
} from '@/db/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Loader2, Save, Trash2, Upload } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

const RULES: { key: AdminRuleKey; title: string; hint: string }[] = [
  { key: 'character-rules', title: '字符生成规则', hint: '例：{"charTypes": ["lower", "upper", "number", "punct"]}' },
  { key: 'movement-rules', title: '移动规则', hint: '例：{"minSpeed": 0.8, "maxSpeed": 3.0}，minSpeed 须大于 0 且不大于 maxSpeed' },
  { key: 'difficulty-params', title: '难度参数', hint: '例：{"minDifficulty": 0, "maxDifficulty": 1}' },
];

type Drafts = Record<AdminRuleKey, string>;

const EMPTY_DRAFTS: Drafts = { 'character-rules': '', 'movement-rules': '', 'difficulty-params': '' };

function parseObject(text: string): Record<string, unknown> {
  const value: unknown = JSON.parse(text);
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('必须是 JSON 对象');
  }
  return value as Record<string, unknown>;
}

export default function AdvancedConfigManagement() {
  const [drafts, setDrafts] = useState<Drafts>(EMPTY_DRAFTS);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<AdminRuleKey | null>(null);
  const [templates, setTemplates] = useState<ConfigTemplateSummary[]>([]);
  const [templateName, setTemplateName] = useState('');
  const { toast } = useToast();

  useEffect(() => {
    loadAll();
  }, []);

  async function loadAll() {
    setLoading(true);
    try {
      const [rules, list] = await Promise.all([
        Promise.all(RULES.map((r) => adminGetRules(r.key))),
        adminGetConfigTemplates(),
      ]);
      const next = { ...EMPTY_DRAFTS };
      RULES.forEach((r, i) => {
        next[r.key] = JSON.stringify(rules[i], null, 2);
      });
      setDrafts(next);
      setTemplates(list);
    } catch (e) {
      toast({ title: '加载配置失败', description: e instanceof Error ? e.message : '未知错误', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  }

  async function handleSaveRule(key: AdminRuleKey) {
    let value: Record<string, unknown>;
    try {
      value = parseObject(drafts[key]);
    } catch (e) {
      toast({ title: 'JSON 格式错误', description: e instanceof Error ? e.message : String(e), variant: 'destructive' });
      return;
    }
    setSaving(key);
    try {
      await adminSaveRules(key, value);
      toast({ title: '已保存', description: RULES.find((r) => r.key === key)?.title });
    } catch (e) {
      toast({ title: '保存失败', description: e instanceof Error ? e.message : '未知错误', variant: 'destructive' });
    } finally {
      setSaving(null);
    }
  }

  async function handleSaveTemplate() {
    const name = templateName.trim();
    if (!name) return;
    let config: Record<string, unknown>;
    try {
      config = Object.fromEntries(RULES.map((r) => [r.key, parseObject(drafts[r.key])]));
    } catch (e) {
      toast({ title: 'JSON 格式错误', description: e instanceof Error ? e.message : String(e), variant: 'destructive' });
      return;
    }
    try {
      await adminSaveConfigTemplate(name, config);
      toast({ title: '模板已保存', description: `同名模板会被覆盖：${name}` });
      setTemplateName('');
      setTemplates(await adminGetConfigTemplates());
    } catch (e) {
      toast({ title: '保存模板失败', description: e instanceof Error ? e.message : '未知错误', variant: 'destructive' });
    }
  }

  /** 套用模板只填入编辑框，需逐项点"保存"才会生效 */
  async function handleApplyTemplate(t: ConfigTemplateSummary) {
    try {
      const detail = await adminGetConfigTemplate(t.id);
      const config = (detail.config ?? {}) as Record<string, unknown>;
      setDrafts((prev) => {
        const next = { ...prev };
        RULES.forEach((r) => {
          if (config[r.key] !== undefined) next[r.key] = JSON.stringify(config[r.key], null, 2);
        });
        return next;
      });
      toast({ title: `已载入模板「${t.name}」`, description: '确认后请逐项点击保存使其生效' });
    } catch (e) {
      toast({ title: '载入模板失败', description: e instanceof Error ? e.message : '未知错误', variant: 'destructive' });
    }
  }

  async function handleDeleteTemplate(t: ConfigTemplateSummary) {
    if (!confirm(`确定删除模板「${t.name}」吗？`)) return;
    try {
      await adminDeleteConfigTemplate(t.id);
      setTemplates((prev) => prev.filter((x) => x.id !== t.id));
    } catch (e) {
      toast({ title: '删除失败', description: e instanceof Error ? e.message : '未知错误', variant: 'destructive' });
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="grid gap-4 lg:grid-cols-3">
        {RULES.map((r) => (
          <Card key={r.key}>
            <CardHeader>
              <CardTitle className="text-base">{r.title}</CardTitle>
              <CardDescription className="text-xs">{r.hint}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <Textarea
                className="font-mono text-xs min-h-[180px]"
                value={drafts[r.key]}
                onChange={(e) => setDrafts((prev) => ({ ...prev, [r.key]: e.target.value }))}
              />
              <Button size="sm" onClick={() => handleSaveRule(r.key)} disabled={saving !== null}>
                {saving === r.key ? <Loader2 className="w-4 h-4 mr-1 animate-spin" /> : <Save className="w-4 h-4 mr-1" />}
                保存
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">配置模板</CardTitle>
          <CardDescription>把上面三项当前编辑框中的内容存为模板，之后可一键载入</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex gap-2">
            <Input
              placeholder="模板名称"
              value={templateName}
              maxLength={100}
              onChange={(e) => setTemplateName(e.target.value)}
            />
            <Button onClick={handleSaveTemplate} disabled={!templateName.trim()}>
              <Save className="w-4 h-4 mr-1" />
              存为模板
            </Button>
          </div>
          {templates.length === 0 ? (
            <p className="text-sm text-muted-foreground">暂无模板</p>
          ) : (
            <div className="space-y-2">
              {templates.map((t) => (
                <div key={t.id} className="flex items-center justify-between rounded-md border px-3 py-2">
                  <div>
                    <p className="font-medium text-sm">{t.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {t.updateTime ? new Date(t.updateTime).toLocaleString() : '-'}
                      {t.createdBy != null ? ` · 创建人 ${t.createdBy}` : ''}
                    </p>
                  </div>
                  <div className="flex gap-1">
                    <Button variant="ghost" size="sm" onClick={() => handleApplyTemplate(t)}>
                      <Upload className="w-4 h-4 mr-1" />
                      载入
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => handleDeleteTemplate(t)}>
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
