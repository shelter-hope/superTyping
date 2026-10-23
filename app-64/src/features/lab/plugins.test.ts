import { describe, expect, it } from 'vitest';
import { visibleLabPlugins } from './registry';
import { pheromoneTotal, stepAco, createAco, defaultAcoParams, CITY_COUNT } from './aco';
import { createGenetic, defaultGeneticParams, stepGenetic } from './genetic';
import { createPso, defaultPsoParams, stepPso } from './pso';

describe('lab catalog', () => {
  it('hides disabled algorithms and keeps the server order', () => {
    const plugins = [{ id: 'boids' }, { id: 'pso' }, { id: 'ant-colony' }];
    expect(visibleLabPlugins(plugins, ['ant-colony', 'boids']).map((plugin) => plugin.id)).toEqual(['ant-colony', 'boids']);
    expect(visibleLabPlugins(plugins, null)).toEqual(plugins);
  });
});

describe('lab plugins', () => {
  it('keeps a seeded PSO reproducible and does not lose the best fitness', () => {
    const first = createPso({ ...defaultPsoParams, count: 30 }, 7, 800, 480);
    const second = createPso({ ...defaultPsoParams, count: 30 }, 7, 800, 480);
    const before = first.bestFit;
    stepPso(first, { ...defaultPsoParams, count: 30 }, 800, 480);
    stepPso(second, { ...defaultPsoParams, count: 30 }, 800, 480);
    expect(first.particles.map((particle) => [particle.x, particle.y])).toEqual(second.particles.map((particle) => [particle.x, particle.y]));
    const after = stepPso(first, { ...defaultPsoParams, count: 30 }, 800, 480);
    expect(after.bestFitness).toBeGreaterThanOrEqual(before);
    expect(first.particles).toHaveLength(30);
  });

  it('keeps twenty cities and lowers pheromone when a step evaporates', () => {
    const state = createAco({ ...defaultAcoParams, ants: 12 }, 7, 800, 480);
    expect(state.cities).toHaveLength(CITY_COUNT);
    const before = pheromoneTotal(state);
    const metrics = stepAco(state, { ...defaultAcoParams, ants: 12 });
    expect(state.ants).toHaveLength(12);
    expect(pheromoneTotal(state)).toBeLessThan(before);
    expect(metrics.bestLength).toBeGreaterThanOrEqual(0);
    expect(Number.isFinite(metrics.pheromone)).toBe(true);
  });

  it('keeps the genetic population stable and does not lose the elite fitness', () => {
    const params = { ...defaultGeneticParams, population: 20, elitism: 2 };
    const first = createGenetic(params, 7);
    const second = createGenetic(params, 7);
    const before = Math.max(...first.people.map((person) => person.fitness));
    stepGenetic(first, params);
    stepGenetic(second, params);
    expect(first.people.map((person) => person.genes)).toEqual(second.people.map((person) => person.genes));
    const after = stepGenetic(first, params);
    expect(first.people).toHaveLength(20);
    expect(after.bestFitness).toBeGreaterThanOrEqual(before);
  });
});
