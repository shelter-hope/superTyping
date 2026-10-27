import { useEffect, useRef } from 'react';
import { Application, Container, Graphics, Text } from 'pixi.js';
import { dnaGeometry } from '@/game/dnaBar';
import { INK, INK_FONT_MONO, INK_FONT_SERIF, MONSTER_INK } from '@/game/inkPalette';
import type { MonsterEntity } from '@/game/Monster';
import { createFxState, syncEffects, type FxState, type HitPing, type MonsterSnap } from '@/game/particles';
import type { InputMode, PlayerState } from '@/game/types';

let canvasBoot: Promise<void> = Promise.resolve();

const GRID = 72;

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

interface Glyph {
  word: Text;
  tag: Text;
  src: string;
  fill: number;
  fontSize: number;
  width: number;
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
    const stageGraphics = new Graphics();
    const glyphLayer = new Container();
    const glyphs = new Map<string, Glyph>();
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
        background: INK.bg,
        antialias: true,
      });
      if (stopped) {
        view.destroy({ removeView: false });
        return;
      }
      app = view;
      view.ticker.stop();
      view.stage.addChild(stageGraphics);
      view.stage.addChild(glyphLayer);
      let last = performance.now();

      const frame = (now: number) => {
        if (stopped || !app) return;
        const current = propsRef.current;
        if (app.renderer.width !== current.width || app.renderer.height !== current.height) {
          app.renderer.resize(current.width, current.height);
        }
        const alive = current.monsters.filter((monster) => monster.isAlive);
        syncGlyphs(glyphLayer, glyphs, alive, current.lockedMonsterId);
        const snaps = alive.map(toSnap);
        let hit: HitPing | null = null;
        if (current.lastCorrect && current.lastCorrect.timestamp !== seenCorrect && current.lockedMonsterId) {
          const locked = snaps.find((monster) => monster.id === current.lockedMonsterId);
          if (locked) {
            hit = { monsterId: locked.id, x: locked.x, y: locked.y, timestamp: current.lastCorrect.timestamp };
          }
          seenCorrect = current.lastCorrect.timestamp;
        }
        syncEffects(fx, snaps, now, Math.min(32, now - last), reduced, current.width, current.height, hit);
        last = now;
        drawStage(stageGraphics, current, fx, now, reduced, glyphs);
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
      glyphs.clear();
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      width={props.width}
      height={props.height}
      className="game-canvas block h-auto w-full"
      aria-label="对局画布"
    />
  );
}

function toSnap(monster: MonsterEntity): MonsterSnap {
  return {
    id: monster.id,
    x: monster.x,
    y: monster.y,
    color: MONSTER_INK[monster.type as MonsterEntity['type']].color,
    generation: monster.currentIndex,
  };
}

function fontSizeFor(text: string): number {
  const length = text.length;
  if (length <= 4) return 27;
  if (length <= 8) return 23;
  if (length <= 14) return 19;
  return 17;
}

function approxWidth(text: string, fontSize: number): number {
  let width = 0;
  for (const char of text) {
    width += char.codePointAt(0)! > 0x2e80 ? fontSize : fontSize * 0.58;
  }
  return width + Math.max(0, text.length - 1) * 1;
}

function syncGlyphs(
  layer: Container,
  glyphs: Map<string, Glyph>,
  monsters: MonsterEntity[],
  lockedMonsterId: string | null,
) {
  const alive = new Set<string>();

  for (const monster of monsters) {
    alive.add(monster.id);
    const kind = MONSTER_INK[monster.type] ?? MONSTER_INK.NORMAL;
    const locked = lockedMonsterId === monster.id;
    const fill = locked ? INK.vermilion : INK.cream;
    const fontSize = fontSizeFor(monster.text);

    let glyph = glyphs.get(monster.id);
    if (!glyph) {
      const word = new Text({
        text: monster.text,
        style: {
          fontFamily: INK_FONT_SERIF,
          fontSize,
          fill,
          letterSpacing: 1,
        },
      });
      word.anchor.set(0.5, 0.5);
      const tag = new Text({
        text: kind.label,
        style: {
          fontFamily: INK_FONT_MONO,
          fontSize: 11,
          fill: kind.color,
          letterSpacing: 2.4,
        },
      });
      tag.anchor.set(0.5, 1);
      layer.addChild(word);
      layer.addChild(tag);
      glyph = { word, tag, src: monster.text, fill, fontSize, width: 0 };
      glyphs.set(monster.id, glyph);
    }

    if (glyph.src !== monster.text) {
      glyph.word.text = monster.text;
      glyph.src = monster.text;
      glyph.width = 0;
    }
    if (glyph.fontSize !== fontSize) {
      glyph.word.style.fontSize = fontSize;
      glyph.fontSize = fontSize;
      glyph.width = 0;
    }
    if (glyph.fill !== fill) {
      glyph.word.style.fill = fill;
      glyph.fill = fill;
    }

    glyph.word.position.set(monster.x, monster.y);
    glyph.word.alpha = locked ? 1 : 0.94;
    glyph.tag.text = kind.label;
    glyph.tag.style.fill = kind.color;
    glyph.tag.position.set(monster.x, monster.y - fontSize * 0.92);
    const measured = glyph.word.width;
    glyph.width = measured > 4 ? measured : approxWidth(monster.text, glyph.fontSize);
  }

  for (const [id, glyph] of glyphs) {
    if (alive.has(id)) continue;
    glyph.word.destroy();
    glyph.tag.destroy();
    glyphs.delete(id);
  }
}

