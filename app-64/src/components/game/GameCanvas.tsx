// 游戏画布渲染组件
import { useEffect, useRef } from 'react';
import type { MonsterEntity } from '@/game/Monster';
import type { PlayerState, InputMode } from '@/game/types';

interface GameCanvasProps {
  width: number;
  height: number;
  monsters: MonsterEntity[];
  player: PlayerState;
  inputMode: InputMode;
  chineseBuffer: string;
  lockedMonsterId: string | null;
}

export default function GameCanvas({
  width,
  height,
  monsters,
  player,
  inputMode,
  chineseBuffer,
  lockedMonsterId,
}: GameCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // 清空画布
    ctx.clearRect(0, 0, width, height);

    // 绘制背景
    drawBackground(ctx, width, height);

    // 绘制玩家
    drawPlayer(ctx, player);

    // 绘制怪兽
    monsters.forEach(monster => {
      drawMonster(ctx, monster, lockedMonsterId === monster.id);
      // 绘制怪兽到玩家的方向线（可选，用于调试）
      drawDirectionLine(ctx, monster, player);
    });

    // 绘制输入模式指示器
    drawInputModeIndicator(ctx, width, height, inputMode, chineseBuffer);
  }, [width, height, monsters, player, inputMode, chineseBuffer, lockedMonsterId]);

  return (
    <canvas
      ref={canvasRef}
      width={width}
      height={height}
      className="game-canvas border-2 border-border bg-[hsl(var(--game-bg))] shadow-[inset_0_0_36px_hsl(0_0%_0%/0.65),0_0_26px_hsl(var(--primary)/0.14)]"
    />
  );
}

// 绘制背景
function drawBackground(ctx: CanvasRenderingContext2D, width: number, height: number) {
  const gradient = ctx.createLinearGradient(0, 0, 0, height);
  gradient.addColorStop(0, '#0c1016');
  gradient.addColorStop(0.55, '#090d12');
  gradient.addColorStop(1, '#05070a');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, width, height);

  ctx.strokeStyle = 'rgba(130, 160, 178, 0.09)';
  ctx.lineWidth = 1;

  // 垂直线
  for (let x = 0; x < width; x += 50) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, height);
    ctx.stroke();
  }

  // 水平线
  for (let y = 0; y < height; y += 50) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(width, y);
    ctx.stroke();
  }

  ctx.strokeStyle = 'rgba(154, 184, 202, 0.16)';
  ctx.lineWidth = 2;
  ctx.strokeRect(18, 18, width - 36, height - 36);

  ctx.fillStyle = 'rgba(140, 165, 178, 0.045)';
  for (let y = 0; y < height; y += 8) {
    ctx.fillRect(0, y, width, 1);
  }
}

// 绘制怪兽
function drawMonster(
  ctx: CanvasRenderingContext2D,
  monster: MonsterEntity,
  isLocked: boolean
) {
  const x = monster.x;
  const y = monster.y;
  const size = monster.size;

  // 怪兽颜色
  const colors = {
    NORMAL: '#6f91a4',
    ATTACKER: '#865456',
    EVASIVE: '#6c6f95',
    SMART: '#5f8b8d',
  };

  const color = colors[monster.type];

  ctx.shadowColor = isLocked ? 'rgba(156, 204, 224, 0.42)' : 'rgba(0, 0, 0, 0.45)';
  ctx.shadowBlur = isLocked ? 18 : 8;

  // 绘制怪兽主体（机械核心）
  ctx.beginPath();
  ctx.arc(x, y, 20 * size, 0, Math.PI * 2);
  ctx.fillStyle = color;
  ctx.fill();
  ctx.shadowBlur = 0;

  // 如果被锁定，绘制高亮边框
  if (isLocked) {
    ctx.strokeStyle = '#9ccce0';
    ctx.lineWidth = 3;
    ctx.stroke();
  } else {
    ctx.strokeStyle = '#252d35';
    ctx.lineWidth = 1;
    ctx.stroke();
  }

  // 绘制类型标识（在圆圈内）
  drawTypeIndicator(ctx, x, y, monster.type, size);

  // 绘制字符串（在圆圈上方，不受圆圈大小限制）
  const typedText = monster.getTypedText();
  const remainingText = monster.getRemainingText();

  ctx.font = `bold ${18 * size}px Bahnschrift, monospace`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'bottom';

  // 计算字符串显示位置（圆圈上方）
  const textY = y - 30 * size;

  // 绘制背景框（让字符串更清晰）
  const fullText = typedText + remainingText;
  const textWidth = ctx.measureText(fullText).width;
  const padding = 8;
  
  ctx.fillStyle = 'rgba(4, 7, 10, 0.86)';
  ctx.fillRect(
    x - textWidth / 2 - padding,
    textY - 20 * size,
    textWidth + padding * 2,
    24 * size
  );

  ctx.strokeStyle = 'rgba(156, 204, 224, 0.34)';
  ctx.lineWidth = 1;
  ctx.strokeRect(
    x - textWidth / 2 - padding,
    textY - 20 * size,
    textWidth + padding * 2,
    24 * size
  );

  // 已输入的字符（绿色）
  if (typedText) {
    ctx.fillStyle = '#9ccce0';
    const typedWidth = ctx.measureText(typedText).width;
    const remainingWidth = ctx.measureText(remainingText).width;
    const startX = x - (typedWidth + remainingWidth) / 2;
    ctx.fillText(typedText, startX + typedWidth / 2, textY);
  }

  // 剩余字符（白色）
  if (remainingText) {
    ctx.fillStyle = '#c1c7ca';
    const typedWidth = ctx.measureText(typedText).width;
    const remainingWidth = ctx.measureText(remainingText).width;
    const startX = x - (typedWidth + remainingWidth) / 2;
    ctx.fillText(remainingText, startX + typedWidth + remainingWidth / 2, textY);
  }

  // 绘制血条（在圆圈下方）
  drawHealthBar(ctx, x, y, monster, size);
}

