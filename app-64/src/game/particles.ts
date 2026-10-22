export const POOL_SIZE = 240;
export const DUST_COUNT = 40;
export const BURST_LIFE = 400;
export const RIPPLE_LIFE = 280;
export const RIPPLE_GAP = 100;

export interface MonsterSnap {
  id: string;
  x: number;
  y: number;
  color: number;
  generation: number;
}

export interface FxParticle {
  active: boolean;
  kind: 'burst' | 'dust';
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  color: number;
  radius: number;
}

export interface Ripple {
  x: number;
  y: number;
  life: number;
  maxLife: number;
  monsterId: string;
}

export interface FxState {
  particles: FxParticle[];
  ripples: Ripple[];
  previous: Map<string, MonsterSnap>;
  lastRippleAt: Map<string, number>;
  dustReady: boolean;
}

export interface HitPing {
  monsterId: string;
  x: number;
  y: number;
  timestamp: number;
}

function emptyParticle(): FxParticle {
  return {
    active: false,
    kind: 'burst',
    x: 0,
    y: 0,
    vx: 0,
    vy: 0,
    life: 0,
    maxLife: 1,
    color: 0x3dffe0,
    radius: 2,
  };
}

export function createFxState(): FxState {
  return {
    particles: Array.from({ length: POOL_SIZE }, emptyParticle),
    ripples: [],
    previous: new Map(),
    lastRippleAt: new Map(),
    dustReady: false,
  };
}

export function burstCount(generation: number): number {
  return 12 + (Math.max(0, Math.floor(generation)) % 9);
}

function claimSlot(state: FxState): FxParticle | null {
  const free = state.particles.find((particle) => !particle.active);
  if (free) return free;
  const bursts = state.particles.filter((particle) => particle.active && particle.kind === 'burst');
  if (bursts.length === 0) return null;
  return bursts.reduce((oldest, particle) => (particle.life < oldest.life ? particle : oldest));
}

function place(state: FxState, next: Omit<FxParticle, 'active'>): boolean {
  const slot = claimSlot(state);
  if (!slot) return false;
  Object.assign(slot, next, { active: true });
  return true;
}

function seedDust(state: FxState, width: number, height: number, reducedMotion: boolean) {
  if (state.dustReady) return;
  for (let index = 0; index < DUST_COUNT; index += 1) {
    const edge = index % 4;
    const along = (index * 37) % 100 / 100;
    const x = edge === 0 || edge === 2 ? along * width : edge === 1 ? width - 4 : 4;
    const y = edge === 1 || edge === 3 ? along * height : edge === 2 ? height - 4 : 4;
    place(state, {
      kind: 'dust',
      x,
      y,
      vx: reducedMotion ? 0 : ((index % 5) - 2) * 0.08,
      vy: reducedMotion ? 0 : ((index % 3) - 1) * 0.06,
      life: 1,
      maxLife: 1,
      color: 0x3dffe0,
      radius: 1.2,
    });
  }
  state.dustReady = true;
}

function spawnBurst(state: FxState, snap: MonsterSnap) {
  const count = burstCount(snap.generation);
  for (let index = 0; index < count; index += 1) {
    const angle = (Math.PI * 2 * index) / count;
    const speed = 0.08 + (index % 4) * 0.03;
    place(state, {
      kind: 'burst',
      x: snap.x,
      y: snap.y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      life: BURST_LIFE,
      maxLife: BURST_LIFE,
      color: snap.color,
      radius: 2 + (snap.generation % 3),
    });
  }
}

export function syncEffects(
  state: FxState,
  monsters: MonsterSnap[],
  now: number,
  dt: number,
  reducedMotion: boolean,
  width: number,
  height: number,
  hit?: HitPing | null,
) {
  seedDust(state, width, height, reducedMotion);
  const seen = new Set<string>();
  for (const monster of monsters) {
    seen.add(monster.id);
    state.previous.set(monster.id, monster);
  }
  if (!reducedMotion) {
    for (const [id, snap] of state.previous) {
      if (seen.has(id)) continue;
      spawnBurst(state, snap);
      state.previous.delete(id);
    }
    if (hit) {
      const last = state.lastRippleAt.get(hit.monsterId) ?? -Infinity;
      if (now - last >= RIPPLE_GAP) {
        state.lastRippleAt.set(hit.monsterId, now);
        state.ripples.push({
          x: hit.x,
          y: hit.y,
          life: RIPPLE_LIFE,
          maxLife: RIPPLE_LIFE,
          monsterId: hit.monsterId,
        });
      }
    }
  } else {
    for (const id of [...state.previous.keys()]) {
      if (!seen.has(id)) state.previous.delete(id);
    }
  }

  const step = Math.max(0, dt);
  for (const particle of state.particles) {
    if (!particle.active) continue;
    if (particle.kind === 'dust') {
      particle.x = (particle.x + particle.vx * step + width) % width;
      particle.y = (particle.y + particle.vy * step + height) % height;
      continue;
    }
    particle.life -= step;
    particle.x += particle.vx * step;
    particle.y += particle.vy * step;
    if (particle.life <= 0) particle.active = false;
  }
  state.ripples = state.ripples
    .map((ripple) => ({ ...ripple, life: ripple.life - step }))
    .filter((ripple) => ripple.life > 0);
}

export function activeBurstCount(state: FxState): number {
  return state.particles.filter((particle) => particle.active && particle.kind === 'burst').length;
}
