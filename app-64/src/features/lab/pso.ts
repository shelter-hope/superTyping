import { z } from 'zod';
import { createRng } from './boids';
import type { LabPlugin } from './plugin';

export interface PsoParams {
  count: number;
  inertia: number;
  cognitive: number;
  social: number;
  maxSpeed: number;
}

export const defaultPsoParams: PsoParams = {
  count: 80,
  inertia: 0.7,
  cognitive: 1.4,
  social: 1.4,
  maxSpeed: 3,
};

export const psoSchema = z.object({
  count: z.number().int().min(1).max(2000),
  inertia: z.number().min(0.1).max(1.2),
  cognitive: z.number().min(0).max(3),
  social: z.number().min(0).max(3),
  maxSpeed: z.number().min(0.2).max(8),
});

interface Peak { x: number; y: number; amp: number }
interface Particle { x: number; y: number; vx: number; vy: number; bestX: number; bestY: number; bestFit: number }
export interface PsoState {
  particles: Particle[];
  peaks: Peak[];
  bestX: number;
  bestY: number;
  bestFit: number;
  random: () => number;
}

export function fitness(x: number, y: number, peaks: Peak[]): number {
  return peaks.reduce((sum, peak) => {
    const distance = (x - peak.x) ** 2 + (y - peak.y) ** 2;
    return sum + peak.amp * Math.exp(-distance / 8000);
  }, 0);
}

export function createPso(params: PsoParams, seed: number, width: number, height: number): PsoState {
  const random = createRng(seed);
  const peaks = Array.from({ length: 4 }, () => ({
    x: 40 + random() * (width - 80),
    y: 40 + random() * (height - 80),
    amp: 0.6 + random() * 0.8,
  }));
  const particles = Array.from({ length: params.count }, () => {
    const x = random() * width;
    const y = random() * height;
    const fit = fitness(x, y, peaks);
    return { x, y, vx: (random() - 0.5) * params.maxSpeed, vy: (random() - 0.5) * params.maxSpeed, bestX: x, bestY: y, bestFit: fit };
  });
  const best = particles.reduce((winner, particle) => (particle.bestFit > winner.bestFit ? particle : winner));
  return { particles, peaks, bestX: best.bestX, bestY: best.bestY, bestFit: best.bestFit, random };
}

export function stepPso(state: PsoState, params: PsoParams, width: number, height: number) {
  if (state.particles.length !== params.count) {
    return metrics(state);
  }
  for (const particle of state.particles) {
    const vx = params.inertia * particle.vx
      + params.cognitive * state.random() * (particle.bestX - particle.x)
      + params.social * state.random() * (state.bestX - particle.x);
    const vy = params.inertia * particle.vy
      + params.cognitive * state.random() * (particle.bestY - particle.y)
      + params.social * state.random() * (state.bestY - particle.y);
    const speed = Math.hypot(vx, vy) || 1;
    const scale = speed > params.maxSpeed ? params.maxSpeed / speed : 1;
    particle.vx = vx * scale;
    particle.vy = vy * scale;
    particle.x = Math.min(width, Math.max(0, particle.x + particle.vx));
    particle.y = Math.min(height, Math.max(0, particle.y + particle.vy));
    const fit = fitness(particle.x, particle.y, state.peaks);
    if (fit > particle.bestFit) {
      particle.bestFit = fit;
      particle.bestX = particle.x;
      particle.bestY = particle.y;
    }
    if (particle.bestFit > state.bestFit) {
      state.bestFit = particle.bestFit;
      state.bestX = particle.bestX;
      state.bestY = particle.bestY;
    }
  }
  return metrics(state);
}

function metrics(state: PsoState) {
  const mean = state.particles.reduce((sum, particle) => sum + fitness(particle.x, particle.y, state.peaks), 0) / Math.max(1, state.particles.length);
  const meanX = state.particles.reduce((sum, particle) => sum + particle.x, 0) / Math.max(1, state.particles.length);
  const spread = Math.sqrt(state.particles.reduce((sum, particle) => sum + (particle.x - meanX) ** 2, 0) / Math.max(1, state.particles.length));
  return { bestFitness: state.bestFit, meanFitness: mean, spread };
}

export const psoPlugin: LabPlugin<PsoParams, PsoState> = {
  id: 'pso',
  name: 'PSO 粒子群',
  description: '粒子记住自己和群体去过的最好位置，在多峰地形上聚拢。',
  schema: psoSchema,
  fields: [
    { key: 'count', label: '粒子数', min: 1, max: 2000, step: 1 },
    { key: 'inertia', label: '惯性', min: 0.1, max: 1.2, step: 0.05 },
    { key: 'cognitive', label: '个体记忆', min: 0, max: 3, step: 0.1 },
    { key: 'social', label: '群体记忆', min: 0, max: 3, step: 0.1 },
    { key: 'maxSpeed', label: '最大速度', min: 0.2, max: 8, step: 0.1 },
  ],
  defaults: defaultPsoParams,
  metricLabels: { bestFitness: '全局最优', meanFitness: '平均适应度', spread: '位置标准差' },
  explain: [
    '惯性让粒子保持原来的方向。',
    '个体记忆把粒子拉向它自己的最好位置。',
    '群体记忆把粒子拉向全群目前的最好位置。',
  ],
  create: createPso,
  step: stepPso,
  draw(graphics, state) {
    graphics.clear();
    for (const peak of state.peaks) {
      graphics.circle(peak.x, peak.y, 18).stroke({ color: 0xff4fd8, width: 2, alpha: 0.45 });
    }
    for (const particle of state.particles) {
      graphics.circle(particle.x, particle.y, 2.5).fill({ color: 0x3dffe0 });
    }
    graphics.circle(state.bestX, state.bestY, 7).fill({ color: 0xffc857 });
  },
};
