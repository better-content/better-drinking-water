package com.bettercontent.betterdrinkingwater;

import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** Shared, paid, bounded hydration credit. Source callbacks must debit before returning a portion. */
final class WaterDrinking {
    private WaterDrinking() {}

    static boolean sip(ServerPlayer player, Supplier<PlayerWaterReserve.Reserve> purchase, int purchaseLimit) {
        long operation = WaterDiagnostics.begin();
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElse(null);
        if (thirst == null || thirst.getThirst() >= 20) {
            WaterDiagnostics.outcome(player, operation, "sip.outcome", "rejected", thirst == null ? "no_capability" : "full_thirst", 0, 0, 0);
            return false;
        }
        int missing = Math.max(0, 20 - thirst.getThirst());
        int thirstBefore = 20 - missing;
        var reserve = PlayerWaterReserve.read(player);
        PlayerWaterReserve.write(player, reserve);
        int restored = 0, quenched = 0, purchases = 0;
        boolean purified = false;
        String stopReason = "thirst_satisfied";
        while (missing > 0) {
            if (reserve == null) {
                if (purchases++ >= purchaseLimit) { stopReason = "purchase_limit"; break; }
                reserve = purchase.get();
                if (reserve == null) { stopReason = "source_unavailable"; break; }
                WaterDiagnostics.outcome(player, operation, "reserve.purchase", "admitted", "paid_portion_returned", 0, 0, purchases);
                if (reserve.beverage() == null && !WaterPurity.givePurityEffects(player, reserve.purity())) {
                    reserve = null;
                    stopReason = "purity_veto";
                    break;
                }
                PlayerWaterReserve.write(player, reserve);
            }
            if (reserve.beverage() != null && !BeverageServing.allowed(player)) { stopReason = "beverage_veto"; break; }
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
            if (WaterDiagnostics.LOG.traceEnabled("fraction")) {
                var context = new com.bettercontent.betterdrinkingwater.diagnostics.PlaytestLog.Context("server",
                    player.serverLevel().dimension().location().toString(), player.serverLevel().getGameTime(), player.getUUID().toString(), "");
                WaterDiagnostics.LOG.trace("fraction", "reserve.fraction", context, "observed", result.bottleCompleted() ? "completed" : "partial",
                    () -> com.bettercontent.betterdrinkingwater.diagnostics.PlaytestLog.fields("operation", operation,
                        "from", purchased.consumed(), "to", result.bottleCompleted() ? 1 : result.remainingFraction()));
            }
            if (result.thirstRestored() == 0) { stopReason = "no_fraction_progress"; break; }
        }
        if (restored > 0) {
            thirst.drink(player, restored, quenched);
            thirst.updateThirstData(player);
            if (purified) WaterSafetyEpisodes.purifiedDrunk(player);
        }
        if (WaterDiagnostics.LOG.enabled("transaction")) {
            int thirstAfter = thirst.getThirst();
            WaterDiagnostics.LOG.debug("transaction", "hydration.observed", WaterDiagnostics.context(player), "observed", "native_meter",
                () -> com.bettercontent.betterdrinkingwater.diagnostics.PlaytestLog.fields("operation", operation, "before", thirstBefore, "after", thirstAfter));
        }
        WaterDiagnostics.outcome(player, operation, "sip.outcome", restored > 0 ? "released" : "rejected", stopReason, restored, quenched, purchases);
        return restored > 0;
    }
}
