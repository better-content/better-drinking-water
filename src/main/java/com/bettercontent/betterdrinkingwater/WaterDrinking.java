package com.bettercontent.betterdrinkingwater;

import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** Shared, paid, bounded hydration credit. Source callbacks must debit before returning a portion. */
final class WaterDrinking {
    private WaterDrinking() {}

    static boolean sip(ServerPlayer player, Supplier<PlayerWaterReserve.Reserve> purchase, int purchaseLimit) {
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElse(null);
        if (thirst == null || thirst.getThirst() >= 20) return false;
        int missing = Math.max(0, 20 - thirst.getThirst());
        var reserve = PlayerWaterReserve.read(player);
        PlayerWaterReserve.write(player, reserve);
        int restored = 0, quenched = 0, purchases = 0;
        boolean purified = false;
        while (missing > 0) {
            if (reserve == null) {
                if (purchases++ >= purchaseLimit) break;
                reserve = purchase.get();
                if (reserve == null) break;
                if (reserve.beverage() == null && !WaterPurity.givePurityEffects(player, reserve.purity())) {
                    reserve = null;
                    break;
                }
                PlayerWaterReserve.write(player, reserve);
            }
            if (reserve.beverage() != null && !BeverageServing.allowed(player)) break;
            var purchased = reserve;
            var result = WaterBottleConsumption.calculate(missing, reserve.thirst(), reserve.quenched(), reserve.consumed());
            restored += result.thirstRestored();
            quenched += result.quenchedRestored();
            missing -= result.thirstRestored();
            purified |= result.thirstRestored() > 0 && reserve.beverage() == null && reserve.purity() == WaterPurity.MAX_PURITY;
            reserve = result.bottleCompleted() ? null : reserve.progress(result.remainingFraction());
            PlayerWaterReserve.write(player, reserve);
            if (purchased.beverage() != null && result.thirstRestored() > 0)
                purchased.beverage().release(player, purchased.consumed(), result.bottleCompleted() ? 1 : result.remainingFraction());
            if (result.thirstRestored() == 0) break;
        }
        if (restored > 0) {
            thirst.drink(player, restored, quenched);
            thirst.updateThirstData(player);
            if (purified) WaterSafetyEpisodes.purifiedDrunk(player);
        }
        return restored > 0;
    }
}
