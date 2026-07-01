// 设置页面
import { useGameConfig } from '@/contexts/GameConfigContext';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Button } from '@/components/ui/button';
import { Volume2, VolumeX } from 'lucide-react';

export default function SettingsPage() {
  const { config, updateConfig } = useGameConfig();

  return (
    <div className="container max-w-2xl py-8">
      <div className="space-y-6">
        <div className="text-center space-y-2">
          <h1 className="text-3xl font-bold">设置</h1>
          <p className="text-muted-foreground">配置游戏选项</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>音效设置</CardTitle>
            <CardDescription>控制游戏音效开关</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                {config.soundEnabled ? (
                  <Volume2 className="w-5 h-5 text-primary" />
                ) : (
                  <VolumeX className="w-5 h-5 text-muted-foreground" />
                )}
                <Label htmlFor="sound" className="cursor-pointer">
                  游戏音效
                </Label>
              </div>
              <Switch
                id="sound"
                checked={config.soundEnabled}
                onCheckedChange={(checked) => updateConfig({ soundEnabled: checked })}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>帮助中心</CardTitle>
            <CardDescription>游戏操作说明</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4 text-sm">
            <div>
              <h3 className="font-medium mb-2">基本操作</h3>
              <ul className="space-y-1 text-muted-foreground">
                <li>• 直接输入字符攻击怪物，首次输入自动锁定目标</li>
                <li>• 按 Tab 键取消当前锁定，重新选择目标</li>
                <li>• 按 Shift 键切换中英文输入模式</li>
                <li>• 按 Caps Lock 切换大小写</li>
              </ul>
            </div>

            <div>
              <h3 className="font-medium mb-2">怪物类型</h3>
              <ul className="space-y-1 text-muted-foreground">
                <li>• <strong>普通型</strong>：匀速前进，1积分</li>
                <li>• <strong>攻击型</strong>：定期生成新怪物，3积分</li>
                <li>• <strong>回避型</strong>：30%概率规避攻击，2积分</li>
                <li>• <strong>智慧型</strong>：吸引周围怪物聚拢，3积分</li>
              </ul>
            </div>

            <div>
              <h3 className="font-medium mb-2">游戏机制</h3>
              <ul className="space-y-1 text-muted-foreground">
                <li>• 准确率≥85%时，速度+0.2，怪物数量增加</li>
                <li>• 准确率≤40%时，速度-0.3，怪物数量减少</li>
                <li>• 系统会记录你的易错字符，优先生成针对性组合</li>
                <li>• 怪物会通过遗传算法进化，变得越来越难对付</li>
              </ul>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
