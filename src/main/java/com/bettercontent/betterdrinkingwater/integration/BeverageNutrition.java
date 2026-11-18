package com.bettercontent.betterdrinkingwater.integration;

/** Pure bounds shared by profile capture and paid fractional awards. No potency amplification. */
public final class BeverageNutrition {
    private BeverageNutrition() {}

    public static double boundedGain(double gain) {
        return Double.isFinite(gain) && gain > 0.0 ? Math.min(1.0, gain) : 0.0;
    }

    public static double fraction(double from, double to) {
        return Double.isFinite(from) && Double.isFinite(to) && from >= 0.0 && to <= 1.0 && to > from
                ? to - from : 0.0;
    }

    public static double award(double gain, double from, double to) {
        return boundedGain(gain) * fraction(from, to);
    }
}
