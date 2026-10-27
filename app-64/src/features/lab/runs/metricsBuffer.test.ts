import { describe, expect, it } from 'vitest';
import { MetricsBuffer } from './metricsBuffer';

describe('MetricsBuffer', () => {
  it('samples on interval and strides for envelope', () => {
    const buffer = new MetricsBuffer(250, 3);
    buffer.reset(0);
    buffer.push({ bestFitness: 3 }, 0);
    buffer.push({ bestFitness: 2.5 }, 100);
    buffer.push({ bestFitness: 2 }, 250);
    buffer.push({ bestFitness: 1.5 }, 500);
    buffer.push({ bestFitness: 1 }, 750);
    const envelope = buffer.envelope();
    expect(envelope.t.length).toBeLessThanOrEqual(3);
    expect(buffer.summary().bestFitness).toBe(1);
  });
});
