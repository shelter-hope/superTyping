import { describe, expect, it } from 'vitest';
import { difficultyRuleSchema, geneticRuleSchema } from './ruleSchema';

const validDifficulty = {
  weightAccuracy: 1,
  weightNormalizedSpeed: 0,
  weightHistorySpeed: 0,
  weightComposure: 1,
  weightRound: 0,
  targetAccuracy: 0.85,
  gain: 0.35,
  step: 0.05,
  minDifficulty: 0.15,
  maxDifficulty: 0.95,
  initialDifficulty: 0.3,
  maxReferenceSpeed: 150,
  refPeakSpeed: 120,
  weakCharDivisor: 10,
  speedSpan: 1,
  countBase: 3,
  countRoundFactor: 6,
  countDifficultyFactor: 16,
  countMin: 3,
  countMax: 25,
  normalBase: 80,
  normalSlope: 0.45,
  normalMin: 35,
  normalMax: 80,
  attackBase: 5,
  attackSlope: 0.15,
  attackMin: 5,
  attackMax: 20,
  evasiveBase: 8,
  evasiveSlope: 0.12,
  evasiveMin: 5,
  evasiveMax: 25,
  roundLogBase: 32,
  highAccuracyAdvanceThreshold: 85,
  lowAccuracyRollbackThreshold: 50,
};

describe('difficultyRuleSchema', () => {
  it('accepts the shipped default weights', () => {
    expect(difficultyRuleSchema.safeParse(validDifficulty).success).toBe(true);
  });

  it('rejects weights that do not sum to one', () => {
    const result = difficultyRuleSchema.safeParse({ ...validDifficulty, weightAccuracy: 0.9 });
    expect(result.success).toBe(false);
  });
});

describe('geneticRuleSchema', () => {
  it('rejects a mutation rate above one', () => {
    const result = geneticRuleSchema.safeParse({
      inRoundParentLimit: 10,
      crossRoundParentLimit: 8,
      inRoundMutationRate: 1.5,
      crossRoundMutationRate: 0.25,
      crossover: 'SINGLE_POINT',
      preferWeakChars: true,
    });
    expect(result.success).toBe(false);
  });
});
