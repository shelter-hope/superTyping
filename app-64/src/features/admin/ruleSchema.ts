import { z } from 'zod';

const unit = z.number().min(0).max(1);
const percent = z.number().min(0).max(100);

export const difficultyRuleSchema = z
  .object({
    weightAccuracy: unit,
    weightNormalizedSpeed: unit,
    weightHistorySpeed: unit,
    weightComposure: unit,
    weightRound: unit,
    targetAccuracy: unit,
    gain: z.number().min(0).max(2),
    step: z.number().min(0).max(1),
    minDifficulty: unit,
    maxDifficulty: unit,
    initialDifficulty: unit,
    maxReferenceSpeed: z.number().positive(),
    refPeakSpeed: z.number().positive(),
    weakCharDivisor: z.number().positive(),
    speedSpan: z.number().positive(),
    countBase: z.number(),
    countRoundFactor: z.number(),
    countDifficultyFactor: z.number(),
    countMin: z.number().int().min(1),
    countMax: z.number().int().min(1),
    normalBase: z.number().int().min(0).max(100),
    normalSlope: z.number().min(0),
    normalMin: z.number().int().min(0).max(100),
    normalMax: z.number().int().min(0).max(100),
    attackBase: z.number().int().min(0).max(100),
    attackSlope: z.number().min(0),
    attackMin: z.number().int().min(0).max(100),
    attackMax: z.number().int().min(0).max(100),
    evasiveBase: z.number().int().min(0).max(100),
    evasiveSlope: z.number().min(0),
    evasiveMin: z.number().int().min(0).max(100),
    evasiveMax: z.number().int().min(0).max(100),
    roundLogBase: z.number().int().min(2),
    highAccuracyAdvanceThreshold: percent,
    lowAccuracyRollbackThreshold: percent,
  })
  .refine(
    (rule) =>
      Math.abs(rule.weightAccuracy + rule.weightNormalizedSpeed + rule.weightHistorySpeed - 1) < 0.001,
    { message: '准确率、速度、历史峰值权重之和必须为 1' },
  )
  .refine(
    (rule) =>
      rule.minDifficulty < rule.maxDifficulty &&
      rule.initialDifficulty >= rule.minDifficulty &&
      rule.initialDifficulty <= rule.maxDifficulty,
    { message: '难度区间不合法' },
  )
  .refine((rule) => rule.countMin <= rule.countMax, { message: '怪物数量区间不合法' })
  .refine((rule) => rule.normalMin <= rule.normalMax, { message: '普通型占比区间不合法' })
  .refine((rule) => rule.attackMin <= rule.attackMax, { message: '攻击型占比区间不合法' })
  .refine((rule) => rule.evasiveMin <= rule.evasiveMax, { message: '回避型占比区间不合法' })
  .refine(
    (rule) => rule.lowAccuracyRollbackThreshold < rule.highAccuracyAdvanceThreshold,
    { message: '低准确率回退阈值必须小于高准确率推进阈值' },
  );

export const geneticRuleSchema = z.object({
  inRoundParentLimit: z.number().int().min(1).max(100),
  crossRoundParentLimit: z.number().int().min(1).max(100),
  inRoundMutationRate: unit,
  crossRoundMutationRate: unit,
  crossover: z.string().min(1),
  preferWeakChars: z.boolean(),
});

export type DifficultyRuleForm = z.infer<typeof difficultyRuleSchema>;
export type GeneticRuleForm = z.infer<typeof geneticRuleSchema>;

export const difficultyFieldGroups: Array<{
  title: string;
  fields: Array<{ key: keyof DifficultyRuleForm; label: string; step?: string }>;
}> = [
  {
    title: '调节器权重（前三项之和为 1）',
    fields: [
      { key: 'weightAccuracy', label: '准确率权重' },
      { key: 'weightNormalizedSpeed', label: '速度偏差权重' },
      { key: 'weightHistorySpeed', label: '峰值偏差权重' },
      { key: 'weightComposure', label: '易错强化系数' },
      { key: 'weightRound', label: '轮次保留（不参与计算）' },
      { key: 'targetAccuracy', label: '目标准确率' },
      { key: 'gain', label: '增益' },
      { key: 'step', label: '步长' },
    ],
  },
  {
    title: '难度边界',
    fields: [
      { key: 'minDifficulty', label: '下界' },
      { key: 'maxDifficulty', label: '上界' },
      { key: 'initialDifficulty', label: '初始难度' },
    ],
  },
  {
    title: '参考量',
    fields: [
      { key: 'maxReferenceSpeed', label: '最大参考速度', step: '1' },
      { key: 'refPeakSpeed', label: '参考峰值速度', step: '1' },
      { key: 'weakCharDivisor', label: '弱项字符除数', step: '1' },
      { key: 'roundLogBase', label: '轮次对数底', step: '1' },
      { key: 'speedSpan', label: '速度覆盖比例' },
    ],
  },
  {
    title: '怪物数量',
    fields: [
      { key: 'countBase', label: '数量基数', step: '0.1' },
      { key: 'countRoundFactor', label: '轮次增量', step: '0.1' },
      { key: 'countDifficultyFactor', label: '难度增量', step: '0.1' },
      { key: 'countMin', label: '数量下限', step: '1' },
      { key: 'countMax', label: '数量上限', step: '1' },
    ],
  },
  {
    title: '普通型占比',
    fields: [
      { key: 'normalBase', label: '基数', step: '1' },
      { key: 'normalSlope', label: '斜率' },
      { key: 'normalMin', label: '下限', step: '1' },
      { key: 'normalMax', label: '上限', step: '1' },
    ],
  },
  {
    title: '攻击型占比',
    fields: [
      { key: 'attackBase', label: '基数', step: '1' },
      { key: 'attackSlope', label: '斜率' },
      { key: 'attackMin', label: '下限', step: '1' },
      { key: 'attackMax', label: '上限', step: '1' },
    ],
  },
  {
    title: '回避型占比',
    fields: [
      { key: 'evasiveBase', label: '基数', step: '1' },
      { key: 'evasiveSlope', label: '斜率' },
      { key: 'evasiveMin', label: '下限', step: '1' },
      { key: 'evasiveMax', label: '上限', step: '1' },
    ],
  },
  {
    title: '触发阈值（准确率 %）',
    fields: [
      { key: 'highAccuracyAdvanceThreshold', label: '高准确率推进', step: '1' },
      { key: 'lowAccuracyRollbackThreshold', label: '低准确率回退', step: '1' },
    ],
  },
];
