/**
 * 字符生成配置组件
 * 提供可视化的字符生成规则配置界面
 */

import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Slider } from '@/components/ui/slider';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import type { CharacterGenerationConfig, CharSourceType, FrequencyMode } from '@/types/database';

interface CharacterGenerationConfigEditorProps {
  config: Partial<CharacterGenerationConfig>;
  onChange: (config: Partial<CharacterGenerationConfig>) => void;
}

export function CharacterGenerationConfigEditor({
  config,
  onChange,
}: CharacterGenerationConfigEditorProps) {
  const [selectedCharTypes, setSelectedCharTypes] = useState<string[]>(
    config.char_types || ['lowercase']
  );

  const handleCharTypeToggle = (type: string) => {
    const newTypes = selectedCharTypes.includes(type)
      ? selectedCharTypes.filter(t => t !== type)
      : [...selectedCharTypes, type];
    setSelectedCharTypes(newTypes);
    onChange({ ...config, char_types: newTypes });
  };

  return (
    <div className="space-y-6">
      {/* 基本信息 */}
      <Card>
        <CardHeader>
          <CardTitle>基本信息</CardTitle>
          <CardDescription>配置名称和描述</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">配置名称 *</Label>
            <Input
              id="name"
              value={config.name || ''}
              onChange={e => onChange({ ...config, name: e.target.value })}
              placeholder="例如：默认配置"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="description">配置描述</Label>
            <Textarea
              id="description"
              value={config.description || ''}
              onChange={e => onChange({ ...config, description: e.target.value })}
              placeholder="简要描述此配置的用途"
              rows={3}
            />
          </div>
        </CardContent>
      </Card>

      {/* 字符来源配置 */}
      <Card>
        <CardHeader>
          <CardTitle>字符来源</CardTitle>
          <CardDescription>设置字符的生成来源</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="source-type">来源类型 *</Label>
            <Select
              value={config.source_type || 'fixed_set'}
              onValueChange={(value: CharSourceType) =>
                onChange({ ...config, source_type: value })
              }
            >
              <SelectTrigger id="source-type">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="fixed_set">固定字符集</SelectItem>
                <SelectItem value="dynamic">动态生成</SelectItem>
                <SelectItem value="dictionary">词库</SelectItem>
                <SelectItem value="mixed">混合模式</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              {config.source_type === 'fixed_set' && '从预定义的字符集中选择'}
              {config.source_type === 'dynamic' && '根据规则动态生成字符'}
              {config.source_type === 'dictionary' && '从系统词库中选择'}
              {config.source_type === 'mixed' && '混合使用多种来源'}
            </p>
          </div>

          {(config.source_type === 'fixed_set' || config.source_type === 'mixed') && (
            <div className="space-y-2">
              <Label htmlFor="fixed-chars">固定字符集</Label>
              <Textarea
                id="fixed-chars"
                value={config.fixed_characters || ''}
                onChange={e => onChange({ ...config, fixed_characters: e.target.value })}
                placeholder="输入字符，用逗号分隔，例如：a,b,c,1,2,3"
                rows={3}
              />
              <p className="text-xs text-muted-foreground">
                支持单个字符或字符串，用逗号分隔
              </p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 字符属性配置 */}
      <Card>
        <CardHeader>
          <CardTitle>字符属性</CardTitle>
          <CardDescription>设置字符的长度和类型</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="min-length">最小长度 *</Label>
              <Input
                id="min-length"
                type="number"
                min={1}
                max={20}
                value={config.min_length || 1}
                onChange={e =>
                  onChange({ ...config, min_length: parseInt(e.target.value) })
                }
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="max-length">最大长度 *</Label>
              <Input
                id="max-length"
                type="number"
                min={1}
                max={20}
                value={config.max_length || 15}
                onChange={e =>
                  onChange({ ...config, max_length: parseInt(e.target.value) })
                }
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label>允许的字符类型 *</Label>
            <div className="flex flex-wrap gap-2">
              {[
                { value: 'lowercase', label: '小写字母' },
                { value: 'uppercase', label: '大写字母' },
                { value: 'number', label: '数字' },
                { value: 'punctuation', label: '标点符号' },
                { value: 'special', label: '特殊字符' },
                { value: 'chinese', label: '中文字符' },
              ].map(type => (
                <Badge
                  key={type.value}
                  variant={selectedCharTypes.includes(type.value) ? 'default' : 'outline'}
                  className="cursor-pointer"
                  onClick={() => handleCharTypeToggle(type.value)}
                >
                  {type.label}
                </Badge>
              ))}
            </div>
            <p className="text-xs text-muted-foreground">
              点击选择或取消选择字符类型
            </p>
          </div>
        </CardContent>
      </Card>

      {/* 频率和顺序配置 */}
      <Card>
        <CardHeader>
          <CardTitle>频率和顺序</CardTitle>
          <CardDescription>设置字符出现的频率和顺序规则</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="frequency-mode">频率模式 *</Label>
            <Select
              value={config.frequency_mode || 'random'}
              onValueChange={(value: FrequencyMode) =>
                onChange({ ...config, frequency_mode: value })
              }
            >
              <SelectTrigger id="frequency-mode">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="random">随机</SelectItem>
                <SelectItem value="weighted">加权</SelectItem>
                <SelectItem value="sequential">顺序</SelectItem>
                <SelectItem value="adaptive">自适应</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              {config.frequency_mode === 'random' && '完全随机选择字符'}
              {config.frequency_mode === 'weighted' && '根据权重选择字符'}
              {config.frequency_mode === 'sequential' && '按顺序依次选择字符'}
              {config.frequency_mode === 'adaptive' && '根据玩家表现自适应调整'}
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="randomness">随机性程度</Label>
            <div className="flex items-center gap-4">
              <Slider
                id="randomness"
                min={0}
                max={100}
                step={1}
                value={[(config.randomness_level || 0.5) * 100]}
                onValueChange={([value]) =>
                  onChange({ ...config, randomness_level: value / 100 })
                }
                className="flex-1"
              />
              <span className="text-sm font-medium w-12 text-right">
                {Math.round((config.randomness_level || 0.5) * 100)}%
              </span>
            </div>
            <p className="text-xs text-muted-foreground">
              0% = 完全确定性，100% = 完全随机
            </p>
          </div>
        </CardContent>
      </Card>

      {/* 状态配置 */}
      <Card>
        <CardHeader>
          <CardTitle>状态设置</CardTitle>
          <CardDescription>配置的启用状态</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label htmlFor="is-active">启用此配置</Label>
              <p className="text-xs text-muted-foreground">
                启用后此配置将可以在游戏中使用
              </p>
            </div>
            <Switch
              id="is-active"
              checked={config.is_active !== false}
              onCheckedChange={checked => onChange({ ...config, is_active: checked })}
            />
          </div>

          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label htmlFor="is-default">设为默认配置</Label>
              <p className="text-xs text-muted-foreground">
                新游戏将默认使用此配置
              </p>
            </div>
            <Switch
              id="is-default"
              checked={config.is_default === true}
              onCheckedChange={checked => onChange({ ...config, is_default: checked })}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
