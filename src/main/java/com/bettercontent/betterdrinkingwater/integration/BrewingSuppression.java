package com.bettercontent.betterdrinkingwater.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import umpaz.brewinandchewin.common.capability.TipsyNumbedHeartsCapability;

/** Water's beverage compatibility owns the pre-existing no-second-heart-buffer rule. */
public final class BrewingSuppression {
    private BrewingSuppression() {}

    public static void suppressNumbedHearts(ServerPlayer player) {
        if (!ModList.get().isLoaded("brewinandchewin")) return;
        player.getCapability(TipsyNumbedHeartsCapability.INSTANCE).ifPresent(capability -> {
            if (capability.getNumbedHealth() == 0.0f && capability.getTicksUntilDamage() == 0) return;
            capability.setNumbedHealth(0.0f);
            capability.setTicksUntilDamage(0);
            capability.syncToPlayer(player);
        });
    }
}
