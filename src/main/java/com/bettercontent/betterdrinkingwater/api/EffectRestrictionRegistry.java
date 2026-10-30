package com.bettercontent.betterdrinkingwater.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Pure deterministic restriction algebra, independent of game objects, registries, and probability draws. */
final class EffectRestrictionRegistry<K> {
    static final int MAX_DURATION = 1_200_000;
    static final int MAX_AMPLIFIER = 31;
    record Selected<K>(K id, int duration, int amplifier) {
        Selected {
            Objects.requireNonNull(id, "Null selected effect ID");
            if (duration < 1 || duration > MAX_DURATION || amplifier < 0 || amplifier > MAX_AMPLIFIER)
                throw new IllegalArgumentException("Invalid selected effect entitlement");
        }
    }
    record Bounds(boolean suppress, int maxDuration, int maxAmplifier) {
        Bounds { validateBounds(maxDuration, maxAmplifier); }
    }
    @FunctionalInterface interface Rule<K> { Bounds restrict(K paidSource, Selected<K> selected); }
    private final Map<K, Rule<K>> rules = new LinkedHashMap<>();
    private boolean frozen;
    static void validateBounds(int duration, int amplifier) {
        if (duration < 0 || duration > MAX_DURATION || amplifier < 0 || amplifier > MAX_AMPLIFIER)
            throw new IllegalArgumentException("Invalid serving effect restriction bounds");
    }
    synchronized void register(K owner, Rule<K> rule) {
        Objects.requireNonNull(owner, "Null effect provider owner"); Objects.requireNonNull(rule, "Null effect restriction rule");
        if (frozen) throw new IllegalStateException("Serving effect restrictions are frozen");
        if (rules.putIfAbsent(owner, rule) != null) throw new IllegalStateException("Duplicate serving effect provider " + owner);
    }
    synchronized void freeze() { frozen = true; }
    synchronized Optional<Selected<K>> restrict(K source, Selected<K> selected) {
        Objects.requireNonNull(source, "Null paid source ID"); Objects.requireNonNull(selected, "Null selected effect");
        frozen = true;
        Optional<Selected<K>> restricted = apply(source, selected);
        // Old paid reserves are re-read, not rerolled. Reject providers whose bounds compound on re-read.
        if (restricted.isPresent() && !restricted.get().equals(selected) && !restricted.equals(apply(source, restricted.get())))
            throw new IllegalStateException("Non-idempotent serving effect restriction provider");
        return restricted;
    }
    private Optional<Selected<K>> apply(K source, Selected<K> selected) {
        int duration = selected.duration(), amplifier = selected.amplifier();
        boolean suppressed = false;
        for (var entry : rules.entrySet()) {
            // Every provider sees the same selected entitlement; composition cannot depend on owner order.
            Bounds bounds = Objects.requireNonNull(entry.getValue().restrict(source, selected), "Null effect restriction from " + entry.getKey());
            suppressed |= bounds.suppress(); duration = Math.min(duration, bounds.maxDuration()); amplifier = Math.min(amplifier, bounds.maxAmplifier());
        }
        return suppressed || duration == 0 ? Optional.empty() : Optional.of(new Selected<>(selected.id(), duration, amplifier));
    }
}
