// 怪物配置管理组件
import { useState, useEffect } from 'react';
import { getMonsterConfigs, updateMonsterConfig } from '@/db/api';
import type { MonsterConfig } from '@/types/database';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { Save, Loader2 } from 'lucide-react';

const monsterTypeNames: Record<string, string> = {
  NORMAL: '普通型',
  ATTACKER: '攻击型',
  EVASIVE: '回避型',
  SMART: '智慧型',
};

export default function MonsterConfigManagement() {
  const [configs, setConfigs] = useState<MonsterConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editedScores, setEditedScores] = useState<Record<string, number>>({});
  const { toast } = useToast();

  useEffect(() => {
    loadConfigs();
  }, []);

  async function loadConfigs() {
    setLoading(true);
    try {
      const data = await getMonsterConfigs();
      setConfigs(data);
      const scores: Record<string, number> = {};
      data.forEach(config => {
        scores[config.type] = config.score;
      });
      setEditedScores(scores);
    } catch (error) {
      toast({
        title: '加载失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  }

  async function handleSave() {
    setSaving(true);
    try {
      const updates = configs.map(config => {
        const newScore = editedScores[config.type];
        if (newScore !== config.score) {
          return updateMonsterConfig(config.type, { score: newScore });
        }
        return Promise.resolve(config);
      });

      await Promise.all(updates);

      toast({
        title: '保存成功',
        description: '怪物积分配置已更新',
      });
      loadConfigs();
    } catch (error) {
      toast({
        title: '保存失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
    } finally {
      setSaving(false);
    }
  }

  function handleScoreChange(type: string, value: string) {
    const score = parseInt(value) || 0;
    setEditedScores(prev => ({
      ...prev,
      [type]: score,
    }));
  }

  const hasChanges = configs.some(config => editedScores[config.type] !== config.score);

  return (
    <Card>
      <CardHeader>
        <CardTitle>怪物配置管理</CardTitle>
        <CardDescription>
          调整不同类型怪物的积分值，影响游戏得分计算
        </CardDescription>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="flex justify-center py-8">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        ) : (
          <div className="space-y-6">
            <div className="grid gap-6">
              {configs.map((config) => (
                <div
                  key={config.type}
                  className="p-4 border rounded-lg space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="font-medium text-lg">
                        {monsterTypeNames[config.type] || config.type}
                      </h3>
                      <p className="text-sm text-muted-foreground mt-1">
                        {config.description}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <Label htmlFor={`score-${config.type}`} className="text-sm">
                        积分值
                      </Label>
                      <Input
                        id={`score-${config.type}`}
                        type="number"
                        min="0"
                        max="100"
                        value={editedScores[config.type] || 0}
                        onChange={(e) => handleScoreChange(config.type, e.target.value)}
                        className="w-24"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={loadConfigs}
                disabled={!hasChanges || saving}
              >
                重置
              </Button>
              <Button
                onClick={handleSave}
                disabled={!hasChanges || saving}
              >
                {saving ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    保存中...
                  </>
                ) : (
                  <>
                    <Save className="w-4 h-4 mr-2" />
                    保存更改
                  </>
                )}
              </Button>
            </div>

            <div className="p-4 bg-muted rounded-lg">
              <h4 className="font-medium mb-2">💡 提示</h4>
              <ul className="text-sm text-muted-foreground space-y-1">
                <li>• 积分值越高，消灭该类型怪物获得的分数越多</li>
                <li>• 建议根据怪物难度设置合理的积分值</li>
                <li>• 修改后立即生效，影响所有新游戏</li>
              </ul>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
