package com.bettercontent.betterdrinkingwater;

/** Keep Thirst's safe endpoint for compatibility; intermediate water is not purified. */
public final class WaterQuality {
    public static int normalize(int purity) {
        return purity == 3 ? 3 : 0;
    }

    private WaterQuality() {}
}
