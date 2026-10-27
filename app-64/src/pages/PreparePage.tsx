// 准备页面 - 游戏配置
import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { useGameConfig } from '@/contexts/GameConfigContext';
import { getUserDictionaries, getSystemDictionaries, getGameRecommend, getGameCharTypes, updateGameCharTypes } from '@/db/api';
import type { CustomDictionary, SystemDictionary } from '@/types/database';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import GameTutorial from '@/components/game/GameTutorial';
import { PageHeader, PageBody } from '@/components/common/PageHeader';
import { useToast } from '@/hooks/use-toast';
import { selectedCharsLabel } from '@/features/game/utils/gameRuntime';

const MODES = [
  {
    value: 'endless' as const,
    title: '无尽适应',
    en: 'ENDLESS',
    desc: '打完一局，字库就按这一局的失误重排一次。难度跟着你走，不跟着设定走。',
  },
  {
    value: 'limit' as const,
    title: '限时冲刺',
    en: 'TIMED',
    desc: '固定时间里尽可能多打。适合热身，也适合验证某个键位到底练没练成。',
  },
  {
    value: 'dictionary' as const,
    title: '词源界定',
    en: 'LEXICAL',
    desc: '只从指定词库出词。专业术语、代码关键字、自己的高频词都可以。',
  },
];

const GLYPH_OPTIONS = [
  { key: 'enableLowercase' as const, label: '小写字母', sample: 'a-z' },
  { key: 'enableUppercase' as const, label: '大写字母', sample: 'A-Z' },
  { key: 'enableNumbers' as const, label: '数字', sample: '0-9' },
  { key: 'enablePunctuation' as const, label: '标点', sample: ',.!?;:\'"' },
  { key: 'enableOperators' as const, label: '运算符', sample: '+-*/=<>' },
  { key: 'enableSpecialChars' as const, label: '特殊字符', sample: '@#$%^&*' },
  { key: 'enableChinese' as const, label: '中文字符', sample: '汉字' },
];

