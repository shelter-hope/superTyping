import { useEffect, useRef } from 'react';
import { Application, Graphics } from 'pixi.js';
import type { MonsterEntity } from '@/game/Monster';
import type { PlayerState, InputMode } from '@/game/types';
import { cellShape, type MonsterKind } from '@/game/cellShape';
import { dnaGeometry } from '@/game/dnaBar';
import { createFxState, syncEffects, type FxState, type MonsterSnap } from '@/game/particles';

let canvasBoot: Promise<void> = Promise.resolve();

interface GameCanvasProps {
  width: number;
  height: number;
  monsters: MonsterEntity[];
  player: PlayerState;
  inputMode: InputMode;
  chineseBuffer: string;
  lockedMonsterId: string | null;
  progress01: number;
  lastCorrect: { timestamp: number } | null;
}

export default function GameCanvas(props: GameCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const propsRef = useRef(props);
  propsRef.current = props;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let stopped = false;
    let app: Application | null = null;
    const graphics = new Graphics();
    const fx: FxState = createFxState();
    let seenCorrect = 0;
    const motion = window.matchMedia('(prefers-reduced-motion: reduce)');
    let reduced = motion.matches;
    const onMotion = () => {
      reduced = motion.matches;
    };
    motion.addEventListener('change', onMotion);

    const boot = canvasBoot.then(async () => {
      if (stopped) return;
      const view = new Application();
      await view.init({
        canvas,
        width: propsRef.current.width,
        height: propsRef.current.height,
        background: 0x071612,
        antialias: true,
      });
      if (stopped) {
        view.destroy({ removeView: false });
        return;
      }
      app = view;
      view.ticker.stop();
      view.stage.addChild(graphics);
      let last = performance.now();
      const frame = (now: number) => {
        if (stopped || !app) return;
        const current = propsRef.current;
        if (app.renderer.width !== current.width || app.renderer.height !== current.height) {
          app.renderer.resize(current.width, current.height);
        }
        const snaps = current.monsters.filter((monster) => monster.isAlive).map(toSnap);
        let hit = null;
        if (current.lastCorrect && current.lastCorrect.timestamp !== seenCorrect && current.lockedMonsterId) {
          const locked = snaps.find((monster) => monster.id === current.lockedMonsterId);
          if (locked) {
            hit = { monsterId: locked.id, x: locked.x, y: locked.y, timestamp: current.lastCorrect.timestamp };
          }
          seenCorrect = current.lastCorrect.timestamp;
        }
        syncEffects(fx, snaps, now, Math.min(32, now - last), reduced, current.width, current.height, hit);
        last = now;
        drawFrame(graphics, current, fx, now, reduced);
        view.render();
        requestAnimationFrame(frame);
      };
      requestAnimationFrame(frame);
    });
    canvasBoot = boot.catch(() => undefined);
    return () => {
      stopped = true;
      motion.removeEventListener('change', onMotion);
      app?.ticker.stop();
      app?.destroy({ removeView: false });
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      width={props.width}
      height={props.height}
      className="game-canvas w-full max-w-full rounded-[1.5rem] border border-primary/40"
    />
  );
}

function toSnap(monster: MonsterEntity): MonsterSnap {
  const shape = cellShape(monster.type as MonsterKind, monster.currentIndex);
  return {
    id: monster.id,
    x: monster.x,
    y: monster.y,
    color: shape.color,
    generation: monster.currentIndex,
  };
}

function drawFrame(
  graphics: Graphics,
  props: GameCanvasProps,
  fx: FxState,
  now: number,
  reduced: boolean,
) {
  graphics.clear();
  graphics.circle(props.player.x, props.player.y, 10).fill({ color: 0xffc857 });
  props.monsters.forEach((monster) => {
    if (!monster.isAlive) return;
    const shape = cellShape(monster.type as MonsterKind, monster.currentIndex);
    const color = props.lockedMonsterId === monster.id ? 0xffffff : shape.color;
    graphics.circle(monster.x, monster.y, shape.radius).fill({ color, alpha: shape.alpha });
    for (let i = 0; i < shape.tentacles; i += 1) {
      const angle = (Math.PI * 2 * i) / shape.tentacles + monster.y / 40;
      graphics.moveTo(monster.x, monster.y).lineTo(monster.x + Math.cos(angle) * (shape.radius + 14), monster.y + Math.sin(angle) * (shape.radius + 14)).stroke({ color, width: 2, alpha: 0.9 });
    }
    for (let i = 0; i < shape.nodes; i += 1) {
      const angle = (Math.PI * 2 * i) / shape.nodes;
      graphics.circle(monster.x + Math.cos(angle) * 22, monster.y + Math.sin(angle) * 22, 3).fill({ color: 0xff4fa3 });
    }
  });
  for (const particle of fx.particles) {
    if (!particle.active) continue;
    const alpha = particle.kind === 'dust' ? 0.35 : Math.max(0, particle.life / particle.maxLife);
    graphics.circle(particle.x, particle.y, particle.radius).fill({ color: particle.color, alpha });
  }
  for (const ripple of fx.ripples) {
    const age = 1 - ripple.life / ripple.maxLife;
    graphics.circle(ripple.x, ripple.y, 8 + age * 28).stroke({ color: 0x3dffe0, width: 2, alpha: 1 - age });
  }
  const dna = dnaGeometry(props.width, props.height, props.progress01, now, reduced);
  for (const rung of dna.rungs) {
    graphics.moveTo(rung.x, rung.y1).lineTo(rung.x, rung.y2).stroke({
      color: rung.current ? 0xff4fa3 : rung.color,
      width: rung.lit || rung.current ? 3 : 1,
      alpha: rung.lit || rung.current ? 0.95 : 0.28,
    });
  }
  if (props.inputMode === 'CHINESE' && props.chineseBuffer) {
    graphics.rect(16, props.height - 64, Math.min(280, 24 + props.chineseBuffer.length * 14), 22).fill({ color: 0x12342c, alpha: 0.8 });
  }
}
