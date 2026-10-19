package com.bettercontent.betterdrinkingwater;

/** Cumulative integer entitlement; never round each sip independently. */
final class FractionalCredit {
    private FractionalCredit() {}
    static int delta(int total, double from, double to) {
        if (total < 0 || !Double.isFinite(from) || !Double.isFinite(to) || from < 0 || to > 1 || from > to)
            throw new IllegalArgumentException("Invalid fractional entitlement");
        return credit(total, to) - credit(total, from);
    }
    private static int credit(int total, double fraction) {
        return Math.min(total, (int) Math.floor(total * fraction + 1.0e-9));
    }
}