export default function PreparePage() {
  const { user } = useAuth();
  const { config, updateConfig } = useGameConfig();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [dictionaries, setDictionaries] = useState<CustomDictionary[]>([]);
  const [systemDictionaries, setSystemDictionaries] = useState<SystemDictionary[]>([]);
  const [selectedDictId, setSelectedDictId] = useState<string>('');
  const [selectedSystemDictId, setSelectedSystemDictId] = useState<string>('');
  const [recommendLoading, setRecommendLoading] = useState(false);

  const userId = user?.userId;

  useEffect(() => {
    if (userId) {
      loadDictionaries();
    }
  }, [userId]);

  // 从后端加载某个模式下的字符类型配置，并刷新本地开关状态
  async function loadCharTypesForMode(mode: 'endless' | 'dictionary' | 'limit') {
    if (!userId) return;
    try {
      const dto = await getGameCharTypes(mode);
      const types = new Set(
        (dto.charTypes ?? (dto.charType ? dto.charType.split(',') : []))
          .map((s: string) => s.trim())
          .filter(Boolean),
      );
      const next = {
        ...config,
        enableLowercase: types.has('lower'),
        enableUppercase: types.has('upper'),
        enableNumbers: types.has('number'),
        enablePunctuation: types.has('punct'),
        enableOperators: types.has('operator'),
        enableSpecialChars: types.has('special'),
        enableChinese: types.has('chinese'),
      };
      updateConfig({
        enableLowercase: next.enableLowercase,
        enableUppercase: next.enableUppercase,
        enableNumbers: next.enableNumbers,
        enablePunctuation: next.enablePunctuation,
        enableOperators: next.enableOperators,
        enableSpecialChars: next.enableSpecialChars,
        enableChinese: next.enableChinese,
        selectedChars: selectedCharsLabel(next),
      });
    } catch (e) {
      console.error('加载字符类型配置失败:', e);
    }
  }

  // 首次进入准备界面时，为当前模式加载一次字符类型配置
  useEffect(() => {
    if (!userId) return;
    void loadCharTypesForMode(config.gameMode as 'endless' | 'dictionary' | 'limit');
  }, [userId]);

  function buildCharTypesFromConfig(next: typeof config) {
    const types: string[] = [];
    if (next.enableLowercase) types.push('lower');
    if (next.enableUppercase) types.push('upper');
    if (next.enableNumbers) types.push('number');
    if (next.enablePunctuation) types.push('punct');
    if (next.enableOperators) types.push('operator');
    if (next.enableSpecialChars) types.push('special');
    if (next.enableChinese) types.push('chinese');
    return types;
  }

  async function persistCharTypes(next: typeof config) {
    const charTypes = buildCharTypesFromConfig(next);
    if (charTypes.length === 0) {
      toast({
        title: '至少选择一种字符类型',
        description: '请至少保留一种字符类型，否则无法生成练习内容。',
        variant: 'destructive',
      });
      return;
    }
    try {
      await updateGameCharTypes(config.gameMode, charTypes);
    } catch (e) {
      console.error('保存字符类型配置失败:', e);
      toast({
        title: '保存失败',
        description: e instanceof Error ? e.message : '请稍后重试',
        variant: 'destructive',
      });
    }
  }

  async function loadDictionaries() {
    if (!userId) return;

    try {
      const [data, system] = await Promise.all([
        getUserDictionaries(),
        getSystemDictionaries().catch(() => [] as SystemDictionary[]),
      ]);
      setDictionaries(data);
      setSystemDictionaries(system);
    } catch (error) {
      console.error('加载词库失败:', error);
    }
  }

  async function handleRecommend() {
    if (!user) return;
    setRecommendLoading(true);
    try {
      const res = await getGameRecommend(config.gameMode);
      const mode = (res.suggestedMode as 'endless' | 'dictionary' | 'limit') || 'endless';
      const words = res.previewWords ?? [];
      if (words.length > 0) {
        updateConfig({
          gameMode: 'dictionary',
          customDictionary: words,
          customDictIds: [],
          systemDictIds: [],
          selectedChars: selectedCharsLabel(config),
        });
        setSelectedDictId('recommend');
        setSelectedSystemDictId('');
        toast({
          title: '已应用推荐配置',
          description: res.summary ?? `已为你生成 ${words.length} 个推荐练习词，点击「开始游戏」即可使用。`,
        });
      } else {
        updateConfig({ gameMode: mode });
        toast({
          title: '已应用推荐配置',
          description: res.summary ?? '建议使用无尽适应模式，多玩几局后会给出更针对性的推荐。',
        });
      }
    } catch (e) {
      console.error('获取推荐配置失败:', e);
      toast({
        title: '获取推荐失败',
        description: e instanceof Error ? e.message : '请稍后重试',
        variant: 'destructive',
      });
    } finally {
      setRecommendLoading(false);
    }
  }

  function handleStartGame() {
    if (config.gameMode === 'dictionary') {
      const usingRecommend = selectedDictId === 'recommend' && (config.customDictionary?.length ?? 0) > 0;
      const selectedDict = selectedDictId && selectedDictId !== 'recommend'
        ? dictionaries.find((dict) => String(dict.dictId) === selectedDictId)
        : undefined;
      if (selectedDictId && selectedDictId !== 'recommend' && !selectedDict) {
        toast({ title: '词库不存在', description: '请重新选择词库', variant: 'destructive' });
        return;
      }
      if (selectedDict && selectedDict.wordCount === 0 && !selectedDict.words?.length) {
        toast({ title: '词库是空的', description: '这个词库还没有可用词语', variant: 'destructive' });
        return;
      }
      const systemId = selectedSystemDictId ? Number(selectedSystemDictId) : undefined;
      if (!usingRecommend && !selectedDict && !systemId) {
        toast({
          title: '请选择词库',
          description: '词源界定模式需要选择自定义词库、系统词库，或使用推荐词',
          variant: 'destructive',
        });
        return;
      }
      updateConfig({
        customDictionary: usingRecommend ? config.customDictionary : (selectedDict?.words?.length ? selectedDict.words : undefined),
        customDictIds: selectedDict ? [selectedDict.dictId] : [],
        systemDictIds: systemId ? [systemId] : [],
        selectedChars: selectedCharsLabel(config),
      });
    } else {
      updateConfig({ selectedChars: selectedCharsLabel(config) });
    }
    navigate('/apps/typing/game');
  }

  const currentMode = MODES.find((m) => m.value === config.gameMode) ?? MODES[0];
  const glyphCount = GLYPH_OPTIONS.filter((g) => config[g.key]).length;
  const selectedDictName =
    selectedDictId === 'recommend'
      ? `推荐词（${config.customDictionary?.length ?? 0} 词）`
      : dictionaries.find((dict) => String(dict.dictId) === selectedDictId)?.name ??
        (selectedSystemDictId
          ? systemDictionaries.find((dict) => String(dict.dictId) === selectedSystemDictId)?.name ?? '—'
          : '—');

  return (
    <PageBody>
      <PageHeader
        eyebrow="对局 · PREPARATION"
        title="配置这一局"
        description="模式决定字从哪里来，字符类型决定字长什么样。难度不用你选——它每轮跟着你的失误走。"
        actions={
          <>
            <GameTutorial />
            <Button variant="outline" size="sm" onClick={handleRecommend} disabled={recommendLoading}>
              {recommendLoading ? '分析中…' : '根据我的数据推荐'}
            </Button>
          </>
        }
      />

      <div className="mt-10 grid gap-x-10 gap-y-10 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-12">
          {/* 01 模式 */}
          <section>
            <div className="flex items-baseline justify-between border-b border-border pb-2">
              <span className="label-mono">01 · 模式 / MODE</span>
              <span className="label-mono text-muted-foreground">{currentMode.en}</span>
            </div>

            <RadioGroup
              value={config.gameMode}
              onValueChange={(value) => {
                const mode = value as 'endless' | 'dictionary' | 'limit';
                updateConfig({ gameMode: mode });
                // 仅在模式切换时请求一次对应模式的字符类型配置
                void loadCharTypesForMode(mode);
              }}
              className="mt-1"
            >
              {MODES.map((mode) => {
                const active = config.gameMode === mode.value;
                return (
                  <div key={mode.value} className="border-b border-border">
                    <div className="flex items-start gap-4 py-4">
                      <RadioGroupItem value={mode.value} id={`mode-${mode.value}`} className="mt-[5px]" />
                      <Label htmlFor={`mode-${mode.value}`} className="cursor-pointer flex-1">
                        <div className="flex flex-wrap items-baseline gap-x-3">
                          <span className="font-serif text-xl leading-none">{mode.title}</span>
                          <span className="label-mono text-muted-foreground">{mode.en}</span>
                        </div>
                        <p className="mt-2 max-w-[46ch] text-[13px] leading-6 text-muted-foreground">
                          {mode.desc}
                        </p>
                      </Label>
                      <span className={`mt-1 text-[11px] ${active ? 'text-accent' : 'text-transparent'}`}>
                        ●
                      </span>
                    </div>

                    {/* 模式内配置：贴着被选中的那一行往下长 */}
                    {config.gameMode === 'limit' && mode.value === 'limit' ? (
                      <div className="flex flex-wrap items-center gap-4 pb-5 pl-8">
                        <span className="label-mono text-muted-foreground">时长</span>
                        <Select
                          value={String(config.timeLimitSeconds || 60)}
                          onValueChange={(value) => updateConfig({ timeLimitSeconds: Number(value) })}
                        >
                          <SelectTrigger id="time-limit" className="h-9 w-[220px]">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="30">30 秒 · 快速挑战</SelectItem>
                            <SelectItem value="60">60 秒 · 标准</SelectItem>
                            <SelectItem value="120">120 秒 · 长局</SelectItem>
                            <SelectItem value="180">180 秒 · 马拉松</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    ) : null}

                    {config.gameMode === 'dictionary' && mode.value === 'dictionary' ? (
                      <div className="space-y-4 pb-6 pl-8">
                        {selectedDictId === 'recommend' && config.customDictionary && config.customDictionary.length > 0 ? (
                          <p className="text-[13px] text-accent">
                            已应用推荐词库（{config.customDictionary.length} 个词），可直接开打。
                          </p>
                        ) : null}

                        {dictionaries.length === 0 && selectedDictId !== 'recommend' ? (
                          <div className="border border-border p-4">
                            <p className="label-mono text-muted-foreground">
                              {systemDictionaries.length > 0 ? '暂无自定义词库，可改用系统词库' : '暂无自定义词库'}
                            </p>
                            <Link to="/apps/typing/dictionary" className="mt-3 inline-block">
                              <Button variant="outline" size="sm">
                                前往上传词库 →
                              </Button>
                            </Link>
                          </div>
                        ) : null}

                        {dictionaries.length > 0 || selectedDictId === 'recommend' ? (
                          <div className="flex flex-wrap items-center gap-4">
                            <span className="label-mono text-muted-foreground">自定义</span>
                            <Select value={selectedDictId} onValueChange={setSelectedDictId}>
                              <SelectTrigger id="dictionary-select" className="h-9 w-[260px]">
                                <SelectValue placeholder="请选择一个词库" />
                              </SelectTrigger>
                              <SelectContent>
                                {config.customDictionary && config.customDictionary.length > 0 ? (
                                  <SelectItem value="recommend">
                                    推荐词库（{config.customDictionary.length} 词）
                                  </SelectItem>
                                ) : null}
                                {dictionaries.map((dict) => (
                                  <SelectItem key={dict.dictId} value={String(dict.dictId)}>
                                    {dict.name}（{dict.wordCount} 词）
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                        ) : null}

                        {systemDictionaries.length > 0 ? (
                          <div className="flex flex-wrap items-center gap-4">
                            <span className="label-mono text-muted-foreground">系统库</span>
                            <Select
                              value={selectedSystemDictId || 'none'}
                              onValueChange={(value) => setSelectedSystemDictId(value === 'none' ? '' : value)}
                            >
                              <SelectTrigger id="system-dictionary-select" className="h-9 w-[260px]">
                                <SelectValue placeholder="不使用系统词库" />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="none">不使用系统词库</SelectItem>
                                {systemDictionaries.map((dict) => (
                                  <SelectItem key={dict.dictId} value={String(dict.dictId)}>
                                    {dict.name}（{dict.wordCount} 词）
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                        ) : null}

                        <Link to="/apps/typing/dictionary" className="inline-block">
                          <Button variant="ghost" size="sm">
                            管理词库 →
                          </Button>
                        </Link>
                      </div>
                    ) : null}
                  </div>
                );
              })}
            </RadioGroup>
          </section>

          {/* 02 字符 */}
          <section>
            <div className="flex items-baseline justify-between border-b border-border pb-2">
              <span className="label-mono">02 · 字符 / GLYPHS</span>
              <span className="label-mono text-muted-foreground">已选 {glyphCount} / {GLYPH_OPTIONS.length}</span>
            </div>

            <div>
              {GLYPH_OPTIONS.map((glyph) => (
                <div
                  key={glyph.key}
                  className="flex items-center justify-between gap-4 border-b border-border py-3"
                >
                  <div className="flex min-w-0 items-baseline gap-4">
                    <Label htmlFor={glyph.key} className="cursor-pointer text-[13px]">
                      {glyph.label}
                    </Label>
                    <span className="mono-num truncate text-[11px] text-muted-foreground">{glyph.sample}</span>
                  </div>
                  <Switch
                    id={glyph.key}
                    checked={config[glyph.key]}
                    onCheckedChange={(checked) => {
                      const next = { ...config, [glyph.key]: checked };
                      updateConfig({ [glyph.key]: checked });
                      void persistCharTypes(next);
                    }}
                  />
                </div>
              ))}
            </div>
            <p className="mt-3 text-[12px] leading-5 text-muted-foreground">
              每局结算都会记录每个键的失误，下一局的字序列由这些记录生成——关掉某类字符，它就不会出现在序列里。
            </p>
          </section>
        </div>

        {/* 右侧摘要 */}
        <aside className="lg:sticky lg:top-24 lg:self-start">
          <div className="border border-border bg-card">
            <div className="flex items-center justify-between border-b border-border px-5 py-3">
              <span className="label-mono">摘要 / SUMMARY</span>
              <span className="label-mono text-muted-foreground">{currentMode.en}</span>
            </div>

            <dl className="divide-y divide-border">
              <div className="px-5 py-4">
                <dt className="label-mono text-muted-foreground">模式</dt>
                <dd className="mt-1.5 font-serif text-xl leading-none">{currentMode.title}</dd>
              </div>
              <div className="px-5 py-4">
                <dt className="label-mono text-muted-foreground">字符</dt>
                <dd className="mt-2 flex flex-wrap gap-1.5">
                  {GLYPH_OPTIONS.filter((g) => config[g.key]).map((g) => (
                    <span key={g.key} className="border border-border px-1.5 py-0.5 text-[11px]">
                      {g.label}
                    </span>
                  ))}
                  {glyphCount === 0 ? <span className="text-[13px] text-accent">尚未选择</span> : null}
                </dd>
              </div>
              <div className="px-5 py-4">
                <dt className="label-mono text-muted-foreground">
                  {config.gameMode === 'dictionary' ? '词库' : '时长'}
                </dt>
                <dd className="mt-1.5 text-[13px]">
                  {config.gameMode === 'dictionary'
                    ? selectedDictName
                    : config.gameMode === 'limit'
                      ? `${config.timeLimitSeconds || 60} 秒`
                      : '不限时，直到本局结算'}
                </dd>
              </div>
              <div className="px-5 py-4">
                <dt className="label-mono text-muted-foreground">难度</dt>
                <dd className="mt-1.5 text-[13px] text-muted-foreground">
                  由模型每轮给出，不在这里设定
                </dd>
              </div>
            </dl>

            <div className="border-t border-border p-5">
              <Button size="lg" className="w-full justify-between" onClick={handleStartGame}>
                <span>开始游戏</span>
                <span aria-hidden>→</span>
              </Button>
              <p className="mt-3 text-[11px] leading-5 text-muted-foreground">
                输入首字母即自动锁定目标，Tab 换目标，Shift 切中英文。
              </p>
            </div>
          </div>
        </aside>
      </div>
    </PageBody>
  );
}
