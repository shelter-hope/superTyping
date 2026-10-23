// 准备页面 - 游戏配置
import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { useGameConfig } from '@/contexts/GameConfigContext';
import { getUserDictionaries, getSystemDictionaries, getGameRecommend, getGameCharTypes, updateGameCharTypes } from '@/db/api';
import type { CustomDictionary, SystemDictionary } from '@/types/database';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
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
import { Play, FileText, ExternalLink, Sparkles } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { selectedCharsLabel } from '@/features/game/utils/gameRuntime';

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
    navigate('/game');
  }

  return (
    <div className="container max-w-4xl py-8">
      <div className="space-y-6">
        <div className="text-center space-y-2">
          <h1 className="text-3xl font-bold gradient-text">游戏配置</h1>
          <p className="text-muted-foreground">选择游戏模式和字符类型</p>
          <div className="flex flex-wrap items-center justify-center gap-2 pt-2">
            <GameTutorial />
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleRecommend}
              disabled={recommendLoading}
            >
              {recommendLoading ? (
                <>加载中…</>
              ) : (
                <>
                  <Sparkles className="w-4 h-4 mr-2" />
                  根据我的数据推荐配置
                </>
              )}
            </Button>
          </div>
        </div>

        {/* 游戏模式选择 */}
        <Card>
          <CardHeader>
            <CardTitle>游戏模式</CardTitle>
            <CardDescription>选择你想要的游戏模式</CardDescription>
          </CardHeader>
          <CardContent>
            <RadioGroup
              value={config.gameMode}
              onValueChange={(value) => {
                const mode = value as 'endless' | 'dictionary' | 'limit';
                updateConfig({ gameMode: mode });
                // 仅在模式切换时请求一次对应模式的字符类型配置
                void loadCharTypesForMode(mode);
              }}
            >
              <div className="flex items-center space-x-2 p-3 rounded-lg hover:bg-muted/50">
                <RadioGroupItem value="endless" id="endless" />
                <Label htmlFor="endless" className="cursor-pointer flex-1">
                  <div>
                    <div className="font-medium">🔄 无尽适应模式</div>
                    <div className="text-sm text-muted-foreground">
                      系统自动分析你的弱点，生成针对性字符组合，难度自适应调整
                    </div>
                  </div>
                </Label>
              </div>
              
              <div className="flex items-center space-x-2 p-3 rounded-lg hover:bg-muted/50">
                <RadioGroupItem value="limit" id="limit" />
                <Label htmlFor="limit" className="cursor-pointer flex-1">
                  <div>
                    <div className="font-medium">⏱️ 限时冲刺模式</div>
                    <div className="text-sm text-muted-foreground">
                      在固定时间内尽可能获得高分，适合快速练习
                    </div>
                  </div>
                </Label>
              </div>
              
              <div className="flex items-center space-x-2 p-3 rounded-lg hover:bg-muted/50">
                <RadioGroupItem value="dictionary" id="dictionary" />
                <Label htmlFor="dictionary" className="cursor-pointer flex-1">
                  <div>
                    <div className="font-medium">📚 词源界定模式</div>
                    <div className="text-sm text-muted-foreground">
                      使用自定义词库进行训练，适合专业术语练习
                    </div>
                  </div>
                </Label>
              </div>
            </RadioGroup>

            {/* 限时模式配置 */}
            {config.gameMode === 'limit' && (
              <div className="mt-4 space-y-2">
                <Label htmlFor="time-limit">时间限制（秒）</Label>
                <Select 
                  value={String(config.timeLimitSeconds || 60)} 
                  onValueChange={(value) => updateConfig({ timeLimitSeconds: Number(value) })}
                >
                  <SelectTrigger id="time-limit">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="30">30秒 - 快速挑战</SelectItem>
                    <SelectItem value="60">60秒 - 标准模式</SelectItem>
                    <SelectItem value="120">120秒 - 长时间挑战</SelectItem>
                    <SelectItem value="180">180秒 - 马拉松模式</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            )}

            {/* 词库选择 */}
            {config.gameMode === 'dictionary' && (
              <div className="mt-4 space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="dictionary-select">选择词库</Label>
                  {selectedDictId === 'recommend' && config.customDictionary && config.customDictionary.length > 0 && (
                    <p className="text-sm text-primary">已应用推荐词库（{config.customDictionary.length} 个词），可直接开始游戏。</p>
                  )}
                  {dictionaries.length === 0 && selectedDictId !== 'recommend' && (
                    <div className="p-4 border rounded-lg bg-muted/30">
                      <div className="flex items-center gap-2 text-sm text-muted-foreground mb-2">
                        <FileText className="w-4 h-4" />
                        <span>还没有自定义词库{systemDictionaries.length > 0 ? '，可以改用下面的系统词库' : ''}</span>
                      </div>
                      <Link to="/dictionary">
                        <Button variant="outline" size="sm">
                          <ExternalLink className="w-4 h-4 mr-2" />
                          前往上传词库
                        </Button>
                      </Link>
                    </div>
                  )}
                  {(dictionaries.length > 0 || selectedDictId === 'recommend') && (
                    <Select value={selectedDictId} onValueChange={setSelectedDictId}>
                      <SelectTrigger id="dictionary-select">
                        <SelectValue placeholder="请选择一个词库" />
                      </SelectTrigger>
                      <SelectContent>
                        {config.customDictionary && config.customDictionary.length > 0 && (
                          <SelectItem value="recommend">
                            推荐词库（{config.customDictionary.length} 词）
                          </SelectItem>
                        )}
                        {dictionaries.map((dict) => (
                          <SelectItem key={dict.dictId} value={String(dict.dictId)}>
                            {dict.name} ({dict.wordCount}个词语)
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                </div>
                {systemDictionaries.length > 0 && (
                  <div className="space-y-2">
                    <Label htmlFor="system-dictionary-select">系统词库（可选）</Label>
                    <Select value={selectedSystemDictId || 'none'} onValueChange={(value) => setSelectedSystemDictId(value === 'none' ? '' : value)}>
                      <SelectTrigger id="system-dictionary-select">
                        <SelectValue placeholder="不使用系统词库" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="none">不使用系统词库</SelectItem>
                        {systemDictionaries.map((dict) => (
                          <SelectItem key={dict.dictId} value={String(dict.dictId)}>
                            {dict.name} ({dict.wordCount}个词语)
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                )}
                <div className="flex items-center gap-2">
                  <Link to="/dictionary">
                    <Button variant="outline" size="sm">
                      <FileText className="w-4 h-4 mr-2" />
                      管理词库
                    </Button>
                  </Link>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* 字符类型选择 */}
        <Card>
          <CardHeader>
            <CardTitle>字符类型</CardTitle>
            <CardDescription>选择游戏中包含的字符类型</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <Label htmlFor="lowercase" className="cursor-pointer">小写字母 (a-z)</Label>
              <Switch
                id="lowercase"
                checked={config.enableLowercase}
                onCheckedChange={(checked) => {
                  const next = { ...config, enableLowercase: checked };
                  updateConfig({ enableLowercase: checked });
                  void persistCharTypes(next);
                }}
              />
            </div>
            <div className="flex items-center justify-between">
              <Label htmlFor="uppercase" className="cursor-pointer">大写字母 (A-Z)</Label>
              <Switch
                id="uppercase"
                checked={config.enableUppercase}
                onCheckedChange={(checked) => {
                  const next = { ...config, enableUppercase: checked };
                  updateConfig({ enableUppercase: checked });
                  void persistCharTypes(next);
                }}
              />
            </div>
            <div className="flex items-center justify-between">
              <Label htmlFor="numbers" className="cursor-pointer">数字 (0-9)</Label>
              <Switch
                id="numbers"
                checked={config.enableNumbers}
                onCheckedChange={(checked) => {
                  const next = { ...config, enableNumbers: checked };
                  updateConfig({ enableNumbers: checked });
                  void persistCharTypes(next);
                }}
              />
            </div>
            <div className="flex items-center justify-between">
              <Label htmlFor="punctuation" className="cursor-pointer">标点符号 (,.!?;:'")</Label>
              <Switch
                id="punctuation"
                checked={config.enablePunctuation}
                onCheckedChange={(checked) => {
                  const next = { ...config, enablePunctuation: checked };
                  updateConfig({ enablePunctuation: checked });
                  void persistCharTypes(next);
                }}
              />
            </div>
            <div className="flex items-center justify-between">
              <Label htmlFor="operators" className="cursor-pointer">运算符号 (+-*/=&lt;&gt;)</Label>
              <Switch
                id="operators"
                checked={config.enableOperators}
                onCheckedChange={(checked) => {
                  const next = { ...config, enableOperators: checked };
                  updateConfig({ enableOperators: checked });
                  void persistCharTypes(next);
                }}
              />
            </div>
            <div className="flex items-center justify-between">
              <Label htmlFor="specialChars" className="cursor-pointer">特殊字符 (@#$%^&*)</Label>
              <Switch
                id="specialChars"
                checked={config.enableSpecialChars}
                onCheckedChange={(checked) => {
                  const next = { ...config, enableSpecialChars: checked };
                  updateConfig({ enableSpecialChars: checked });
                  void persistCharTypes(next);
                }}
              />
            </div>
            <div className="flex items-center justify-between">
              <Label htmlFor="chinese" className="cursor-pointer">中文字符</Label>
              <Switch
                id="chinese"
                checked={config.enableChinese}
                onCheckedChange={(checked) => {
                  const next = { ...config, enableChinese: checked };
                  updateConfig({ enableChinese: checked });
                  void persistCharTypes(next);
                }}
              />
            </div>
          </CardContent>
        </Card>

        {/* 开始游戏按钮 */}
        <div className="flex justify-center pt-4">
          <Button size="lg" onClick={handleStartGame}>
            <Play className="mr-2 h-5 w-5" />
            开始游戏
          </Button>
        </div>
      </div>
    </div>
  );
}
