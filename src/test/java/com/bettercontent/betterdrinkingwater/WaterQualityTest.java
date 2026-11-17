package com.bettercontent.betterdrinkingwater;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaterQualityTest {
    @ParameterizedTest(name = "legacy purity {0} normalizes to {1}")
    @CsvSource({"-1, 0", "0, 0", "1, 0", "2, 0", "3, 3", "4, 0"})
    void onlyTheExactPurifiedEndpointRemainsSafe(int legacy, int expected) {
        assertEquals(expected, WaterQuality.normalize(legacy));
    }
}
