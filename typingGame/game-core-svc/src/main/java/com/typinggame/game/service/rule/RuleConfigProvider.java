package com.typinggame.game.service.rule;

public interface RuleConfigProvider {
    DifficultyRule activeDifficulty();

    GeneticRule activeGenetic();

    int difficultyVersion();

    int geneticVersion();

    RuleConfigProvider DEFAULTS = new RuleConfigProvider() {
        @Override
        public DifficultyRule activeDifficulty() {
            return new DifficultyRule();
        }

        @Override
        public GeneticRule activeGenetic() {
            return new GeneticRule();
        }

        @Override
        public int difficultyVersion() {
            return 0;
        }

        @Override
        public int geneticVersion() {
            return 0;
        }
    };
}
