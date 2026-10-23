import { describe, expect, it } from 'vitest';
import { boidsSchema } from './registry';
import { createField, createFlock, defaultBoidParams, stepBoids } from './boids';

describe('boids', () => {
  it('keeps a seeded flock reproducible', () => {
    const params = { ...defaultBoidParams, count: 30 };
    const first = createFlock(params, 7, 400, 300);
    const second = createFlock(params, 7, 400, 300);
    stepBoids(first, params, 400, 300);
    stepBoids(second, params, 400, 300);
    expect(first.map((boid) => [boid.x, boid.y])).toEqual(second.map((boid) => [boid.x, boid.y]));
    const result = stepBoids(first, params, 400, 300);
    expect(result.polarization).toBeGreaterThanOrEqual(0);
    expect(result.polarization).toBeLessThanOrEqual(1);
    expect(result.clusters).toBeGreaterThan(0);
    expect(first).toHaveLength(30);
    expect(boidsSchema.safeParse({ ...defaultBoidParams, count: 0 }).success).toBe(false);
  });

  it('keeps flock size when obstacles and predators are present', () => {
    const params = { ...defaultBoidParams, count: 40 };
    const flock = createFlock(params, 11, 800, 480);
    const field = createField(11, 800, 480);
    field.pointer = { x: 200, y: 200, mode: 'repel' };
    const first = stepBoids(flock, params, 800, 480, field);
    const again = createFlock(params, 11, 800, 480);
    const sameField = createField(11, 800, 480);
    sameField.pointer = { x: 200, y: 200, mode: 'repel' };
    stepBoids(again, params, 800, 480, sameField);
    expect(flock).toHaveLength(40);
    expect(first.clusters).toBeGreaterThan(0);
    expect(flock.map((boid) => [boid.x, boid.y])).toEqual(again.map((boid) => [boid.x, boid.y]));
  });
});
