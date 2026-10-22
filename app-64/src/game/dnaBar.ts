export const DNA_RUNGS = 24;
export const DNA_TEAL = 0x3dffe0;
export const DNA_AMBER = 0xffc857;
export const DNA_MAGENTA = 0xff4fa3;

export function clampProgress(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.min(1, Math.max(0, value));
}

export function litRungs(progress: number): number {
  return Math.floor(clampProgress(progress) * DNA_RUNGS);
}

export function mixColor(from: number, to: number, t: number): number {
  const amount = Math.min(1, Math.max(0, t));
  const fr = (from >> 16) & 255;
  const fg = (from >> 8) & 255;
  const fb = from & 255;
  const tr = (to >> 16) & 255;
  const tg = (to >> 8) & 255;
  const tb = to & 255;
  const r = Math.round(fr + (tr - fr) * amount);
  const g = Math.round(fg + (tg - fg) * amount);
  const b = Math.round(fb + (tb - fb) * amount);
  return (r << 16) | (g << 8) | b;
}

export interface DnaGeometry {
  y: number;
  amplitude: number;
  rungs: Array<{ x: number; y1: number; y2: number; lit: boolean; color: number; current: boolean }>;
}

export function dnaGeometry(width: number, height: number, progress: number, timeMs: number, reducedMotion: boolean): DnaGeometry {
  const clamped = clampProgress(progress);
  const lit = litRungs(clamped);
  const y = height - 28;
  const amplitude = reducedMotion ? 0 : 8;
  const left = 24;
  const right = Math.max(left + 1, width - 24);
  const rungs = Array.from({ length: DNA_RUNGS }, (_, index) => {
    const t = DNA_RUNGS === 1 ? 0 : index / (DNA_RUNGS - 1);
    const x = left + (right - left) * t;
    const wave = Math.sin(t * Math.PI * 3 + timeMs / 280);
    return {
      x,
      y1: y - amplitude * wave,
      y2: y + amplitude * wave,
      lit: index < lit,
      color: mixColor(DNA_TEAL, DNA_AMBER, t),
      current: index === lit && lit < DNA_RUNGS,
    };
  });
  return { y, amplitude, rungs };
}
