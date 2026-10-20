package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.diagnostics.PlaytestLog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Process-local observations only: no paid reserve, network, RNG or inventory ownership. */
@Mod.EventBusSubscriber(modid = WaterSurvival.MOD_ID)
public final class WaterDiagnostics {
    static final PlaytestLog LOG = new PlaytestLog(WaterSurvival.MOD_ID);
    private static final java.util.concurrent.atomic.AtomicLong OPERATIONS = new java.util.concurrent.atomic.AtomicLong();
    private WaterDiagnostics() {}
    static long begin() { return LOG.enabled("transaction") ? OPERATIONS.incrementAndGet() : 0; }
    static PlaytestLog.Context context(ServerPlayer player) {
        return PlaytestLog.Context.server(player.serverLevel().dimension().location().toString(), player.serverLevel().getGameTime());
    }

    static void outcome(ServerPlayer player, long operation, String event, String result, String reason,
                        int restored, int quenched, int purchases) {
        if (!LOG.enabled("transaction")) return;
        com.bettercontent.betterdrinkingwater.diagnostics.SipObservation.emit(LOG,
            context(player), operation, event, result, reason, restored, quenched, purchases);
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) LOG.summary("server");
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { LOG.reset("server"); }
}
