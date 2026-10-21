import { useEffect, useRef } from 'react';
import { Application, Graphics } from 'pixi.js';
import type { MonsterEntity } from '@/game/Monster';
import type { PlayerState, InputMode } from '@/game/types';
import { cellShape, type MonsterKind } from '@/game/cellShape';

interface GameCanvasProps {
  width: number;
  height: number;
  monsters: MonsterEntity[];
  player: PlayerState;
  inputMode: InputMode;
  chineseBuffer: string;
  lockedMonsterId: string | null;
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

    const boot = async () => {
      const view = new Application();
      await view.init({
        canvas,
        width: propsRef.current.width,
        height: propsRef.current.height,
        background: 0x071612,
        antialias: true,
      });
      if (stopped) {
        view.destroy();
        return;
      }
      app = view;
      view.stage.addChild(graphics);
      const frame = () => {
        if (stopped || !app) return;
        const current = propsRef.current;
        if (app.renderer.width !== current.width || app.renderer.height !== current.height) {
          app.renderer.resize(current.width, current.height);
        }
        drawFrame(graphics, current);
        requestAnimationFrame(frame);
      };
      requestAnimationFrame(frame);
    };
    void boot();
    return () => {
      stopped = true;
      app?.destroy();
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

function drawFrame(graphics: Graphics, props: GameCanvasProps) {
  graphics.clear();
  graphics.circle(props.player.x, props.player.y, 10).fill({ color: 0xffc857 });
  props.monsters.forEach((monster) => {
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
  if (props.inputMode === 'CHINESE' && props.chineseBuffer) {
    graphics.rect(16, props.height - 36, Math.min(280, 24 + props.chineseBuffer.length * 14), 22).fill({ color: 0x12342c, alpha: 0.8 });
  }
}
