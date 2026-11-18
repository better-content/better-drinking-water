package com.bettercontent.betterdrinkingwater.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeverageNutritionTest {
    @Test
    void arbitraryPaidPartitionsConserveWholeServingWithoutAmplification() {
        for (double gain : new double[]{.06, .12, .22, .34, 1.0}) {
            double total = 0.0;
            double[] boundaries = {0.0, .01, .15, .333, .77, .99, 1.0};
            for (int index = 1; index < boundaries.length; index++)
                total += BeverageNutrition.award(gain, boundaries[index - 1], boundaries[index]);
            assertEquals(gain, total, 1e-14);
        }
        assertEquals(.006, BeverageNutrition.award(.06, .4, .5), 1e-14);
    }

    @Test
    void invalidEndpointsCannotReleaseAnyNutrition() {
        for (double[] range : new double[][]{{0, 0}, {.5, .4}, {-.1, .5}, {0, 1.1},
                {Double.NaN, 1}, {0, Double.NaN}, {0, Double.POSITIVE_INFINITY},
                {Double.NEGATIVE_INFINITY, 1}})
            assertEquals(0.0, BeverageNutrition.award(.34, range[0], range[1]));
        assertEquals(1.0, BeverageNutrition.fraction(0, 1));
    }

    @Test
    void hostileProfilesAreFiniteAndCannotAmplifyOneServing() {
        for (double gain : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
            assertEquals(0.0, BeverageNutrition.award(gain, 0, 1));
        assertEquals(.25, BeverageNutrition.award(100, .5, .75));
    }
}
