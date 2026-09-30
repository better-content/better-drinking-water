package com.bettercontent.betterdrinkingwater;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterBottleConsumptionTest {
    private static final double TOLERANCE = 1.0E-9D;

    @Test
    void sixOnePointTopOffsConsumeOneSixPointBottle() {
        double fraction = 0.0D;
        int quenched = 0;

        for (int use = 1; use <= 6; use++) {
            final WaterBottleConsumption.Result result = WaterBottleConsumption.calculate(1, 6, 8, fraction);
            assertEquals(1, result.thirstRestored());
            quenched += result.quenchedRestored();
            fraction = result.remainingFraction();
            assertEquals(use == 6, result.bottleCompleted());
        }

        assertEquals(8, quenched);
        assertEquals(0.0D, fraction, TOLERANCE);
    }

    @Test
    void absentBottleLeavesStoredFractionUntouched() {
        final WaterBottleConsumption.Result result = WaterBottleConsumption.calculate(0, 6, 8, 5.0D / 6.0D);

        assertEquals(0, result.thirstRestored());
        assertFalse(result.bottleCompleted());
        assertEquals(5.0D / 6.0D, result.remainingFraction(), TOLERANCE);
    }

    @Test
    void calculationStopsAtTheCurrentBottleBoundary() {
        final WaterBottleConsumption.Result result = WaterBottleConsumption.calculate(20, 6, 8, 0.0D);

        assertEquals(6, result.thirstRestored());
        assertEquals(8, result.quenchedRestored());
        assertTrue(result.bottleCompleted());
        assertEquals(0.0D, result.remainingFraction(), TOLERANCE);
    }

    @Test
    void nearBoundaryCarriesOnlyRoundingOvershoot() {
        final WaterBottleConsumption.Result result = WaterBottleConsumption.calculate(2, 6, 8, 0.9D);

        assertEquals(1, result.thirstRestored());
        assertTrue(result.bottleCompleted());
        assertEquals(1.0D / 15.0D, result.remainingFraction(), TOLERANCE);
    }

    @Test
    void invalidPersistedFractionsResetSafely() {
        assertEquals(0.0D, WaterBottleConsumption.normalizeFraction(Double.NaN));
        assertEquals(0.0D, WaterBottleConsumption.normalizeFraction(Double.POSITIVE_INFINITY));
        assertEquals(0.0D, WaterBottleConsumption.normalizeFraction(-0.1D));
        assertEquals(0.0D, WaterBottleConsumption.normalizeFraction(1.0D));
    }
}
