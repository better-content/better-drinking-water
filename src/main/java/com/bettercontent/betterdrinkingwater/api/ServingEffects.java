package com.bettercontent.betterdrinkingwater.api;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;
import java.util.Optional;

/** Setup-only effect restrictions for already selected, paid beverage entitlements.
 * No reverse dependency on any policy provider; no registered rules means exact identity.
 * Selection probabilities, groups, native harmful amplifier choices, and payment are upstream.
 */
public final class ServingEffects {
    public record Selected(ResourceLocation id, int duration, int amplifier) {
        public Selected {
            Objects.requireNonNull(id, "Null selected effect ID");
            if (duration < 1 || duration > EffectRestrictionRegistry.MAX_DURATION || amplifier < 0 || amplifier > EffectRestrictionRegistry.MAX_AMPLIFIER)
                throw new IllegalArgumentException("Invalid selected effect entitlement");
        }
    }
    public record Bounds(boolean suppress, int maxDuration, int maxAmplifier) {
        public static final Bounds IDENTITY = new Bounds(false, EffectRestrictionRegistry.MAX_DURATION, EffectRestrictionRegistry.MAX_AMPLIFIER);
        public static final Bounds SUPPRESS = new Bounds(true, 0, 0);
        public Bounds { EffectRestrictionRegistry.validateBounds(maxDuration, maxAmplifier); }
    }
    /** Must be pure, deterministic, server-safe and idempotent under its own restrictions.
     * paidSourceId is the actual persisted serving source, never a fabricated/default item stack.
     * The immutable selected value has already passed all native probability/group/booze draws.
     * Rules cannot change effect identity, increase bounds, debit inventory, or draw random values.
     */
    @FunctionalInterface public interface Rule {
        Bounds restrict(ResourceLocation paidSourceId, Selected selected);
    }
    private static final EffectRestrictionRegistry<ResourceLocation> RULES = new EffectRestrictionRegistry<>();
    private ServingEffects() {}
    /** Register during common setup, before explicit freeze or the first entitlement read/purchase.
     * Duplicate owner IDs, late registration and null providers fail; distinct owners compose by minimum.
     */
    public static void register(ResourceLocation owner, Rule rule) {
        Objects.requireNonNull(rule, "Null effect restriction rule");
        RULES.register(Objects.requireNonNull(owner, "Null effect provider owner"), (source, selected) -> {
            Bounds bounds = Objects.requireNonNull(rule.restrict(source, new Selected(selected.id(), selected.duration(), selected.amplifier())), "Null effect restriction bounds");
            return new EffectRestrictionRegistry.Bounds(bounds.suppress(), bounds.maxDuration(), bounds.maxAmplifier());
        });
    }
    /** Idempotent setup freeze; the first restrict call also freezes to prevent late policy drift. */
    public static void freeze() { RULES.freeze(); }
    /** Read-only entitlement restriction. Empty means suppressed, never a missing/zero-paid serving.
     * Does not initialize an item stack, resolve an effect, roll chance, or touch any payment/cadence ledger.
     */
    public static Optional<Selected> restrict(ResourceLocation paidSourceId, Selected selected) {
        Objects.requireNonNull(selected, "Null selected effect");
        return RULES.restrict(Objects.requireNonNull(paidSourceId, "Null paid source ID"),
                new EffectRestrictionRegistry.Selected<>(selected.id(), selected.duration(), selected.amplifier()))
            .map(effect -> new Selected(effect.id(), effect.duration(), effect.amplifier()));
    }
}
