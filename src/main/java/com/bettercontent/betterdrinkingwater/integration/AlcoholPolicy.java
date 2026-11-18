package com.bettercontent.betterdrinkingwater.integration;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;

/** Water-owned alcohol effects; the central Physiology attribute/cost combiner consumes
 * these terms without another effect modifier or independent damage subscriber. */
public final class AlcoholPolicy {
    private AlcoholPolicy() {}

    public static double reduction(double alcohol, double carbs) {
        return Math.min(.30, .30 * clamp(alcohol) * amplification(carbs));
    }

    public static double impairment(double alcohol, double carbs) {
        return Math.min(1, Math.max(0, (clamp(alcohol) - .30) / .70) * amplification(carbs));
    }

    public static double clearance(double alcohol, double carbs) {
        int ticks = Math.max(1, (int) Math.round(SalienceConfig.value(
                SalienceConfig.ALCOHOL_CLEAR_MINUTES, 20.0) * 60.0 * 20.0));
        return MetabolicMath.clamp01(alcohol - MetabolicMath.sugarDrain(carbs) / ticks);
    }

    public static boolean blackout(double alcohol) { return alcohol >= 1.0f; }
    public static boolean sober(double alcohol) { return alcohol <= .85f; }
    private static double amplification(double carbs) { return 1 + clamp(carbs); }
    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
