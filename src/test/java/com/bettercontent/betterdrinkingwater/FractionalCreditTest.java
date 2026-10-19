package com.bettercontent.betterdrinkingwater;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FractionalCreditTest {
    @Test
    void oneThousandTwoHundredTinySipsReleaseEveryTickExactlyOnce() {
        for (int total : new int[]{0, 1, 2, 6, 22, 1200, 72000}) {
            int credited = 0;
            for (int tick = 0; tick < 1200; tick++) {
                int delta = FractionalCredit.delta(total, tick / 1200.0, (tick + 1) / 1200.0);
                assertTrue(delta >= 0, "A forward sip cannot remove credit");
                credited += delta;
                assertEquals((int) Math.floor(total * ((tick + 1) / 1200.0) + 1.0E-9), credited);
            }
            assertEquals(total, credited, "Partitioning cannot create or erase credit");
        }
    }

    @Test
    void twentyTwoQuenchingSurvivesSixOnePointTopOffs() {
        int sum = 0;
        for (int sip = 0; sip < 6; sip++) {
            int expected = new int[]{3, 4, 4, 3, 4, 4}[sip];
            int actual = FractionalCredit.delta(22, sip / 6.0, (sip + 1) / 6.0);
            assertEquals(expected, actual);
            sum += actual;
        }
        assertEquals(22, sum);
    }

    @Test
    void arbitraryPartitionsTelescopeWithoutRoundingGain() {
        Random random = new Random(0xBE7E2A6EL);
        for (int trial = 0; trial < 1000; trial++) {
            int total = random.nextInt(72001);
            double[] boundaries = new double[2 + random.nextInt(100)];
            boundaries[boundaries.length - 1] = 1;
            for (int i = 1; i < boundaries.length - 1; i++) boundaries[i] = random.nextDouble();
            java.util.Arrays.sort(boundaries);
            int sum = 0;
            for (int i = 1; i < boundaries.length; i++) {
                int delta = FractionalCredit.delta(total, boundaries[i - 1], boundaries[i]);
                assertTrue(delta >= 0);
                sum += delta;
                assertTrue(sum <= total, "No prefix may exceed the purchased entitlement");
            }
            assertEquals(total, sum);
        }
    }

    @Test
    void invalidIntervalsRejectRatherThanCreateCredit() {
        assertThrows(IllegalArgumentException.class, () -> FractionalCredit.delta(-1, 0, 1));
        for (double bad : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> FractionalCredit.delta(22, bad, 1));
            assertThrows(IllegalArgumentException.class, () -> FractionalCredit.delta(22, 0, bad));
        }
        assertThrows(IllegalArgumentException.class, () -> FractionalCredit.delta(22, -0.01, 1));
        assertThrows(IllegalArgumentException.class, () -> FractionalCredit.delta(22, 0, 1.01));
        assertThrows(IllegalArgumentException.class, () -> FractionalCredit.delta(22, 0.8, 0.7));
    }

    @Test
    void repeatedBoundaryAndEmptyIntervalsNeverMintCredit() {
        for (double boundary : new double[]{0, 1.0 / 6, 0.5, 0.999, 1}) {
            assertEquals(0, FractionalCredit.delta(1200, boundary, boundary));
        }
        assertEquals(0, FractionalCredit.delta(2, 0, 0.49));
        assertEquals(1, FractionalCredit.delta(2, 0.49, 0.5));
        assertEquals(0, FractionalCredit.delta(2, 0.5, 0.99));
        assertEquals(1, FractionalCredit.delta(2, 0.99, 1));
    }
}
