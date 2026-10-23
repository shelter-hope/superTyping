import { z } from 'zod';
import { createField, createFlock, defaultBoidParams, drawBoids, stepBoids, type BoidParams } from './boids';
import type { LabPlugin } from './plugin';
import { acoPlugin } from './aco';
import { geneticPlugin } from './genetic';
import { psoPlugin } from './pso';

export const boidsSchema = z.object({
  count: z.number().int().min(1).max(2000),
  visionRadius: z.number().min(1).max(200),
  visionAngle: z.number().min(0.2).max(Math.PI * 2),
  maxSpeed: z.number().min(0.2).max(8),
  maxForce: z.number().min(0.005).max(0.5),
  separation: z.number().min(0).max(4),
  alignment: z.number().min(0).max(4),
  cohesion: z.number().min(0).max(4),
});

export const boidFields = [
  { key: 'count', label: '群体规模', min: 1, max: 2000, step: 1 },
  { key: 'visionRadius', label: '视野半径', min: 1, max: 200, step: 1 },
  { key: 'visionAngle', label: '视野角度', min: 0.2, max: Math.PI * 2, step: 0.1 },
  { key: 'maxSpeed', label: '最大速度', min: 0.2, max: 8, step: 0.1 },
  { key: 'maxForce', label: '转向力', min: 0.005, max: 0.5, step: 0.005 },
  { key: 'separation', label: '分离', min: 0, max: 4, step: 0.1 },
  { key: 'alignment', label: '对齐', min: 0, max: 4, step: 0.1 },
  { key: 'cohesion', label: '凝聚', min: 0, max: 4, step: 0.1 },
];

export const boidsPlugin: LabPlugin<BoidParams, ReturnType<typeof createBoidState>> = {
  id: 'boids',
  name: 'Boids 鸟群',
  description: '分离、对齐和凝聚三条局部规则产生全局涌现。',
  schema: boidsSchema,
  fields: boidFields,
  defaults: defaultBoidParams,
  metricLabels: { averageSpeed: '平均速度', polarization: '极化度', clusters: '聚簇' },
  explain: [
    '分离让个体躲开过近的邻居。',
    '对齐让个体朝邻居的平均方向转。',
    '凝聚让个体靠近邻居的中心。没有一只鸟知道群体的形状。',
  ],
  pointerHint: '左键吸引，右键驱散。青圈是障碍，洋红点是捕食者。',
  create: createBoidState,
  step(state, params, width, height) {
    const metrics = stepBoids(state.flock, params, width, height, state.field);
    return { averageSpeed: metrics.averageSpeed, polarization: metrics.polarization, clusters: metrics.clusters };
  },
  draw(graphics, state) {
    drawBoids(graphics, state.flock, state.field);
  },
  onPointer(state, pointer) {
    state.field.pointer = pointer;
  },
};

function createBoidState(params: BoidParams, seed: number, width: number, height: number) {
  return {
    flock: createFlock(params, seed, width, height),
    field: createField(seed, width, height),
  };
}

export const labRegistry = [boidsPlugin, psoPlugin, acoPlugin, geneticPlugin];
