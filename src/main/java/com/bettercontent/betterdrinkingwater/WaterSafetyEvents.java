package com.bettercontent.betterdrinkingwater;

import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Observes completion of native thirst and drink actions. */
public final class WaterSafetyEvents {
    private static final String LAST_THIRST = "WaterSurvivalLastObservedThirst";
    private WaterSafetyEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) return;
        player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent(thirst -> {
            var data = player.getPersistentData();
            int previous = data.contains(LAST_THIRST) ? data.getInt(LAST_THIRST) : 20;
            int current = thirst.getThirst();
            data.putInt(LAST_THIRST, current);
            if (revealsOnDrop(previous, current)) WaterSafetyEpisodes.thirstLost(player);
        });
    }

    @SubscribeEvent
    public static void onDrinkFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!WaterBottleCurio.isWaterBottle(event.getItem())) return;
        if (isSafePurity(WaterPurity.getPurity(event.getItem()), WaterPurity.MAX_PURITY)) {
            WaterSafetyEpisodes.purifiedDrunk(player);
        }
    }

    static boolean revealsOnDrop(int previous, int current) { return current >= 0 && current < previous; }
    static boolean isSafePurity(int purity, int maximum) { return purity == maximum; }
}
