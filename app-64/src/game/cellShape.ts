export type MonsterKind = 'NORMAL' | 'ATTACKER' | 'EVASIVE' | 'SMART';

export interface CellShape {
  radius: number;
  tentacles: number;
  alpha: number;
  nodes: number;
  color: number;
}

export function cellShape(type: MonsterKind, generation = 0): CellShape {
  const gen = Math.max(0, generation);
  if (type === 'ATTACKER') {
    return { radius: 18 + gen, tentacles: 3 + (gen % 4), alpha: 1, nodes: 0, color: 0xff4fa3 };
  }
  if (type === 'EVASIVE') {
    return { radius: 16 + gen * 0.5, tentacles: 0, alpha: 0.45, nodes: 0, color: 0x7dffe2 };
  }
  if (type === 'SMART') {
    return { radius: 16, tentacles: 0, alpha: 1, nodes: 3 + (gen % 3), color: 0xffc857 };
  }
  return { radius: 16 + gen * 0.4, tentacles: 0, alpha: 1, nodes: 0, color: 0x3dffe0 };
}
