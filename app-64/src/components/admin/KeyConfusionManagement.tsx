// 按键混淆档案：按用户查看「本该打什么、实际打成了什么」
import { useState } from 'react';
import { KeyRound, Search } from 'lucide-react';
import { adminGetKeyConfusion, type KeyConfusionResult } from '@/services/admin';
import { toast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

const LIMIT_OPTIONS = [10, 20, 50, 100];

function charLabel(char: string) {
  if (char === ' ') return '空格';
  if (char === '\n') return '回车';
  if (char === '\t') return 'Tab';
  return char;
}

export default function KeyConfusionManagement() {
  const [userId, setUserId] = useState('');
  const [limit, setLimit] = useState(20);
  const [result, setResult] = useState<KeyConfusionResult | null>(null);
  const [loading, setLoading] = useState(false);

  async function loadData() {
    const parsed = Number(userId.trim());
    if (!userId.trim() || Number.isNaN(parsed)) {
      toast({ title: '先填一个用户 ID', description: '按键混淆按用户查询。', variant: 'destructive' });
      return;
    }
    setLoading(true);
    try {
      setResult(await adminGetKeyConfusion(parsed, limit));
    } catch (error) {
      toast({
        title: '读取混淆档案失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <div className="label-mono text-muted-foreground">混淆 · CONFUSION</div>
        <h2 className="mt-2 font-serif text-2xl">按键混淆档案</h2>
        <p className="mt-2 max-w-2xl text-sm leading-relaxed text-muted-foreground">
          每一行是一个「期望字符 → 实际打出的字符」的累计次数。它同时喂给难度模型的弱项倍率与遗传算法的定向变异，
          所以这里看到的方向，就是下一局字表的偏向。
        </p>
      </div>

      <div className="flex flex-wrap items-end gap-3 border-y border-border py-4">
        <label className="w-40">
          <span className="label-mono text-muted-foreground">用户 ID</span>
          <Input
            value={userId}
            inputMode="numeric"
            placeholder="例如 12"
            className="mt-2"
            onChange={(event) => setUserId(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') void loadData();
            }}
          />
        </label>
        <label className="w-32">
          <span className="label-mono text-muted-foreground">条数</span>
          <select
            value={limit}
            onChange={(event) => setLimit(Number(event.target.value))}
            className="mt-2 h-9 w-full border border-input bg-background px-2 text-sm outline-none focus:border-primary"
          >
            {LIMIT_OPTIONS.map((option) => (
              <option key={option} value={option}>
                前 {option} 条
              </option>
            ))}
          </select>
        </label>
        <Button variant="default" disabled={loading} onClick={loadData}>
          <Search className="mr-2 h-3.5 w-3.5" />
          {loading ? '读取中…' : '查询'}
        </Button>
        {result ? (
          <span className="label-mono text-muted-foreground">
            共 {result.total} 条记录 · 展示 {result.items.length} 条
          </span>
        ) : null}
      </div>

      {!result ? (
        <p className="text-sm text-muted-foreground">填上用户 ID 后点「查询」，即可看到这个人的按键混淆方向。</p>
      ) : result.items.length === 0 ? (
        <p className="text-sm text-muted-foreground">这个用户还没有混淆记录——要么没打错过，要么埋点还没攒够。</p>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)]">
          <section className="border border-border">
            <header className="flex items-center justify-between border-b border-border px-4 py-3">
              <span className="label-mono text-muted-foreground">明细 · TOP {result.limit}</span>
              <span className="label-mono text-muted-foreground">按次数降序</span>
            </header>
            <ol>
              {result.items.map((item, index) => (
                <li
                  key={`${item.expectedChar}-${item.typedChar}-${index}`}
                  className="grid grid-cols-[2.5rem_1fr_auto] items-baseline gap-3 border-b border-border px-4 py-2.5 last:border-b-0"
                >
                  <span className="label-mono text-muted-foreground">{String(index + 1).padStart(2, '0')}</span>
                  <span className="flex items-baseline gap-2 text-sm">
                    <span className="font-serif text-lg">{charLabel(item.expectedChar)}</span>
                    <span className="text-muted-foreground">应该打成</span>
                    <span className="font-serif text-lg text-accent">{charLabel(item.typedChar)}</span>
                  </span>
                  <span className="mono-num text-sm">{item.confusionCount} 次</span>
                </li>
              ))}
            </ol>
          </section>

          <section className="border border-border">
            <header className="flex items-center justify-between border-b border-border px-4 py-3">
              <span className="label-mono text-muted-foreground">按期望键聚合 · GROUPED</span>
              <span className="label-mono text-muted-foreground">混乱方向</span>
            </header>
            <ol>
              {result.byExpected.map((group) => (
                <li key={group.expectedChar} className="border-b border-border px-4 py-3 last:border-b-0">
                  <div className="flex items-baseline justify-between gap-3">
                    <span className="flex items-center gap-2">
                      <KeyRound className="h-3.5 w-3.5 text-accent" />
                      <span className="font-serif text-lg">{charLabel(group.expectedChar)}</span>
                      <span className="text-sm text-muted-foreground">共错 {group.total} 次</span>
                    </span>
                  </div>
                  <div className="mt-2 flex flex-wrap gap-2">
                    {group.items.map((item, index) => (
                      <span
                        key={`${item.typedChar}-${index}`}
                        className="inline-flex items-baseline gap-1.5 border border-border px-2 py-0.5 text-xs"
                      >
                        <span className="text-muted-foreground">打成</span>
                        <span className="font-serif text-sm text-accent">{charLabel(item.typedChar)}</span>
                        <span className="label-mono text-muted-foreground">{item.confusionCount}</span>
                      </span>
                    ))}
                  </div>
                </li>
              ))}
            </ol>
          </section>
        </div>
      )}
    </div>
  );
}
