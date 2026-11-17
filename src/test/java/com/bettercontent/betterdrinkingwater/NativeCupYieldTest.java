package com.bettercontent.betterdrinkingwater;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeCupYieldTest {
    @Test void pinnedTeaMysteryAndMilkYieldsConserveTheWholePaidBase() {
        for (String tea : new String[]{"barley_tea", "tieguanyin", "biluochun", "oolong",
                "sakura_fubuki", "butter_tea", "flower_tea"}) {
            NativeCupYield.require("teapot", "kaleidoscope_cookery:" + tea, 12);
            assertThrows(IllegalArgumentException.class,
                    () -> NativeCupYield.require("teapot", "kaleidoscope_cookery:" + tea, 4));
        }
        NativeCupYield.require("teapot", "kaleidoscope_cookery:mystery_tea", 4);
        for (int count = 1; count <= 9; count++) {
            NativeCupYield.require("stockpot", "kaleidoscope_cookery:clay_pot_milk_tea", count);
            assertEquals(12, NativeCupYield.perCup(12, count) * count, 1e-10);
        }
        assertEquals(16, NativeCupYield.perCup(16, 12) * 12, 1e-10);
        assertEquals(20, NativeCupYield.perCup(20, 4) * 4, 1e-10);
        assertThrows(IllegalArgumentException.class,
                () -> NativeCupYield.require("stockpot", "kaleidoscope_cookery:clay_pot_milk_tea", 10));
        assertThrows(IllegalArgumentException.class, () -> NativeCupYield.perCup(Double.NaN, 12));
    }

    @Test void noOtherRouteOrUnpaidYieldCanCreateCupEntitlements() {
        assertThrows(IllegalArgumentException.class,
                () -> NativeCupYield.require("stockpot", "kaleidoscope_cookery:barley_tea", 12));
        assertThrows(IllegalArgumentException.class,
                () -> NativeCupYield.require("teapot", "kaleidoscope_cookery:clay_pot_milk_tea", 9));
        assertThrows(IllegalArgumentException.class,
                () -> NativeCupYield.require("teapot", "kaleidoscope_cookery:mystery_tea", 12));
        assertThrows(IllegalArgumentException.class,
                () -> NativeCupYield.require("teapot", "kaleidoscope_cookery:unknown_tea", 12));
        assertThrows(IllegalArgumentException.class, () -> NativeCupYield.perCup(1, 0));
        assertThrows(IllegalArgumentException.class, () -> NativeCupYield.perCup(-1, 4));
    }
}
