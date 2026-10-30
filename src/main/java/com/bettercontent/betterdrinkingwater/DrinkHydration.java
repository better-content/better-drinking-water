package com.bettercontent.betterdrinkingwater;

/** Per-250mB authored categories. No keyword discovery and no quality multiplier. */
public enum DrinkHydration {
    WATER(4, 5), JUICE_MILK(3, 4), FERMENTED_ALCOHOL(3, 3), SPIRIT(1, 0), MIXED_ALCOHOL(2, 2);
    private final int hydration, quenching;
    DrinkHydration(int hydration, int quenching) { this.hydration = hydration; this.quenching = quenching; }
    public int hydration(int volumeMb) { return hydration * portions(volumeMb); }
    public int quenching(int volumeMb) { return quenching * portions(volumeMb); }
    private static int portions(int volumeMb) {
        if (volumeMb != 250 && volumeMb != 1000) throw new IllegalArgumentException("Unsupported finite serving volume");
        return volumeMb / 250;
    }
}
