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

const MONSTERS = [
  { name: '普通型', tint: 'text-muted-foreground', desc: '匀速下落，最容易处理的目标', score: '1 分' },
  { name: '攻击型', tint: 'text-accent', desc: '定期生成新怪物，放着不管会失控', score: '3 分' },
  { name: '回避型', tint: 'text-chart-3', desc: '30% 概率规避攻击，要补第二刀', score: '2 分' },
  { name: '智慧型', tint: 'text-chart-4', desc: '吸引周围怪物聚拢，扎堆下来', score: '3 分' },
];

const KEYS = [
  { key: 'A-Z', text: '敲击首字符即锁定目标，逐字符推进' },
  { key: 'Tab', text: '放弃当前锁定，重新选最近的' },
  { key: 'Shift', text: '切换中英文输入模式' },
  { key: 'Caps', text: '切换大小写' },
];

export default function GameTutorial() {
  const [open, setOpen] = useState(false);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <HelpCircle className="mr-2 h-3.5 w-3.5" strokeWidth={1.6} />
          看一遍规则
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[80vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>这一局怎么打</DialogTitle>
          <DialogDescription>四条规则，看完就能上桌。</DialogDescription>
        </DialogHeader>

        <div className="space-y-8">
          <section>
            <div className="border-b border-border pb-2">
              <span className="label-mono text-muted-foreground">01 · 目标</span>
            </div>
            <p className="mt-3 text-[13px] leading-6 text-muted-foreground">
              从上往下落的词块就是目标。逐字符敲完它，它会被打掉；漏掉的词落到最底部，这一局就结束。
              词从哪来、掉多快，由系统根据你上一局的表现决定。
            </p>
          </section>

          <section>
            <div className="border-b border-border pb-2">
              <span className="label-mono text-muted-foreground">02 · 操作</span>
            </div>
            <dl className="mt-1 divide-y divide-border">
              {KEYS.map((row) => (
                <div key={row.key} className="flex items-baseline gap-4 py-2.5">
                  <dt className="w-14 shrink-0">
                    <span className="keycap">{row.key}</span>
                  </dt>
                  <dd className="text-[13px] text-muted-foreground">{row.text}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section>
            <div className="border-b border-border pb-2">
              <span className="label-mono text-muted-foreground">03 · 四种目标</span>
            </div>
            <div className="mt-1 divide-y divide-border">
              {MONSTERS.map((m) => (
                <div key={m.name} className="flex flex-wrap items-baseline gap-x-3 gap-y-1 py-2.5">
                  <span className="font-serif text-base">{m.name}</span>
                  <span className="flex-1 text-[13px] text-muted-foreground">{m.desc}</span>
                  <span className={`mono-num text-[11px] ${m.tint}`}>{m.score}</span>
                </div>
              ))}
            </div>
          </section>

          <section>
            <div className="border-b border-border pb-2">
              <span className="label-mono text-muted-foreground">04 · 难度</span>
            </div>
            <p className="mt-3 text-[13px] leading-6 text-muted-foreground">
              难度 <span className="mono-num text-foreground">D</span> 只有一个目标：把你的准确率稳定在{' '}
              <span className="mono-num text-foreground">85%</span> 附近。每局结算走一小步，单步上限{' '}
              <span className="mono-num text-foreground">0.05</span>，永远待在{' '}
              <span className="mono-num text-foreground">[0.15, 0.95]</span> 之间——不会突然把你甩下车，也不会让你停在舒适区。
            </p>
            <p className="mt-2 mono-num text-[11px] text-muted-foreground">
              D_t = clip(D_(t-1) + k·(A − A*)，±δ)
            </p>
          </section>

          <section>
            <div className="border-b border-border pb-2">
              <span className="label-mono text-muted-foreground">05 · 提醒</span>
            </div>
            <ul className="mt-3 space-y-1.5 text-[13px] text-muted-foreground">
              <li>· 先打攻击型，怪物数量失控通常是它造成的。</li>
              <li>· 准确率比速度值钱：错一次的字，下一局还会回来。</li>
              <li>· 想练哪类字，在配置页把字符类型勾选收窄即可。</li>
            </ul>
          </section>
        </div>

        <div className="flex justify-end border-t border-border pt-4">
          <Button onClick={() => setOpen(false)}>知道了</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
