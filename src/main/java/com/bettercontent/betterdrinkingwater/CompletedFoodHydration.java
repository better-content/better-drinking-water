package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.api.ServingHydration;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Drinking-water owns this ledger, distinct from PlayerWaterReserve. */
public final class CompletedFoodHydration {
    static final String KEY = "better_drinking_water:completed_food";
    private CompletedFoodHydration() {}

    public static boolean complete(ServerPlayer player, long sequence, ServingHydration.Profile profile) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Food completion must run on server thread");
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElse(null);
        if (thirst == null) return false;
        var result = FoodHydrationLedger.complete(read(player), sequence, profile.hydration(), profile.quenching(),
                thirst.getThirst(), thirst.getQuenched());
        if (!result.accepted()) return false;
        // Persist the receipt first. No event/callback can retry this receipt for a second award.
        write(player, result.state());
        thirst.setThirst(thirst.getThirst() + result.hydration());
        thirst.setQuenched(Math.min(thirst.getThirst(), thirst.getQuenched() + result.quenching()));
        thirst.updateThirstData(player);
        return true;
    }

    static FoodHydrationLedger.State read(Player player) {
        var data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);
        if (!data.contains("sequence", Tag.TAG_LONG) || !data.contains("hydration", Tag.TAG_DOUBLE)
                || !data.contains("quenching", Tag.TAG_DOUBLE)) return FoodHydrationLedger.State.EMPTY;
        try { return new FoodHydrationLedger.State(data.getLong("sequence"), data.getDouble("hydration"), data.getDouble("quenching")); }
        catch (IllegalArgumentException invalid) { throw new IllegalStateException("Corrupt completed-food hydration ledger", invalid); }
    }

    static void write(Player player, FoodHydrationLedger.State state) {
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        var data = new CompoundTag();
        data.putLong("sequence", state.sequence()); data.putDouble("hydration", state.hydrationRemainder());
        data.putDouble("quenching", state.quenchingRemainder()); persisted.put(KEY, data);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.getEntity().level().isClientSide) write(event.getEntity(), read(event.getOriginal()));
    }
}
