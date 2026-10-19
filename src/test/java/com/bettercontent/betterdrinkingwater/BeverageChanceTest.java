package com.bettercontent.betterdrinkingwater;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeverageChanceTest {
    @Test void complementaryCoinOutcomesAlwaysChooseExactlyOneIncludingTheBoundary() {
        for (double roll : new double[]{0, Math.nextDown(.5), .5, Math.nextUp(.5), Math.nextDown(1.0)})
            assertNotEquals(BeverageChance.selected(roll, .5, 1), BeverageChance.selected(roll, .5, -1));
    }
    @Test void CertainAndImpossibleOutcomesAreExactInEitherGroupDirection() {
        for (int group : new int[]{-1, 0, 1}) for (double roll : new double[]{0, .5, Math.nextDown(1.0)}) {
            assertTrue(BeverageChance.selected(roll, 1, group));
            assertFalse(BeverageChance.selected(roll, 0, group));
        }
    }
    @Test void malformedProbabilitiesNeverGrantAnOutcome() {
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -.1, 1.1}) {
            assertThrows(IllegalArgumentException.class, () -> BeverageChance.selected(invalid, .5, 1));
            assertThrows(IllegalArgumentException.class, () -> BeverageChance.selected(.5, invalid, -1));
        }
    }
}
