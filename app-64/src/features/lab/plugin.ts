import type { Graphics } from 'pixi.js';
import type { ZodType } from 'zod';

export interface ParamField {
  key: string;
  label: string;
  min: number;
  max: number;
  step: number;
}

export interface PointerForce {
  x: number;
  y: number;
  mode: 'attract' | 'repel';
}

export interface LabPlugin<TParams extends Record<string, number>, TState> {
  id: string;
  name: string;
  description: string;
  schema: ZodType<TParams>;
  fields: ParamField[];
  defaults: TParams;
  metricLabels: Record<string, string>;
  explain: [string, string, string];
  pointerHint?: string;
  create: (params: TParams, seed: number, width: number, height: number) => TState;
  step: (state: TState, params: TParams, width: number, height: number) => Record<string, number>;
  draw: (graphics: Graphics, state: TState, params: TParams) => void;
  onPointer?: (state: TState, pointer: PointerForce | null) => void;
}
