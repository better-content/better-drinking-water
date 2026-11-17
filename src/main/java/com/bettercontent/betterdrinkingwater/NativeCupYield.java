package com.bettercontent.betterdrinkingwater;

import java.util.Set;

/** Pinned KC 1.6 native batch output conservation, independent of recipes/quality. */
public final class NativeCupYield {
    private static final String PREFIX = "kaleidoscope_cookery:";
    private static final Set<String> TEAS = Set.of("barley_tea", "tieguanyin", "biluochun", "oolong",
            "sakura_fubuki", "butter_tea", "flower_tea", "mystery_tea");
    private NativeCupYield() {}
    public static boolean isCupId(String id) {
        return id != null && (id.equals(PREFIX + "clay_pot_milk_tea")
                || id.startsWith(PREFIX) && TEAS.contains(id.substring(PREFIX.length())));
    }
    public static void require(String route, String id, int yield) {
        if (!isCupId(id)) throw new IllegalArgumentException("Not a pinned native KC cup: " + id);
        if (route.equals("teapot") && !id.equals(PREFIX + "clay_pot_milk_tea")
                && yield == (id.equals(PREFIX + "mystery_tea") ? 4 : 12)) return;
        if (route.equals("stockpot") && id.equals(PREFIX + "clay_pot_milk_tea") && yield >= 1 && yield <= 9) return;
        throw new IllegalArgumentException("Native KC cup route/yield drift");
    }
    public static double perCup(double wholePaid, int yield) {
        if (!Double.isFinite(wholePaid) || wholePaid < 0 || yield < 1 || yield > 12)
            throw new IllegalArgumentException("Invalid paid native cup quantity");
        return wholePaid / yield;
    }
}
