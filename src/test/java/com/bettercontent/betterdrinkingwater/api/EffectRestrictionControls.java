package com.bettercontent.betterdrinkingwater.api;

/** Dependency-free executable controls for registration, composition, and persisted-read idempotence. */
public final class EffectRestrictionControls {
    private static int checks;
    private static void check(boolean value, String name) { checks++; if (!value) throw new AssertionError(name); }
    private static void expect(Class<? extends Throwable> type, Runnable action, String name) {
        checks++;
        try { action.run(); } catch (Throwable failure) { if (type.isInstance(failure)) return; throw new AssertionError(name, failure); }
        throw new AssertionError("Expected " + type.getSimpleName() + ": " + name);
    }
    private static EffectRestrictionRegistry.Selected<String> effect(String id, int duration, int amplifier) { return new EffectRestrictionRegistry.Selected<>(id, duration, amplifier); }
    private static EffectRestrictionRegistry.Bounds identity() { return new EffectRestrictionRegistry.Bounds(false, 1_200_000, 31); }
    public static void main(String[] args) {
        var original = effect("speed", 2400, 3);
        var absent = new EffectRestrictionRegistry<String>();
        check(absent.restrict("paid:tea", original).orElseThrow().equals(original), "provider absent is exact identity");
        expect(IllegalStateException.class, () -> absent.register("late", (s, e) -> identity()), "first read freezes");
        var setup = new EffectRestrictionRegistry<String>();
        setup.register("owner", (s, e) -> identity());
        expect(IllegalStateException.class, () -> setup.register("owner", (s, e) -> identity()), "duplicate owner fails");
        setup.freeze(); setup.freeze();
        expect(IllegalStateException.class, () -> setup.register("late", (s, e) -> identity()), "explicit freeze is permanent");
        var invalid = new EffectRestrictionRegistry<String>();
        expect(NullPointerException.class, () -> invalid.register(null, (s, e) -> identity()), "null owner");
        expect(NullPointerException.class, () -> invalid.register("owner", null), "null rule");
        expect(NullPointerException.class, () -> invalid.restrict(null, original), "null source");
        expect(NullPointerException.class, () -> invalid.restrict("paid:tea", null), "null selected");
        expect(NullPointerException.class, () -> effect(null, 20, 0), "null selected ID");
        for (int duration : new int[]{-1, 0, 1_200_001}) expect(IllegalArgumentException.class, () -> effect("speed", duration, 0), "invalid selected duration");
        for (int amp : new int[]{-1, 32}) expect(IllegalArgumentException.class, () -> effect("speed", 20, amp), "invalid selected amplifier");
        for (int duration : new int[]{-1, 1_200_001}) expect(IllegalArgumentException.class, () -> new EffectRestrictionRegistry.Bounds(false, duration, 0), "invalid duration limit");
        for (int amp : new int[]{-1, 32}) expect(IllegalArgumentException.class, () -> new EffectRestrictionRegistry.Bounds(false, 1200, amp), "invalid amplifier limit");
        var caps = new EffectRestrictionRegistry<String>();
        caps.register("culinary", (source, selected) -> selected.id().equals("speed") ? new EffectRestrictionRegistry.Bounds(false, 1200, 0) : identity());
        var bounded = caps.restrict("paid:tea", original).orElseThrow();
        check(bounded.equals(effect("speed", 1200, 0)), "beneficial level I sixty second cap");
        check(caps.restrict("paid:tea", bounded).orElseThrow().equals(bounded), "persisted reread is idempotent");
        check(caps.restrict("paid:tea", effect("speed", 100, 0)).orElseThrow().duration() == 100, "short benefit never increased");
        var harmful = effect("tipsy", 8000, 9);
        check(caps.restrict("paid:booze", harmful).orElseThrow().equals(harmful), "harmful native amplifier and duration preserved");
        var cannotIncrease = new EffectRestrictionRegistry<String>(); cannotIncrease.register("wide", (s, e) -> identity());
        check(cannotIncrease.restrict("paid:tea", effect("speed", 1, 0)).orElseThrow().equals(effect("speed", 1, 0)), "wide rule cannot increase either bound or change ID");
        var compose = new EffectRestrictionRegistry<String>();
        compose.register("duration", (s, e) -> new EffectRestrictionRegistry.Bounds(false, 500, 31));
        compose.register("amplifier", (s, e) -> new EffectRestrictionRegistry.Bounds(false, 1200, 1));
        var reverse = new EffectRestrictionRegistry<String>();
        reverse.register("amplifier", (s, e) -> new EffectRestrictionRegistry.Bounds(false, 1200, 1));
        reverse.register("duration", (s, e) -> new EffectRestrictionRegistry.Bounds(false, 500, 31));
        check(compose.restrict("paid:tea", original).orElseThrow().equals(effect("speed", 500, 1)), "rules compose by minima");
        check(compose.restrict("paid:tea", original).equals(reverse.restrict("paid:tea", original)), "registration order cannot alter restrictions");
        var suppress = new EffectRestrictionRegistry<String>(); suppress.register("off", (s, e) -> new EffectRestrictionRegistry.Bounds(true, 1200, 0));
        check(suppress.restrict("paid:tea", original).isEmpty(), "suppression removes selected effect only");
        var zero = new EffectRestrictionRegistry<String>(); zero.register("zero", (s, e) -> new EffectRestrictionRegistry.Bounds(false, 0, 31));
        check(zero.restrict("paid:tea", original).isEmpty(), "zero max duration suppresses");
        var bad = new EffectRestrictionRegistry<String>(); bad.register("null", (s, e) -> null);
        expect(NullPointerException.class, () -> bad.restrict("paid:tea", original), "null provider result fails before publication");
        var compounding = new EffectRestrictionRegistry<String>(); compounding.register("half", (s, e) -> new EffectRestrictionRegistry.Bounds(false, e.duration() / 2, 31));
        expect(IllegalStateException.class, () -> compounding.restrict("paid:tea", original), "non-idempotent reserve policy rejected");
        var source = new EffectRestrictionRegistry<String>();
        source.register("actual", (id, selected) -> { check(id.equals("paid:actual_variant"), "actual paid source is passed unchanged"); check(selected.equals(original), "immutable selected value passed unchanged"); return identity(); });
        check(source.restrict("paid:actual_variant", original).orElseThrow().id().equals("speed"), "effect ID cannot be substituted");
        System.out.println("Serving effect pure controls PASS " + checks);
    }
}
