// 输入处理器
import type { Monster, InputMode, GameEventHandler } from './types';
import { MonsterEntity } from './Monster';

export type InputOutcome = 'correct' | 'wrong' | 'ignore';

export function isTypingKey(event: { key: string; ctrlKey?: boolean; altKey?: boolean; metaKey?: boolean }): boolean {
  if (event.ctrlKey || event.altKey || event.metaKey) return false;
  if (event.key === 'Tab' || event.key === 'Shift' || event.key === 'CapsLock' || event.key === '`' || event.key === 'F2') return false;
  return event.key.length === 1 || event.key === 'Enter';
}

export class InputHandler {
  private inputMode: InputMode;
  private lockedMonster: MonsterEntity | null;
  private chineseBuffer: string;
  private capsLock: boolean;
  private shiftPressed: boolean;
  private eventHandler: GameEventHandler | null;

  constructor(eventHandler: GameEventHandler | null = null) {
    this.inputMode = 'ENGLISH';
    this.lockedMonster = null;
    this.chineseBuffer = '';
    this.capsLock = false;
    this.shiftPressed = false;
    this.eventHandler = eventHandler;
  }

  // 处理键盘输入
  handleKeyDown(event: KeyboardEvent, monsters: MonsterEntity[]): InputOutcome {
    // 处理特殊键
    const special = this.handleSpecialKey(event, monsters);
    if (special) return special;

    // 处理普通字符输入
    return this.handleCharacterInput(event, monsters);
  }

  // 处理特殊键
  private handleSpecialKey(event: KeyboardEvent, monsters: MonsterEntity[]): InputOutcome | null {
    if (event.key === 'Tab') {
      event.preventDefault();
      this.unlockMonster();
      return 'ignore';
    }

    if (event.key === 'Shift') {
      this.shiftPressed = true;
      return 'ignore';
    }

    if (event.key === '`' || event.key === 'F2') {
      event.preventDefault();
      this.toggleInputMode();
      return 'ignore';
    }

    if (event.key === 'CapsLock') {
      this.capsLock = !this.capsLock;
      return 'ignore';
    }

    // 空格或Ctrl：发射中文（如果在中文模式）
    if ((event.key === ' ' || event.ctrlKey) && this.inputMode === 'CHINESE') {
      event.preventDefault();
      return this.fireChineseInput(monsters);
    }

    return null;
  }

  // 处理键盘释放
  handleKeyUp(event: KeyboardEvent): void {
    if (event.key === 'Shift') {
      this.shiftPressed = false;
    }
  }

  // 处理字符输入
  private handleCharacterInput(event: KeyboardEvent, monsters: MonsterEntity[]): InputOutcome {
    if (!isTypingKey(event)) return 'ignore';

    let char = event.key;

    // 处理大小写
    if (this.shouldUpperCase()) {
      char = char.toUpperCase();
    }

    // 中文模式：添加到缓冲区
    if (this.inputMode === 'CHINESE') {
      this.chineseBuffer += char;
      return 'ignore';
    }

    // 英文模式：直接处理
    return this.processInput(char, monsters);
  }

  // 判断是否应该大写
  private shouldUpperCase(): boolean {
    // Caps Lock开启且Shift未按下，或Caps Lock关闭且Shift按下
    return (this.capsLock && !this.shiftPressed) || (!this.capsLock && this.shiftPressed);
  }

  // 发射中文输入
  private fireChineseInput(monsters: MonsterEntity[]): InputOutcome {
    if (!this.chineseBuffer) return 'ignore';

    const result = this.processInput(this.chineseBuffer, monsters);
    this.chineseBuffer = '';
    return result;
  }

  // 处理输入
  private processInput(char: string, monsters: MonsterEntity[]): InputOutcome {
    // 如果有锁定的怪兽，直接攻击
    if (this.lockedMonster) {
      return this.attackMonster(this.lockedMonster, char) ? 'correct' : 'wrong';
    }

    const target = this.findTargetMonster(char, monsters);
    if (target) {
      this.lockMonster(target);
      return this.attackMonster(target, char) ? 'correct' : 'wrong';
    }

    // 没有匹配的怪兽
    this.emitEvent({
      type: 'INPUT_ERROR',
      char,
      expected: '',
    });
    return 'wrong';
  }

  // 查找目标怪兽
  private findTargetMonster(char: string, monsters: MonsterEntity[]): MonsterEntity | null {
    // 查找首字符匹配的存活怪兽
    const candidates = monsters.filter(m => 
      m.isAlive && 
      !m.isLocked && 
      m.text[0] === char
    );

    if (candidates.length === 0) return null;

    // 选择最接近底部的
    return candidates.reduce((closest, current) => 
      current.y > closest.y ? current : closest
    );
  }

  // 锁定怪兽
  private lockMonster(monster: MonsterEntity): void {
    this.lockedMonster = monster;
    monster.isLocked = true;
  }

  // 解锁怪兽
  private unlockMonster(): void {
    if (this.lockedMonster) {
      this.lockedMonster.isLocked = false;
      this.lockedMonster = null;
    }
  }

  // 攻击怪兽
  private attackMonster(monster: MonsterEntity, char: string): boolean {
    const success = monster.handleInput(char);

    if (success) {
      this.emitEvent({
        type: 'MONSTER_HIT',
        monster: monster.toJSON(),
        char,
      });

      // 如果怪兽被消灭
      if (!monster.isAlive) {
        const score = monster.getScore();
        this.emitEvent({
          type: 'MONSTER_KILLED',
          monster: monster.toJSON(),
          score,
        });
        this.unlockMonster();
      }
    } else {
      // 检查是否是回避
      if (monster.type === 'EVASIVE' && monster.wasLastInputEvaded()) {
        this.emitEvent({
          type: 'MONSTER_EVADED',
          monster: monster.toJSON(),
        });
      } else {
        const expected = monster.text[monster.currentIndex];
        this.emitEvent({
          type: 'INPUT_ERROR',
          char,
          expected,
        });
      }
    }

    return success;
  }

  // 切换输入模式
  toggleInputMode(): void {
    this.inputMode = this.inputMode === 'ENGLISH' ? 'CHINESE' : 'ENGLISH';
    this.chineseBuffer = '';
  }

  // 获取输入模式
  getInputMode(): InputMode {
    return this.inputMode;
  }

  // 获取中文缓冲区
  getChineseBuffer(): string {
    return this.chineseBuffer;
  }

  // 获取锁定的怪兽
  getLockedMonster(): MonsterEntity | null {
    return this.lockedMonster;
  }

  // 重置
  reset(): void {
    this.unlockMonster();
    this.chineseBuffer = '';
    this.inputMode = 'ENGLISH';
    this.capsLock = false;
    this.shiftPressed = false;
  }

  // 发送事件
  private emitEvent(event: any): void {
    if (this.eventHandler) {
      this.eventHandler(event);
    }
  }

  // 设置事件处理器
  setEventHandler(handler: GameEventHandler): void {
    this.eventHandler = handler;
  }
}
