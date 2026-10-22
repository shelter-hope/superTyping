import type { Graphics } from 'pixi.js';

export interface BoidParams {
  count: number;
  visionRadius: number;
  visionAngle: number;
  maxSpeed: number;
  maxForce: number;
  separation: number;
  alignment: number;
  cohesion: number;
}

export interface Boid {
  x: number;
  y: number;
  vx: number;
  vy: number;
}

export interface BoidMetrics {
  averageSpeed: number;
  polarization: number;
  clusters: number;
}

export interface Obstacle {
  x: number;
  y: number;
  radius: number;
}

export interface PointerForce {
  x: number;
  y: number;
  mode: 'attract' | 'repel';
}

export interface BoidField {
  obstacles: Obstacle[];
  predators: Boid[];
  pointer: PointerForce | null;
}

export const defaultBoidParams: BoidParams = {
  count: 80,
  visionRadius: 40,
  visionAngle: Math.PI,
  maxSpeed: 2.2,
  maxForce: 0.05,
  separation: 1.4,
  alignment: 0.8,
  cohesion: 0.6,
};

export function createRng(seed: number) {
  let state = seed >>> 0;
  return () => {
    state = (Math.imul(state, 1664525) + 1013904223) >>> 0;
    return state / 4294967296;
  };
}

export function createFlock(params: BoidParams, seed: number, width: number, height: number): Boid[] {
  const random = createRng(seed);
  return Array.from({ length: params.count }, () => ({
    x: random() * width,
    y: random() * height,
    vx: (random() - 0.5) * params.maxSpeed,
    vy: (random() - 0.5) * params.maxSpeed,
  }));
}

export function createField(seed: number, width: number, height: number): BoidField {
  const random = createRng(seed + 99);
  return {
    obstacles: [
      { x: width * 0.35, y: height * 0.42, radius: 36 },
      { x: width * 0.68, y: height * 0.6, radius: 28 },
    ],
    predators: Array.from({ length: 2 }, () => ({
      x: random() * width,
      y: random() * height,
      vx: (random() - 0.5) * 1.2,
      vy: (random() - 0.5) * 1.2,
    })),
    pointer: null,
  };
}

export function stepBoids(boids: Boid[], params: BoidParams, width: number, height: number, field?: BoidField): BoidMetrics {
  const cell = Math.max(8, params.visionRadius);
  const grid = new Map<string, number[]>();
  boids.forEach((boid, index) => {
    const key = `${Math.floor(boid.x / cell)}:${Math.floor(boid.y / cell)}`;
    const bucket = grid.get(key);
    if (bucket) bucket.push(index);
    else grid.set(key, [index]);
  });
  const next = boids.map((boid, index) => steer(boid, index, boids, grid, cell, params, width, height, field));
  boids.splice(0, boids.length, ...next);
  if (field) stepPredators(field, boids, width, height);
  return metrics(boids, cell);
}

