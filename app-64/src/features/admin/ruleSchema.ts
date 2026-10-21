import { z } from 'zod';

const unit = z.number().min(0).max(1);

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
  })
  .refine(
    (rule) =>
      Math.abs(
        rule.weightAccuracy +
          rule.weightNormalizedSpeed +
          rule.weightHistorySpeed +
          rule.weightComposure +
          rule.weightRound -
          1,
      ) < 0.001,
    { message: '权重之和必须为 1' },
  )
  .refine(
    (rule) =>
      rule.minDifficulty < rule.maxDifficulty &&
      rule.initialDifficulty >= rule.minDifficulty &&
      rule.initialDifficulty <= rule.maxDifficulty,
    { message: '难度区间不合法' },
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
