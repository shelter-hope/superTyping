import { useEffect, useRef } from 'react';
import { Application, Graphics } from 'pixi.js';
import { cellShape, type MonsterKind } from '@/game/cellShape';

const KINDS: MonsterKind[] = ['NORMAL', 'ATTACKER', 'EVASIVE', 'SMART'];
let demoBoot: Promise<void> = Promise.resolve();

export default function EvolutionDemo() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let stopped = false;
    let app: Application | null = null;
    const graphics = new Graphics();
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const boot = demoBoot.then(async () => {
      if (stopped) return;
      const view = new Application();
      await view.init({ canvas, width: 640, height: 180, background: 0x071612, antialias: true });
      if (stopped) {
        view.destroy({ removeView: false });
        return;
      }
      app = view;
      view.ticker.stop();
      view.stage.addChild(graphics);
      const started = performance.now();
      const frame = (now: number) => {
        if (stopped) return;
        const generation = reduced ? 2 : Math.floor((now - started) / 2000) % 6;
        graphics.clear();
        KINDS.forEach((kind, index) => {
          const shape = cellShape(kind, generation);
          const x = 80 + index * 150;
          const y = 90;
          graphics.circle(x, y, shape.radius).fill({ color: shape.color, alpha: shape.alpha });
          for (let tentacle = 0; tentacle < shape.tentacles; tentacle += 1) {
            const angle = (Math.PI * 2 * tentacle) / shape.tentacles;
            graphics.moveTo(x, y).lineTo(x + Math.cos(angle) * (shape.radius + 16), y + Math.sin(angle) * (shape.radius + 16)).stroke({ color: shape.color, width: 2, alpha: 0.9 });
          }
          for (let node = 0; node < shape.nodes; node += 1) {
            const angle = (Math.PI * 2 * node) / shape.nodes;
            graphics.circle(x + Math.cos(angle) * 24, y + Math.sin(angle) * 24, 3).fill({ color: 0xff4fa3 });
          }
        });
        view.render();
        requestAnimationFrame(frame);
      };
      requestAnimationFrame(frame);
    });
    demoBoot = boot.catch(() => undefined);
    return () => {
      stopped = true;
      app?.ticker.stop();
      app?.destroy({ removeView: false });
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      width={640}
      height={180}
      aria-hidden="true"
      className="mx-auto h-auto w-full max-w-3xl rounded-[1.5rem] border border-primary/30"
    />
  );
}