function steer(
  boid: Boid,
  index: number,
  boids: Boid[],
  grid: Map<string, number[]>,
  cell: number,
  params: BoidParams,
  width: number,
  height: number,
  field?: BoidField,
): Boid {
  let sx = 0, sy = 0, ax = 0, ay = 0, cx = 0, cy = 0, neighbors = 0;
  const gx = Math.floor(boid.x / cell);
  const gy = Math.floor(boid.y / cell);
  for (let oy = -1; oy <= 1; oy += 1) {
    for (let ox = -1; ox <= 1; ox += 1) {
      const bucket = grid.get(`${gx + ox}:${gy + oy}`);
      if (!bucket) continue;
      for (const otherIndex of bucket) {
        if (otherIndex === index) continue;
        const other = boids[otherIndex];
        const dx = other.x - boid.x;
        const dy = other.y - boid.y;
        const dist = Math.hypot(dx, dy) || 0.001;
        if (dist > params.visionRadius || !inFront(boid, dx, dy, params.visionAngle)) continue;
        neighbors += 1;
        sx -= dx / dist / dist;
        sy -= dy / dist / dist;
        ax += other.vx;
        ay += other.vy;
        cx += other.x;
        cy += other.y;
      }
    }
  }
  let vx = boid.vx;
  let vy = boid.vy;
  if (neighbors > 0) {
    vx += limit(sx * params.separation, params.maxForce);
    vy += limit(sy * params.separation, params.maxForce);
    vx += limit(ax / neighbors - boid.vx, params.maxForce) * params.alignment;
    vy += limit(ay / neighbors - boid.vy, params.maxForce) * params.alignment;
    vx += limit(cx / neighbors - boid.x, params.maxForce) * params.cohesion;
    vy += limit(cy / neighbors - boid.y, params.maxForce) * params.cohesion;
  }
  if (field) {
    for (const obstacle of field.obstacles) {
      const dx = boid.x - obstacle.x;
      const dy = boid.y - obstacle.y;
      const dist = Math.hypot(dx, dy) || 0.001;
      const influence = obstacle.radius + 24;
      if (dist < influence) {
        const strength = (influence - dist) / influence;
        vx += (dx / dist) * strength * params.maxForce * 3;
        vy += (dy / dist) * strength * params.maxForce * 3;
      }
    }
    for (const predator of field.predators) {
      const dx = boid.x - predator.x;
      const dy = boid.y - predator.y;
      const dist = Math.hypot(dx, dy) || 0.001;
      if (dist < params.visionRadius * 1.5) {
        vx += limit((dx / dist) * params.maxForce * 2.4, params.maxForce * 2);
        vy += limit((dy / dist) * params.maxForce * 2.4, params.maxForce * 2);
      }
    }
    if (field.pointer) {
      const dx = field.pointer.x - boid.x;
      const dy = field.pointer.y - boid.y;
      const dist = Math.hypot(dx, dy) || 0.001;
      if (dist < 140) {
        const sign = field.pointer.mode === 'attract' ? 1 : -1;
        vx += limit((sign * dx) / dist, params.maxForce) * 1.8;
        vy += limit((sign * dy) / dist, params.maxForce) * 1.8;
      }
    }
  }
  const speed = Math.hypot(vx, vy) || 1;
  if (speed > params.maxSpeed) {
    vx = vx / speed * params.maxSpeed;
    vy = vy / speed * params.maxSpeed;
  }
  return {
    x: wrap(boid.x + vx, width),
    y: wrap(boid.y + vy, height),
    vx,
    vy,
  };
}

function stepPredators(field: BoidField, boids: Boid[], width: number, height: number) {
  for (const predator of field.predators) {
    let nearest = boids[0];
    let best = Infinity;
    for (const boid of boids) {
      const dist = Math.hypot(boid.x - predator.x, boid.y - predator.y);
      if (dist < best) {
        best = dist;
        nearest = boid;
      }
    }
    if (nearest && best < 180) {
      predator.vx += limit((nearest.x - predator.x) * 0.02, 0.08);
      predator.vy += limit((nearest.y - predator.y) * 0.02, 0.08);
    }
    const speed = Math.hypot(predator.vx, predator.vy) || 1;
    const maxSpeed = 1.6;
    if (speed > maxSpeed) {
      predator.vx = (predator.vx / speed) * maxSpeed;
      predator.vy = (predator.vy / speed) * maxSpeed;
    }
    predator.x = wrap(predator.x + predator.vx, width);
    predator.y = wrap(predator.y + predator.vy, height);
  }
}

function inFront(boid: Boid, dx: number, dy: number, angle: number) {
  const heading = Math.atan2(boid.vy, boid.vx);
  let delta = Math.atan2(dy, dx) - heading;
  while (delta > Math.PI) delta -= Math.PI * 2;
  while (delta < -Math.PI) delta += Math.PI * 2;
  return Math.abs(delta) <= angle / 2;
}

function limit(value: number, max: number) {
  return Math.max(-max, Math.min(max, value));
}

function wrap(value: number, size: number) {
  return ((value % size) + size) % size;
}

function metrics(boids: Boid[], cell: number): BoidMetrics {
  let speed = 0, vx = 0, vy = 0;
  const occupied = new Set<string>();
  for (const boid of boids) {
    const mag = Math.hypot(boid.vx, boid.vy) || 1;
    speed += mag;
    vx += boid.vx / mag;
    vy += boid.vy / mag;
    occupied.add(`${Math.floor(boid.x / cell)}:${Math.floor(boid.y / cell)}`);
  }
  const count = Math.max(1, boids.length);
  return {
    averageSpeed: speed / count,
    polarization: Math.hypot(vx, vy) / count,
    clusters: occupied.size,
  };
}

export function drawBoids(graphics: Graphics, flock: Boid[], field: BoidField) {
  graphics.clear();
  for (const obstacle of field.obstacles) {
    graphics.circle(obstacle.x, obstacle.y, obstacle.radius).stroke({ color: 0x3dffe0, width: 2, alpha: 0.7 });
  }
  for (const predator of field.predators) {
    graphics.circle(predator.x, predator.y, 6).fill({ color: 0xff4fd8 });
  }
  for (const boid of flock) {
    graphics.circle(boid.x, boid.y, 2.5).fill({ color: 0x3dffe0 });
  }
}
