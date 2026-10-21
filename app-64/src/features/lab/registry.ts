import { z } from 'zod';
import { defaultBoidParams, type BoidParams } from './boids';

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

export interface ParamField {
  key: keyof BoidParams;
  label: string;
  min: number;
  max: number;
  step: number;
}

export const boidFields: ParamField[] = [
  { key: 'count', label: '群体规模', min: 1, max: 2000, step: 1 },
  { key: 'visionRadius', label: '视野半径', min: 1, max: 200, step: 1 },
  { key: 'visionAngle', label: '视野角度', min: 0.2, max: Math.PI * 2, step: 0.1 },
  { key: 'maxSpeed', label: '最大速度', min: 0.2, max: 8, step: 0.1 },
  { key: 'maxForce', label: '转向力', min: 0.005, max: 0.5, step: 0.005 },
  { key: 'separation', label: '分离', min: 0, max: 4, step: 0.1 },
  { key: 'alignment', label: '对齐', min: 0, max: 4, step: 0.1 },
  { key: 'cohesion', label: '凝聚', min: 0, max: 4, step: 0.1 },
];

export interface LabAlgorithm<TParams extends Record<string, number>> {
  id: string;
  name: string;
  description: string;
  defaults: TParams;
  fields: ParamField[];
}

export const labRegistry: LabAlgorithm<BoidParams>[] = [
  {
    id: 'boids',
    name: 'Boids 鸟群',
    description: '分离、对齐和凝聚三条局部规则产生全局涌现。',
    defaults: defaultBoidParams,
    fields: boidFields,
  },
];