function drawStage(
  graphics: Graphics,
  props: GameCanvasProps,
  fx: FxState,
  now: number,
  reduced: boolean,
  glyphs: Map<string, Glyph>,
) {
  graphics.clear();
  const { width, height } = props;

  for (let x = GRID; x < width; x += GRID) {
    graphics.moveTo(x, 0).lineTo(x, height);
  }
  for (let y = GRID; y < height; y += GRID) {
    graphics.moveTo(0, y).lineTo(width, y);
  }
  graphics.stroke({ color: INK.grid, width: 1, alpha: 0.55 });
  graphics.rect(0.5, 0.5, width - 1, height - 1).stroke({ color: INK.grid, width: 1, alpha: 1 });

  for (const monster of props.monsters) {
    if (!monster.isAlive) continue;
    const glyph = glyphs.get(monster.id);
    if (!glyph) continue;
    const locked = props.lockedMonsterId === monster.id;
    const kind = MONSTER_INK[monster.type] ?? MONSTER_INK.NORMAL;
    const span = Math.max(28, glyph.width);
    const baseline = monster.y + glyph.fontSize * 0.86;
    const progress = monster.text.length > 0 ? Math.min(1, monster.currentIndex / monster.text.length) : 0;

    graphics.moveTo(monster.x - span / 2, baseline).lineTo(monster.x + span / 2, baseline);
    graphics.stroke({ color: INK.cream, width: 1, alpha: locked ? 0.45 : 0.16 });

    if (progress > 0) {
      graphics.moveTo(monster.x - span / 2, baseline).lineTo(monster.x - span / 2 + span * progress, baseline);
      graphics.stroke({ color: locked ? INK.vermilion : INK.ochre, width: locked ? 2 : 1.5, alpha: 0.95 });
    }

    graphics.rect(monster.x - span / 2 - 13, monster.y - 3, 5, 5).fill({ color: kind.color, alpha: 0.85 });
  }

  graphics.rect(props.player.x - 1.5, props.player.y - 11, 3, 22).fill({ color: INK.vermilion, alpha: 0.95 });
  graphics.circle(props.player.x, props.player.y, 15).stroke({ color: INK.cream, width: 1, alpha: 0.2 });

  for (const particle of fx.particles) {
    if (!particle.active) continue;
    const alpha = particle.kind === 'dust' ? 0.22 : Math.max(0, particle.life / particle.maxLife);
    graphics.circle(particle.x, particle.y, particle.radius).fill({
      color: particle.kind === 'dust' ? INK.cream : INK.vermilion,
      alpha,
    });
  }
  for (const ripple of fx.ripples) {
    const age = 1 - ripple.life / ripple.maxLife;
    graphics.circle(ripple.x, ripple.y, 8 + age * 28).stroke({ color: INK.cream, width: 1, alpha: (1 - age) * 0.5 });
  }

  const dna = dnaGeometry(props.width, props.height, props.progress01, now, reduced);
  for (const rung of dna.rungs) {
    const active = rung.lit || rung.current;
    graphics.moveTo(rung.x, rung.y1).lineTo(rung.x, rung.y2).stroke({
      color: rung.current ? INK.vermilion : INK.cream,
      width: active ? 2 : 1,
      alpha: rung.current ? 0.95 : active ? 0.55 : 0.16,
    });
  }

  if (props.inputMode === 'CHINESE' && props.chineseBuffer) {
    const width2 = Math.min(320, 28 + props.chineseBuffer.length * 14);
    graphics.rect(20, props.height - 68, width2, 24).fill({ color: INK.cream, alpha: 0.08 });
    graphics.rect(20, props.height - 68, width2, 24).stroke({ color: INK.vermilion, width: 1, alpha: 0.7 });
    graphics.rect(24, props.height - 58, Math.min(width2 - 8, 8 + props.chineseBuffer.length * 12), 6).fill({
      color: INK.vermilion,
      alpha: 0.6,
    });
  }
}
