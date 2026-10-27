// 设置页面：纸与墨（分组行）
import { useState, type ReactNode } from 'react';
import { useGameConfig } from '@/contexts/GameConfigContext';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
import { Volume2, VolumeX } from 'lucide-react';
import { changePassword } from '@/services/auth';
import { updateSoundSettings } from '@/services/user';
import { useToast } from '@/hooks/use-toast';

/** 分组报头：衬线小标题 + 右侧等宽注脚；首组不画上发丝线（页头已有） */
function GroupHeading({ title, note, first = false }: { title: string; note: string; first?: boolean }) {
  return (
    <div className={`${first ? '' : 'border-t border-border pt-5'} flex items-baseline justify-between gap-4`}>
      <h2 className="font-serif text-[17px] leading-none">{title}</h2>
      <span className="label-mono text-[9px]">{note}</span>
    </div>
  );
}

const helpGroups: { title: string; items: ReactNode[] }[] = [
  {
    title: '基本操作',
    items: [
      '直接输入字符攻击怪物，首次输入自动锁定目标',
      '按 Tab 键取消当前锁定，重新选择目标',
      '按 Shift 键切换中英文输入模式',
      '按 Caps Lock 切换大小写',
    ],
  },
  {
    title: '怪物类型',
    items: [
      <>
        <strong className="font-medium text-foreground">普通型</strong>：匀速前进，1积分
      </>,
      <>
        <strong className="font-medium text-foreground">攻击型</strong>：定期生成新怪物，3积分
      </>,
      <>
        <strong className="font-medium text-foreground">回避型</strong>：30%概率规避攻击，2积分
      </>,
      <>
        <strong className="font-medium text-foreground">智慧型</strong>：吸引周围怪物聚拢，3积分
      </>,
    ],
  },
  {
    title: '游戏机制',
    items: [
      '准确率≥85%时，速度+0.2，怪物数量增加',
      '准确率≤40%时，速度-0.3，怪物数量减少',
      '系统会记录你的易错字符，优先生成针对性组合',
      '怪物会通过遗传算法进化，变得越来越难对付',
    ],
  },
];

