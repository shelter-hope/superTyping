import type { MonsterType } from '@/game/types';

/**
 * 对局的墨色色板。
 * 画布走的是「纸墨排字台」语汇：深墨底、米白字、朱红标记，
 * 与外壳的纸面色板是同一套，只是正片叠底的方向反过来。
 */
export const INK = {
  bg: 0x14120f,
  grid: 0x2b2723,
  cream: 0xf4efe4,
  creamDim: 0x8e8577,
  vermilion: 0xce3a2a,
  ochre: 0xb08a45,
  teal: 0x5c8a80,
} as const;

export const MONSTER_INK: Record<MonsterType, { color: number; label: string }> = {
  NORMAL: { color: INK.creamDim, label: 'NORMAL' },
  ATTACKER: { color: INK.vermilion, label: 'ATTACK' },
  EVASIVE: { color: INK.teal, label: 'EVADE' },
  SMART: { color: INK.ochre, label: 'SMART' },
};

export const INK_FONT_SERIF = '"Noto Serif SC", "Songti SC", "Source Han Serif SC", Georgia, serif';
export const INK_FONT_MONO = '"JetBrains Mono", "Cascadia Code", Consolas, monospace';
