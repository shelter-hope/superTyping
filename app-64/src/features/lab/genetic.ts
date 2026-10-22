import { z } from 'zod';
import { createRng } from './boids';
import type { LabPlugin } from './plugin';

const GENE_LENGTH = 16;

export interface GeneticParams {
  population: number;
  mutationRate: number;
  crossoverRate: number;
  elitism: number;
}

export const defaultGeneticParams: GeneticParams = {
  population: 60,
  mutationRate: 0.05,
  crossoverRate: 0.8,
  elitism: 2,
};

export const geneticSchema = z.object({
  population: z.number().int().min(2).max(500).refine((value) => value % 2 === 0, '种群必须是偶数'),
  mutationRate: z.number().min(0).max(0.5),
  crossoverRate: z.number().min(0).max(1),
  elitism: z.number().int().min(0).max(10),
});

interface Individual { genes: number[]; fitness: number }
export interface GeneticState {
  people: Individual[];
  target: number[];
  generation: number;
  random: () => number;
}

function score(genes: number[], target: number[]): number {
  return genes.reduce((sum, gene, index) => sum + (gene === target[index] ? 1 : 0), 0);
}

export function createGenetic(params: GeneticParams, seed: number): GeneticState {
  const random = createRng(seed);
  const target = Array.from({ length: GENE_LENGTH }, () => (random() > 0.5 ? 1 : 0));
  const people = Array.from({ length: params.population }, () => {
    const genes = Array.from({ length: GENE_LENGTH }, () => (random() > 0.5 ? 1 : 0));
    return { genes, fitness: score(genes, target) };
  });
  return { people, target, generation: 0, random };
}

function tournament(state: GeneticState): Individual {
  const first = state.people[Math.floor(state.random() * state.people.length)];
  const second = state.people[Math.floor(state.random() * state.people.length)];
  return first.fitness >= second.fitness ? first : second;
}

export function stepGenetic(state: GeneticState, params: GeneticParams) {
  const ranked = [...state.people].sort((left, right) => right.fitness - left.fitness);
  const eliteCount = Math.min(params.elitism, ranked.length);
  const next = ranked.slice(0, eliteCount).map((person) => ({ genes: [...person.genes], fitness: person.fitness }));
  while (next.length < params.population) {
    const left = tournament(state);
    const right = tournament(state);
    const genes = left.genes.map((gene, index) => {
      const crossed = state.random() < params.crossoverRate && index > Math.floor(state.random() * GENE_LENGTH) ? right.genes[index] : gene;
      return state.random() < params.mutationRate ? 1 - crossed : crossed;
    });
    next.push({ genes, fitness: score(genes, state.target) });
  }
  state.people = next.slice(0, params.population);
  state.generation += 1;
  const best = Math.max(...state.people.map((person) => person.fitness));
  const mean = state.people.reduce((sum, person) => sum + person.fitness, 0) / state.people.length;
  return { bestFitness: best, meanFitness: mean, generation: state.generation };
}

export const geneticPlugin: LabPlugin<GeneticParams, GeneticState> = {
  id: 'genetic',
  name: '遗传算法',
  description: '种群通过选择、交叉和变异靠近一条隐藏的目标基因组。',
  schema: geneticSchema,
  fields: [
    { key: 'population', label: '种群', min: 2, max: 500, step: 2 },
    { key: 'mutationRate', label: '变异率', min: 0, max: 0.5, step: 0.01 },
    { key: 'crossoverRate', label: '交叉率', min: 0, max: 1, step: 0.05 },
    { key: 'elitism', label: '精英数', min: 0, max: 10, step: 1 },
  ],
  defaults: defaultGeneticParams,
  metricLabels: { bestFitness: '最佳适应度', meanFitness: '平均适应度', generation: '代数' },
  explain: [
    '锦标赛选择让更接近目标的个体更可能留下后代。',
    '交叉把两个父本的基因拼到一起。',
    '变异翻开个别位点，精英则原样进入下一代。',
  ],
  create: (params, seed) => createGenetic(params, seed),
  step: (state, params) => stepGenetic(state, params),
  draw(graphics, state) {
    graphics.clear();
    const columns = 10;
    state.people.forEach((person, index) => {
      const x = 40 + (index % columns) * 72;
      const y = 40 + Math.floor(index / columns) * 48;
      const alpha = 0.25 + person.fitness / GENE_LENGTH * 0.75;
      graphics.circle(x, y, 10).fill({ color: 0x3dffe0, alpha });
      if (index < 2) graphics.circle(x, y, 14).stroke({ color: 0xffc857, width: 2, alpha: 0.9 });
    });
  },
};
