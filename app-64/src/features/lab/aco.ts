import { z } from 'zod';
import { createRng } from './boids';
import type { LabPlugin } from './plugin';

export const CITY_COUNT = 20;

export interface AcoParams {
  ants: number;
  alpha: number;
  beta: number;
  evaporation: number;
  deposit: number;
}

export const defaultAcoParams: AcoParams = {
  ants: 40,
  alpha: 1,
  beta: 2,
  evaporation: 0.2,
  deposit: 2,
};

export const acoSchema = z.object({
  ants: z.number().int().min(1).max(500),
  alpha: z.number().min(0).max(4),
  beta: z.number().min(0).max(6),
  evaporation: z.number().min(0.01).max(0.8),
  deposit: z.number().min(0.1).max(10),
});

interface City { x: number; y: number }
interface Ant { tour: number[] }
export interface AcoState {
  cities: City[];
  pheromone: number[][];
  ants: Ant[];
  bestLength: number;
  bestTour: number[];
  lastMean: number;
  random: () => number;
}

export function createAco(params: AcoParams, seed: number, width: number, height: number): AcoState {
  const random = createRng(seed);
  const cities = Array.from({ length: CITY_COUNT }, () => ({
    x: 30 + random() * (width - 60),
    y: 30 + random() * (height - 60),
  }));
  const pheromone = Array.from({ length: CITY_COUNT }, () => Array.from({ length: CITY_COUNT }, () => 1));
  const ants = Array.from({ length: params.ants }, (_, index) => ({ tour: [index % CITY_COUNT] }));
  return { cities, pheromone, ants, bestLength: Number.POSITIVE_INFINITY, bestTour: [], lastMean: 0, random };
}

function distance(state: AcoState, from: number, to: number) {
  return Math.hypot(state.cities[from].x - state.cities[to].x, state.cities[from].y - state.cities[to].y);
}

export function tourLength(state: AcoState, tour: number[]): number {
  let length = 0;
  for (let index = 0; index < tour.length; index += 1) {
    length += distance(state, tour[index], tour[(index + 1) % tour.length]);
  }
  return length;
}

export function pheromoneTotal(state: AcoState): number {
  return state.pheromone.reduce((sum, row) => sum + row.reduce((rowSum, value) => rowSum + value, 0), 0);
}

export function stepAco(state: AcoState, params: AcoParams) {
  const deposit = state.pheromone.map((row) => row.map(() => 0));
  const finished: number[] = [];
  for (const ant of state.ants) {
    const current = ant.tour[ant.tour.length - 1];
    const visited = new Set(ant.tour);
    const options: Array<{ city: number; score: number }> = [];
    let total = 0;
    for (let city = 0; city < CITY_COUNT; city += 1) {
      if (visited.has(city)) continue;
      const score = (state.pheromone[current][city] ** params.alpha) * ((1 / Math.max(1, distance(state, current, city))) ** params.beta);
      options.push({ city, score });
      total += score;
    }
    if (options.length === 0) continue;
    let roll = state.random() * total;
    let chosen = options[options.length - 1].city;
    for (const option of options) {
      roll -= option.score;
      if (roll <= 0) {
        chosen = option.city;
        break;
      }
    }
    ant.tour.push(chosen);
    if (ant.tour.length === CITY_COUNT) {
      const length = tourLength(state, ant.tour);
      finished.push(length);
      if (length < state.bestLength) {
        state.bestLength = length;
        state.bestTour = [...ant.tour];
      }
      const amount = params.deposit / Math.max(1, length);
      for (let index = 0; index < ant.tour.length; index += 1) {
        const from = ant.tour[index];
        const to = ant.tour[(index + 1) % ant.tour.length];
        deposit[from][to] += amount;
        deposit[to][from] += amount;
      }
      ant.tour = [ant.tour[0]];
    }
  }
  for (let from = 0; from < CITY_COUNT; from += 1) {
    for (let to = 0; to < CITY_COUNT; to += 1) {
      state.pheromone[from][to] = state.pheromone[from][to] * (1 - params.evaporation) + deposit[from][to];
    }
  }
  state.lastMean = finished.length ? finished.reduce((sum, length) => sum + length, 0) / finished.length : state.lastMean;
  return {
    bestLength: Number.isFinite(state.bestLength) ? state.bestLength : 0,
    meanLength: state.lastMean,
    pheromone: pheromoneTotal(state),
  };
}

export const acoPlugin: LabPlugin<AcoParams, AcoState> = {
  id: 'ant-colony',
  name: '蚁群',
  description: '蚂蚁把走过的路留下信息素，短回路会逐渐变亮。',
  schema: acoSchema,
  fields: [
    { key: 'ants', label: '蚂蚁数', min: 1, max: 500, step: 1 },
    { key: 'alpha', label: '信息素权重', min: 0, max: 4, step: 0.1 },
    { key: 'beta', label: '距离权重', min: 0, max: 6, step: 0.1 },
    { key: 'evaporation', label: '蒸发', min: 0.01, max: 0.8, step: 0.01 },
    { key: 'deposit', label: '沉积', min: 0.1, max: 10, step: 0.1 },
  ],
  defaults: defaultAcoParams,
  metricLabels: { bestLength: '最优回路', meanLength: '本代平均', pheromone: '信息素总量' },
  explain: [
    '每只蚂蚁只看附近城市和路上残留的信息素。',
    '更短的回路留下更多痕迹。',
    '蒸发让过时的路慢慢消失，群体因此收成一条短回路。',
  ],
  create: createAco,
  step: (state, params) => stepAco(state, params),
  draw(graphics, state) {
    graphics.clear();
    for (let from = 0; from < state.cities.length; from += 1) {
      for (let to = from + 1; to < state.cities.length; to += 1) {
        const alpha = Math.min(0.8, state.pheromone[from][to] / 8);
        graphics.moveTo(state.cities[from].x, state.cities[from].y).lineTo(state.cities[to].x, state.cities[to].y).stroke({ color: 0x3dffe0, width: 1, alpha });
      }
    }
    if (state.bestTour.length > 1) {
      for (let index = 0; index < state.bestTour.length; index += 1) {
        const from = state.cities[state.bestTour[index]];
        const to = state.cities[state.bestTour[(index + 1) % state.bestTour.length]];
        graphics.moveTo(from.x, from.y).lineTo(to.x, to.y).stroke({ color: 0xff4fd8, width: 2, alpha: 0.9 });
      }
    }
    for (const city of state.cities) {
      graphics.circle(city.x, city.y, 4).fill({ color: 0xffc857 });
    }
  },
};
