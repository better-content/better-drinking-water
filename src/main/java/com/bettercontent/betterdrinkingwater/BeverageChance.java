package com.bettercontent.betterdrinkingwater;

/** Signed groups share a roll; negative groups select its complementary interval. */
final class BeverageChance {
    private BeverageChance() {}
    static boolean selected(double roll, double chance, int group) {
        if (!Double.isFinite(roll) || roll < 0 || roll >= 1 || !Double.isFinite(chance) || chance < 0 || chance > 1)
            throw new IllegalArgumentException("Invalid serving chance");
        return group < 0 ? roll >= 1 - chance : roll < chance;
    }
}
