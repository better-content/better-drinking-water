package com.bettercontent.betterdrinkingwater.integration;

/** Only the captured canonical dose, NOT an HP budget or food/provenance copy.
 * Typed Water v1 permits bounded numeric diet-map entries, not new top-level fields.
 * A bounded mantissa/exponent pair preserves every finite nonnegative IEEE754 dose.
 * Each actual paid portion resolves units*fraction, then applies its own gain/HP cap. */
public final class BeverageSpiceDose {
    public static final String MANTISSA = "better_spicy_farmers:units_mantissa";
    public static final String EXPONENT = "better_spicy_farmers:units_exponent";
    private BeverageSpiceDose() {}
    public static double mantissa(double units) {
        validateUnits(units);
        return units == 0 ? 0 : Math.scalb(units, -Math.getExponent(units)) / 2;
    }
    public static double exponent(double units) {
        validateUnits(units);
        return units == 0 ? 0 : (Math.getExponent(units) + 1023) / 2046.0;
    }
    public static double decode(double mantissa, double exponent) {
        bounded(mantissa); bounded(exponent);
        double units = mantissa == 0 ? 0 : Math.scalb(2 * mantissa, (int) Math.round(exponent * 2046) - 1023);
        validateUnits(units);
        return units;
    }
    private static void validateUnits(double units) {
        if (!Double.isFinite(units) || units < 0) throw new IllegalArgumentException("Invalid captured Spice dose");
    }
    private static void bounded(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("Invalid captured Spice component");
    }
}
