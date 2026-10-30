package com.bettercontent.betterdrinkingwater;

/** Pure completed-food accounting. Only sub-unit rounding residue survives, never whole credit. */
public final class FoodHydrationLedger {
    public record State(long sequence, double hydrationRemainder, double quenchingRemainder) {
        public static final State EMPTY = new State(0, 0, 0);
        public State {
            if (sequence < 0 || !fraction(hydrationRemainder) || !fraction(quenchingRemainder))
                throw new IllegalArgumentException("Invalid food hydration state");
        }
    }
    public record Result(State state, int hydration, int quenching, boolean accepted) {}
    private FoodHydrationLedger() {}
    private static boolean fraction(double value) { return Double.isFinite(value) && value >= 0 && value < 1; }

    public static Result complete(State before, long sequence, double hydration, double quenching,
                                  int currentHydration, int currentQuenching) {
        if (sequence <= 0 || !Double.isFinite(hydration) || hydration < 0 || hydration > 64
                || !Double.isFinite(quenching) || quenching < 0 || quenching > 128
                || currentHydration < 0 || currentHydration > 20 || currentQuenching < 0 || currentQuenching > 20)
            throw new IllegalArgumentException("Invalid completed food hydration");
        if (sequence <= before.sequence()) return new Result(before, 0, 0, false);
        double h = before.hydrationRemainder() + hydration;
        double q = before.quenchingRemainder() + quenching;
        int wholeH = (int) Math.floor(h + 1e-9), wholeQ = (int) Math.floor(q + 1e-9);
        int awardedH = Math.min(20 - currentHydration, wholeH);
        int awardedQ = Math.min(Math.max(0, currentHydration + awardedH - currentQuenching), wholeQ);
        // Native quenched is bounded by thirst, not an independent 20-point reservoir.
        // Clear rounding residue at the actual full meter, not the temporary quenched<=thirst
        // bound. At thirst0 two .5/.625 servings must still combine to1/1, not lose the first .625.
        // Whole overflow is always discarded; only subunit rounding residue can survive.
        double remainderH = currentHydration + awardedH >= 20 ? 0 : Math.max(0, h - wholeH);
        double remainderQ = currentQuenching + awardedQ >= 20 ? 0 : Math.max(0, q - wholeQ);
        return new Result(new State(sequence, remainderH, remainderQ), awardedH, awardedQ, true);
    }
}
