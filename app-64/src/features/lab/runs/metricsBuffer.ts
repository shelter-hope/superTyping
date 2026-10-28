/** 客户端指标缓冲：按固定间隔采样，入库前 stride 压到 maxPoints。 */

export interface MetricsEnvelope {
  sampleIntervalMs: number;
  maxPoints: number;
  policy: 'stride';
  t: number[];
  series: Record<string, number[]>;
}

export class MetricsBuffer {
  private readonly intervalMs: number;
  private readonly maxPoints: number;
  private startedAt = 0;
  private lastSampleAt = -Infinity;
  private readonly times: number[] = [];
  private readonly series: Record<string, number[]> = {};

  constructor(intervalMs = 250, maxPoints = 240) {
    this.intervalMs = intervalMs;
    this.maxPoints = maxPoints;
  }

  reset(now = performance.now()) {
    this.startedAt = now;
    this.lastSampleAt = -Infinity;
    this.times.length = 0;
    for (const key of Object.keys(this.series)) delete this.series[key];
  }

  push(metrics: Record<string, number>, now = performance.now()) {
    if (this.startedAt === 0) this.startedAt = now;
    if (now - this.lastSampleAt < this.intervalMs) return;
    this.lastSampleAt = now;
    const t = Math.round(now - this.startedAt);
    this.times.push(t);
    for (const [key, value] of Object.entries(metrics)) {
      if (!Number.isFinite(value)) continue;
      if (!this.series[key]) this.series[key] = [];
      this.series[key].push(value);
    }
  }

  summary(): Record<string, number> {
    const out: Record<string, number> = {};
    for (const [key, values] of Object.entries(this.series)) {
      if (values.length === 0) continue;
      // step() 返回的 best* 已是运行中最优，取末点即可
      out[key] = values[values.length - 1];
    }
    return out;
  }

  envelope(): MetricsEnvelope {
    const stride = Math.max(1, Math.ceil(this.times.length / this.maxPoints));
    const t: number[] = [];
    const series: Record<string, number[]> = {};
    for (let i = 0; i < this.times.length; i += stride) {
      t.push(this.times[i]);
      for (const [key, values] of Object.entries(this.series)) {
        if (!series[key]) series[key] = [];
        series[key].push(values[i] ?? values[values.length - 1] ?? 0);
      }
    }
    return {
      sampleIntervalMs: this.intervalMs,
      maxPoints: this.maxPoints,
      policy: 'stride',
      t,
      series,
    };
  }
}
