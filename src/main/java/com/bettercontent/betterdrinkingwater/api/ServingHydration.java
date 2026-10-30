package com.bettercontent.betterdrinkingwater.api;

import com.bettercontent.betterdrinkingwater.CompletedFoodHydration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Registration-only integration: consumers depend on drinking-water, never the reverse.
 * Resolvers must be pure, deterministic, server-safe, and return empty for unmanaged stacks.
 * A present ZERO profile deliberately owns (and suppresses native credit for) dry food.
 */
public final class ServingHydration {
    public record Profile(double hydration, double quenching) {
        public static final Profile ZERO = new Profile(0, 0);
        public Profile {
            if (!Double.isFinite(hydration) || !Double.isFinite(quenching)
                    || hydration < 0 || hydration > 64 || quenching < 0 || quenching > 128)
                throw new IllegalArgumentException("Invalid serving hydration");
        }
    }
    @FunctionalInterface
    public interface Resolver {
        Optional<Profile> resolve(ServerPlayer player, ItemStack consumedServing);
    }
    private static final Map<ResourceLocation, Resolver> RESOLVERS = new LinkedHashMap<>();
    private ServingHydration() {}

    /** Register during common setup. Duplicate IDs and ambiguous ownership fail closed. */
    public static synchronized void register(ResourceLocation providerId, Resolver resolver) {
        Objects.requireNonNull(providerId); Objects.requireNonNull(resolver);
        if (RESOLVERS.putIfAbsent(providerId, resolver) != null)
            throw new IllegalStateException("Duplicate hydration provider " + providerId);
    }

    /** Same resolver is available to display and actual intake. Never debits or credits. */
    public static synchronized Optional<Profile> resolve(ServerPlayer player, ItemStack consumedServing) {
        Objects.requireNonNull(player); Objects.requireNonNull(consumedServing);
        if (consumedServing.isEmpty()) return Optional.empty();
        Profile found = null;
        for (var entry : RESOLVERS.entrySet()) {
            var result = Objects.requireNonNull(entry.getValue().resolve(player, consumedServing.copy()), "Null hydration result");
            if (result.isPresent()) {
                if (found != null) throw new IllegalStateException("Ambiguous hydration ownership for " + consumedServing);
                found = result.get();
            }
        }
        return Optional.ofNullable(found);
    }

    /** Call ONLY after a paid, actually completed edible portion (including a placed bite).
     * Caller allocates/persists a positive strictly increasing PER-PLAYER sequence before calling;
     * retries MUST reuse it. Older/equal sequences return false, including after reconnect/clone.
     * Preview/partial/cancelled uses must never call this. No item or drink reserve is created.
     * Excess at full meters is lost. Quality must not scale this profile.
     */
    public static boolean completeFood(ServerPlayer player, long completionSequence, Profile profile) {
        return CompletedFoodHydration.complete(player, completionSequence, Objects.requireNonNull(profile));
    }

    public static boolean completeFood(ServerPlayer player, long completionSequence, ItemStack consumedServing) {
        return resolve(player, consumedServing).map(profile -> completeFood(player, completionSequence, profile)).orElse(false);
    }
}
