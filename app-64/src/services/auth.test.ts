import { describe, expect, it } from 'vitest';
import { resolveUserId } from '@/services/auth';

describe('resolveUserId', () => {
  it('优先取新接口的 userId', () => {
    expect(resolveUserId({ userId: 12, id: 34 })).toBe(12);
  });

  it('兼容旧接口的 id', () => {
    expect(resolveUserId({ id: 34 })).toBe(34);
  });

  it('两者都没有时返回 undefined', () => {
    expect(resolveUserId({ username: 'test' })).toBeUndefined();
  });

  it('非数字 id 视为缺失，避免拼出 /user/undefined', () => {
    expect(resolveUserId({ userId: '12' })).toBeUndefined();
  });
});