export default function SettingsPage() {
  const { config, updateConfig } = useGameConfig();
  const { toast } = useToast();
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [savingPassword, setSavingPassword] = useState(false);

  async function handleSoundChange(checked: boolean) {
    updateConfig({ soundEnabled: checked });
    try {
      await updateSoundSettings({ enabled: checked });
    } catch (error) {
      updateConfig({ soundEnabled: !checked });
      toast({
        title: '音效设置未保存',
        description: error instanceof Error ? error.message : '请稍后重试',
        variant: 'destructive',
      });
    }
  }

  async function handleChangePassword(event: React.FormEvent) {
    event.preventDefault();
    if (newPassword.length < 6) {
      toast({ title: '密码太短', description: '新密码至少需要 6 个字符', variant: 'destructive' });
      return;
    }
    if (newPassword !== confirmPassword) {
      toast({ title: '密码不匹配', description: '两次输入的新密码不一致', variant: 'destructive' });
      return;
    }
    setSavingPassword(true);
    try {
      await changePassword(oldPassword, newPassword, confirmPassword);
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      toast({ title: '密码已更新', description: '下次登录请使用新密码' });
    } catch (error) {
      toast({
        title: '修改失败',
        description: error instanceof Error ? error.message : '请检查当前密码',
        variant: 'destructive',
      });
    } finally {
      setSavingPassword(false);
    }
  }

  return (
    <PageBody>
      <div className="max-w-3xl">
        <PageHeader
          eyebrow="设置 · SETTINGS"
          title="设置"
          description="配置游戏选项：音效开关、账户密码与操作说明。"
        />

        <div className="mt-8 space-y-10">
          {/* 音效 */}
          <section>
            <GroupHeading title="音效" note="sound" first />

            <div className="mt-2 divide-y divide-border border-y border-border">
              <div className="flex items-center justify-between gap-6 py-4">
                <div className="min-w-0">
                  <Label htmlFor="sound" className="flex cursor-pointer items-center gap-2 text-[13px]">
                    {config.soundEnabled ? (
                      <Volume2 className="h-4 w-4 text-accent" />
                    ) : (
                      <VolumeX className="h-4 w-4 text-muted-foreground" />
                    )}
                    游戏音效
                  </Label>
                  <p className="mt-1 text-[12px] leading-[1.7] text-muted-foreground">
                    控制击键、命中与结算提示音；关闭后不影响成绩记录。
                  </p>
                </div>
                <Switch
                  id="sound"
                  checked={config.soundEnabled}
                  onCheckedChange={(checked) => void handleSoundChange(checked)}
                />
              </div>
            </div>
          </section>

          {/* 修改密码 */}
          <section>
            <GroupHeading title="修改密码" note="security" />

            <form className="mt-2" onSubmit={(event) => void handleChangePassword(event)}>
              <div className="divide-y divide-border border-y border-border">
                <div className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-8">
                  <div className="min-w-0">
                    <Label htmlFor="old-password" className="cursor-pointer text-[13px]">
                      当前密码
                    </Label>
                    <p className="mt-1 text-[12px] leading-[1.7] text-muted-foreground">用于验证身份。</p>
                  </div>
                  <Input
                    id="old-password"
                    type="password"
                    value={oldPassword}
                    onChange={(event) => setOldPassword(event.target.value)}
                    autoComplete="current-password"
                    className="w-full sm:max-w-[17rem]"
                  />
                </div>

                <div className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-8">
                  <div className="min-w-0">
                    <Label htmlFor="new-password" className="cursor-pointer text-[13px]">
                      新密码
                    </Label>
                    <p className="mt-1 text-[12px] leading-[1.7] text-muted-foreground">至少 6 个字符。</p>
                  </div>
                  <Input
                    id="new-password"
                    type="password"
                    value={newPassword}
                    onChange={(event) => setNewPassword(event.target.value)}
                    autoComplete="new-password"
                    className="w-full sm:max-w-[17rem]"
                  />
                </div>

                <div className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-8">
                  <div className="min-w-0">
                    <Label htmlFor="confirm-password" className="cursor-pointer text-[13px]">
                      确认新密码
                    </Label>
                    <p className="mt-1 text-[12px] leading-[1.7] text-muted-foreground">
                      再输入一次，避免手误。
                    </p>
                  </div>
                  <Input
                    id="confirm-password"
                    type="password"
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    autoComplete="new-password"
                    className="w-full sm:max-w-[17rem]"
                  />
                </div>
              </div>

              <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-4">
                <span className="text-[12px] leading-[1.7] text-muted-foreground">
                  更新后需用新密码重新登录。
                </span>
                <Button
                  type="submit"
                  disabled={savingPassword || !oldPassword || !newPassword || !confirmPassword}
                >
                  {savingPassword ? '保存中…' : '更新密码'}
                </Button>
              </div>
            </form>
          </section>

          {/* 帮助中心 */}
          <section>
            <GroupHeading title="帮助中心" note="help" />

            {helpGroups.map((group) => (
              <div key={group.title} className="mt-5">
                <h3 className="label-mono text-[9px]">{group.title}</h3>
                <ul className="mt-2 divide-y divide-border border-y border-border">
                  {group.items.map((item, index) => (
                    <li
                      key={`${group.title}-${index}`}
                      className="flex gap-4 py-2.5 text-[13px] leading-[1.85] text-muted-foreground"
                    >
                      <span className="label-mono shrink-0 pt-1 text-[9px] opacity-60">
                        {String(index + 1).padStart(2, '0')}
                      </span>
                      <span className="min-w-0">{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </section>
        </div>
      </div>
    </PageBody>
  );
}
