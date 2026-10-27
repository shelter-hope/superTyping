import { useMemo, useState } from 'react';
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { parseSeries, summarizeCurve } from '@/features/admin/simulateSeries';
import { adminSimulateGameRule, type GameRuleSimulationPoint } from '@/services/admin';

/** 图表用纸面墨色（SVG 呈现属性不解析 CSS 变量，必须写具体色值） */
const CHART = {
  ink: '#17150f',
  vermilion: '#c8503f',
  ochre: '#b08a45',
  teal: '#5c8a80',
  grid: '#e7e1d3',
  axis: '#8b8272',
  paper: '#f7f4ed',
};

export const PROFILE_PRESETS = [
  {
    id: 'rookie',
    label: '新手',
    detail: '62% → 72%，40–62 cpm',
    accuracies: '0.62,0.64,0.66,0.68,0.70,0.72',
    speeds: '40,45,50,55,60,62',
  },
  {
    id: 'steady',
    label: '普通',
    detail: '稳定 85%，100 cpm',
    accuracies: '0.85',
    speeds: '100',
  },
  {
    id: 'expert',
    label: '高手',
    detail: '93% → 96%，150–190 cpm',
    accuracies: '0.93,0.94,0.95,0.95,0.96,0.96',
    speeds: '150,160,170,180,185,190',
  },
];

export interface RuleSimulatorProps {
  /** 返回草稿参数；勾选「用草稿参数」时调用，返回值直接作为候选规则发给后端 */
  getDraftPayload?: () => Record<string, unknown> | null;
  initialCurve?: GameRuleSimulationPoint[];
  initialVersion?: number | null;
}