// 绘制类型标识
function drawTypeIndicator(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  type: string,
  size: number
) {
  const icons = {
    NORMAL: 'N',
    ATTACKER: 'A',
    EVASIVE: 'E',
    SMART: 'S',
  };

  ctx.font = `bold ${13 * size}px Bahnschrift, Arial`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillStyle = '#d0d5d8';
  ctx.fillText(icons[type as keyof typeof icons] || '●', x, y);
}

// 绘制血条
function drawHealthBar(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  monster: MonsterEntity,
  size: number
) {
  const barWidth = 60 * size;
  const barHeight = 6 * size;
  const barX = x - barWidth / 2;
  const barY = y + 30 * size;

  const progress = monster.currentIndex / monster.text.length;

  // 背景
  ctx.fillStyle = 'rgba(19, 24, 29, 0.92)';
  ctx.fillRect(barX, barY, barWidth, barHeight);

  // 进度
  ctx.fillStyle = '#9ccce0';
  ctx.fillRect(barX, barY, barWidth * progress, barHeight);

  // 边框
  ctx.strokeStyle = '#36424b';
  ctx.lineWidth = 1;
  ctx.strokeRect(barX, barY, barWidth, barHeight);
}

// 绘制怪兽到玩家的方向线
function drawDirectionLine(
  ctx: CanvasRenderingContext2D,
  monster: MonsterEntity,
  player: PlayerState
) {
  // 只为锁定的怪兽绘制方向线
  if (!monster.isLocked) return;

  ctx.beginPath();
  ctx.moveTo(monster.x, monster.y);
  ctx.lineTo(player.x, player.y);
  ctx.strokeStyle = 'rgba(156, 204, 224, 0.32)';
  ctx.lineWidth = 2;
  ctx.setLineDash([5, 5]);
  ctx.stroke();
  ctx.setLineDash([]);
}

// 绘制玩家
function drawPlayer(ctx: CanvasRenderingContext2D, player: PlayerState) {
  const x = player.x;
  const y = player.y;

  // 绘制玩家（三角形）
  ctx.beginPath();
  ctx.moveTo(x, y - 20);
  ctx.lineTo(x - 15, y + 10);
  ctx.lineTo(x + 15, y + 10);
  ctx.closePath();

  if (player.isAlive) {
    ctx.fillStyle = '#9ccce0';
  } else {
    ctx.fillStyle = '#865456';
  }
  ctx.fill();

  ctx.strokeStyle = '#2c3640';
  ctx.lineWidth = 2;
  ctx.stroke();

  // 绘制玩家碰撞范围（调试用，半透明圆圈）
  ctx.beginPath();
  ctx.arc(x, y, 30, 0, Math.PI * 2);
  ctx.strokeStyle = 'rgba(156, 204, 224, 0.28)';
  ctx.lineWidth = 2;
  ctx.stroke();
}

// 绘制输入模式指示器
function drawInputModeIndicator(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  inputMode: InputMode,
  chineseBuffer: string
) {
  const x = 20;
  const y = height - 60;

  // 绘制背景
  ctx.fillStyle = 'rgba(11, 17, 23, 0.92)';
  ctx.fillRect(x, y, 200, 40);

  // 绘制边框
  ctx.strokeStyle = '#6f91a4';
  ctx.lineWidth = 2;
  ctx.strokeRect(x, y, 200, 40);

  // 绘制模式文本
  ctx.font = 'bold 14px Bahnschrift, Arial';
  ctx.textAlign = 'left';
  ctx.fillStyle = '#c1c7ca';
  ctx.fillText(
    inputMode === 'ENGLISH' ? '英文模式' : '中文模式',
    x + 10,
    y + 15
  );

  // 如果是中文模式，显示缓冲区
  if (inputMode === 'CHINESE' && chineseBuffer) {
    ctx.font = '16px Bahnschrift, monospace';
    ctx.fillText(`输入: ${chineseBuffer}`, x + 10, y + 32);
  } else if (inputMode === 'ENGLISH') {
    ctx.font = '12px Bahnschrift, Arial';
    ctx.fillStyle = 'rgba(193, 199, 202, 0.68)';
    ctx.fillText('按Shift切换中文', x + 10, y + 32);
  } else {
    ctx.font = '12px Bahnschrift, Arial';
    ctx.fillStyle = 'rgba(193, 199, 202, 0.68)';
    ctx.fillText('按空格或Ctrl发射', x + 10, y + 32);
  }
}
