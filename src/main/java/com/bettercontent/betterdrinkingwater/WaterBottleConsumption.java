package com.bettercontent.betterdrinkingwater;

final class WaterBottleConsumption {
    private static final double EPSILON = 1.0E-9D;

    private WaterBottleConsumption() {
    }

    static Result calculate(
            final int missingThirst,
            final int bottleThirst,
            final int bottleQuenched,
            final double storedFraction) {
        final double fraction = normalizeFraction(storedFraction);
        if (missingThirst <= 0 || bottleThirst <= 0) {
            return new Result(0, 0, false, fraction);
        }

        final int thirstRestored = Math.min(missingThirst, thirstUntilBottleBoundary(fraction, bottleThirst));
        final double cumulativeFraction = fraction + thirstRestored / (double) bottleThirst;
        final int quenchedRestored = Math.max(0,
                quenchedCredit(cumulativeFraction, bottleQuenched) - quenchedCredit(fraction, bottleQuenched));
        final boolean bottleCompleted = cumulativeFraction >= 1.0D - EPSILON;
        final double remainingFraction = bottleCompleted
                ? normalizeFraction(cumulativeFraction - 1.0D)
                : normalizeFraction(cumulativeFraction);
        return new Result(thirstRestored, quenchedRestored, bottleCompleted, remainingFraction);
    }

    static double normalizeFraction(final double fraction) {
        if (!Double.isFinite(fraction) || fraction < 0.0D || fraction >= 1.0D) {
            return 0.0D;
        }
        if (fraction < EPSILON || 1.0D - fraction < EPSILON) {
            return 0.0D;
        }
        return fraction;
    }

    private static int thirstUntilBottleBoundary(final double fraction, final int bottleThirst) {
        return Math.max(1, (int) Math.ceil((1.0D - fraction) * bottleThirst - EPSILON));
    }

    private static int quenchedCredit(final double fraction, final int bottleQuenched) {
        return (int) Math.floor(fraction * Math.max(0, bottleQuenched) + EPSILON);
    }

    record Result(int thirstRestored, int quenchedRestored, boolean bottleCompleted, double remainingFraction) {
    }
}