export default function RuleSimulator({ getDraftPayload, initialCurve, initialVersion = null }: RuleSimulatorProps) {
  const [curve, setCurve] = useState<GameRuleSimulationPoint[]>(initialCurve ?? []);
  const [curveVersion, setCurveVersion] = useState<number | null>(initialVersion);
  const [accuracies, setAccuracies] = useState(PROFILE_PRESETS[1].accuracies);
  const [speeds, setSpeeds] = useState(PROFILE_PRESETS[1].speeds);
  const [rounds, setRounds] = useState(8);
  const [useDraft, setUseDraft] = useState(false);
  const [simulating, setSimulating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const summary = useMemo(() => summarizeCurve(curve), [curve]);
  const curveData = useMemo(() => curve.map((point) => ({ ...point, accuracyPercent: point.accuracy * 100 })), [curve]);

  async function simulate() {
    setSimulating(true);
    setError(null);
    try {
      const payload = useDraft ? (getDraftPayload?.() ?? undefined) : undefined;
      const result = await adminSimulateGameRule(parseSeries(accuracies), parseSeries(speeds), rounds, payload);
      setCurve(result.curve);
      setCurveVersion(result.version);
    } catch (thrown) {
      setCurve([]);
      setCurveVersion(null);
      setError(thrown instanceof Error ? thrown.message : '推演失败，请检查参数是否符合校验规则');
    } finally {
      setSimulating(false);
    }
  }

  function applyPreset(preset: (typeof PROFILE_PRESETS)[number]) {
    setAccuracies(preset.accuracies);
    setSpeeds(preset.speeds);
    setCurve([]);
    setCurveVersion(null);
    setError(null);
  }

  return (
    <section className="border-t-2 border-foreground pt-6" aria-labelledby="simulate-title">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="label-mono">仿真 · SIMULATOR</p>
          <h2 id="simulate-title" className="mt-2 font-serif text-2xl">调参台：先看曲线，再存草稿</h2>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            推演用的是服务端的同一套调节器。默认按线上生效规则跑；勾选「用草稿参数」就能拿表单里还没保存的值试算，看准了再存。
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {PROFILE_PRESETS.map((preset) => (
            <button
              key={preset.id}
              type="button"
              onClick={() => applyPreset(preset)}
              className="border border-border px-3 py-2 text-left transition-colors hover:border-foreground"
            >
              <span className="block text-sm">{preset.label}</span>
              <span className="block text-[11px] text-muted-foreground">{preset.detail}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[300px_minmax(0,1fr)]">
        <div className="space-y-4 border border-border p-4">
          <label className="block space-y-1 text-sm">
            <span className="text-muted-foreground">推演轮数</span>
            <Input
              type="number"
              min={1}
              max={200}
              value={rounds}
              onChange={(event) => setRounds(Number(event.target.value) || 1)}
            />
          </label>
          <label className="block space-y-1 text-sm">
            <span className="text-muted-foreground">每轮准确率（逗号分隔；短于轮数时沿用最后一项）</span>
            <Input value={accuracies} onChange={(event) => setAccuracies(event.target.value)} />
          </label>
          <label className="block space-y-1 text-sm">
            <span className="text-muted-foreground">每轮速度 cpm（可留空）</span>
            <Input value={speeds} onChange={(event) => setSpeeds(event.target.value)} />
          </label>
          <label className="flex items-start gap-2 text-sm">
            <input
              type="checkbox"
              className="mt-1"
              checked={useDraft}
              onChange={(event) => setUseDraft(event.target.checked)}
            />
            <span>
              用草稿参数（不落库）
              <span className="block text-[11px] text-muted-foreground">读取上方难度表单的当前值；校验不过会直接报错</span>
            </span>
          </label>
          <Button type="button" onClick={() => void simulate()} disabled={simulating} className="w-full">
            {simulating ? '推演中…' : '推演曲线'}
          </Button>
          {error && <p className="text-sm text-accent">{error}</p>}
        </div>

        <div className="space-y-4">
          {curve.length > 0 && summary.first && summary.last ? (
            <>
              <dl className="grid grid-cols-2 gap-px bg-border sm:grid-cols-4">
                {[
                  { label: '起始难度', value: summary.first.difficulty.toFixed(3), hint: 'FIRST' },
                  {
                    label: '末轮难度',
                    value: summary.last.difficulty.toFixed(3),
                    hint: `LAST · v${curveVersion ?? '?'}`,
                  },
                  { label: '末轮准确率', value: `${summary.last.accuracy.toFixed(2)}`, hint: 'A*' },
                  { label: '平均怪物数', value: String(summary.averageMonsters), hint: 'AVG · N' },
                ].map((item) => (
                  <div key={item.label} className="bg-background px-3 py-3">
                    <dt className="text-[11px] text-muted-foreground">{item.label}</dt>
                    <dd className="mono-num mt-1 font-serif text-xl">{item.value}</dd>
                    <dd className="label-mono mt-1">{item.hint}</dd>
                  </div>
                ))}
              </dl>
              <div className="h-64 w-full border border-border p-2">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={curveData} margin={{ top: 8, right: 12, bottom: 0, left: 0 }}>
                    <CartesianGrid stroke={CHART.grid} strokeDasharray="2 4" vertical={false} />
                    <XAxis
                      dataKey="round"
                      tick={{ fill: CHART.axis, fontSize: 11 }}
                      axisLine={{ stroke: CHART.grid }}
                      tickLine={false}
                    />
                    <YAxis
                      yAxisId="left"
                      domain={[0, 1]}
                      tick={{ fill: CHART.axis, fontSize: 11 }}
                      axisLine={false}
                      tickLine={false}
                      width={36}
                    />
                    <YAxis
                      yAxisId="right"
                      orientation="right"
                      domain={[0, 100]}
                      tick={{ fill: CHART.axis, fontSize: 11 }}
                      axisLine={false}
                      tickLine={false}
                      width={36}
                    />
                    <Tooltip
                      contentStyle={{ background: CHART.paper, border: `1px solid ${CHART.grid}`, borderRadius: 0, fontSize: 12 }}
                      labelFormatter={(round) => `第 ${round} 轮`}
                      formatter={(value, name) => [typeof value === 'number' ? value.toFixed(3) : value, name]}
                    />
                    <Legend wrapperStyle={{ fontSize: 12 }} />
                    <Line
                      yAxisId="left"
                      type="monotone"
                      dataKey="difficulty"
                      name="难度 D"
                      stroke={CHART.ink}
                      strokeWidth={1.8}
                      dot={false}
                      isAnimationActive={false}
                    />
                    <Line
                      yAxisId="right"
                      type="monotone"
                      dataKey="attackRatio"
                      name="攻击型"
                      stroke={CHART.vermilion}
                      strokeWidth={1.2}
                      dot={false}
                      isAnimationActive={false}
                    />
                    <Line
                      yAxisId="right"
                      type="monotone"
                      dataKey="evasiveRatio"
                      name="回避型"
                      stroke={CHART.teal}
                      strokeWidth={1.2}
                      dot={false}
                      isAnimationActive={false}
                    />
                    <Line
                      yAxisId="right"
                      type="monotone"
                      dataKey="smartRatio"
                      name="智慧型"
                      stroke={CHART.ochre}
                      strokeWidth={1.2}
                      dot={false}
                      isAnimationActive={false}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <div className="overflow-x-auto border border-border">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border text-left text-[11px] uppercase tracking-wider text-muted-foreground">
                      <th className="px-3 py-2 font-normal">轮次</th>
                      <th className="px-3 py-2 font-normal">难度 D</th>
                      <th className="px-3 py-2 font-normal">准确率</th>
                      <th className="px-3 py-2 font-normal">怪物数</th>
                      <th className="px-3 py-2 font-normal">普通 / 攻击 / 回避 / 智慧</th>
                    </tr>
                  </thead>
                  <tbody>
                    {curve.map((point) => (
                      <tr key={point.round} className="border-b border-border last:border-b-0">
                        <td className="mono-num px-3 py-2">{String(point.round).padStart(2, '0')}</td>
                        <td className="mono-num px-3 py-2 font-serif">{point.difficulty.toFixed(4)}</td>
                        <td className="mono-num px-3 py-2">{point.accuracy.toFixed(2)}</td>
                        <td className="mono-num px-3 py-2">{point.monsterCount}</td>
                        <td className="px-3 py-2 text-muted-foreground">
                          {point.normalRatio} / {point.attackRatio} / {point.evasiveRatio} / {point.smartRatio}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <div className="flex h-64 items-center justify-center border border-dashed border-border">
              <p className="label-mono">{simulating ? '推演中' : '尚未推演'}</p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
