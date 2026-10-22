// 游戏教程组件
import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { HelpCircle } from 'lucide-react';

export default function GameTutorial() {
  const [open, setOpen] = useState(false);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <HelpCircle className="mr-2 h-4 w-4" />
          游戏教程
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>游戏教程</DialogTitle>
          <DialogDescription>
            学习如何玩打字进化游戏
          </DialogDescription>
        </DialogHeader>
        
        <div className="space-y-6">
          <div>
            <h3 className="text-lg font-semibold mb-2">🎯 游戏目标</h3>
            <p className="text-muted-foreground">
              通过打字消灭从屏幕顶部下落的字符怪物，防止它们接触到底部的玩家。
              游戏会根据你的表现自动调整难度，并针对你的弱点生成怪物。
            </p>
          </div>

          <div>
            <h3 className="text-lg font-semibold mb-2">⌨️ 基本操作</h3>
            <ul className="space-y-2 text-muted-foreground">
              <li>• <strong>输入字符</strong>：直接敲击键盘上的字符来攻击怪物</li>
              <li>• <strong>自动锁定</strong>：首次输入正确字符会自动锁定该怪物</li>
              <li>• <strong>Tab键</strong>：取消当前锁定，重新选择目标</li>
              <li>• <strong>Esc键</strong>：暂停游戏</li>
            </ul>
          </div>

          <div>
            <h3 className="text-lg font-semibold mb-2">👾 怪物类型</h3>
            <div className="space-y-2">
              <div className="flex items-start gap-2">
                <span className="text-primary font-bold">普通型</span>
                <span className="text-muted-foreground">- 匀速下落，1积分</span>
              </div>
              <div className="flex items-start gap-2">
                <span className="text-destructive font-bold">攻击型</span>
                <span className="text-muted-foreground">- 定期生成新怪物，3积分</span>
              </div>
              <div className="flex items-start gap-2">
                <span className="font-bold text-accent">回避型</span>
                <span className="text-muted-foreground">- 30%概率规避攻击，2积分</span>
              </div>
              <div className="flex items-start gap-2">
                <span className="text-secondary font-bold">智慧型</span>
                <span className="text-muted-foreground">- 吸引周围怪物聚拢，3积分</span>
              </div>
            </div>
          </div>

          <div>
            <h3 className="text-lg font-semibold mb-2">🧬 自适应机制</h3>
            <ul className="space-y-2 text-muted-foreground">
              <li>• <strong>准确率≥85%</strong>：速度+0.2，怪物数量增加</li>
              <li>• <strong>准确率≤40%</strong>：速度-0.3，怪物数量减少</li>
              <li>• <strong>遗传算法</strong>：系统会记录你的易错字符，优先生成针对性组合</li>
              <li>• <strong>进化系统</strong>：表现好的怪物会在下一轮"繁殖"出更强的后代</li>
            </ul>
          </div>

          <div>
            <h3 className="text-lg font-semibold mb-2">💡 游戏技巧</h3>
            <ul className="space-y-2 text-muted-foreground">
              <li>• 优先消灭<span className="text-destructive font-bold">攻击型</span>怪物，防止怪物数量失控</li>
              <li>• 注意<span className="font-bold text-accent">回避型</span>怪物可能会躲避你的攻击</li>
              <li>• 使用Tab键灵活切换目标，优先处理接近底部的怪物</li>
              <li>• 保持高准确率比追求速度更重要</li>
              <li>• 定期查看个人数据，了解自己的弱点字符</li>
            </ul>
          </div>

          <div>
            <h3 className="text-lg font-semibold mb-2">🎮 游戏模式</h3>
            <div className="space-y-2 text-muted-foreground">
              <p><strong>无尽适应模式</strong>：系统自动分析你的打字数据，生成针对性的字符组合</p>
              <p><strong>词源界定模式</strong>：上传自定义词库，在特定词汇范围内进行训练</p>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-4">
          <Button onClick={() => setOpen(false)}>
            开始游戏
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
