package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.api.ServingHydration;
import com.bettercontent.betterspicyfarmers.api.event.NativePlacedServingEvent;
import dev.ghen.thirst.api.ThirstHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/** Existing Water ledger is the ONE persisted sequence authority for paid food and cups.
 * Thirst's native ordinary edible path remains authoritative when it already supplies water. */
public final class NativeFoodHydration {
    private static final Map<Object, Boolean> COMMITTED = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<Object, Boolean> eldest) { return size() > 4096; }
    };
    private NativeFoodHydration() {}
    public static void register() {
        MinecraftForge.EVENT_BUS.register(NativeFoodHydration.class);
        if (ModList.get().isLoaded("better_spicy_farmers")) MinecraftForge.EVENT_BUS.register(Placed.class);
    }
    private static boolean eligible(ServerPlayer player) {
        return !(player instanceof FakePlayer) && player.connection != null && !player.hasDisconnected()
                && player.server.isSameThread();
    }
    private static synchronized boolean claim(Object actualCompletion) {
        return COMMITTED.putIfAbsent(actualCompletion, Boolean.TRUE) == null;
    }
    @SubscribeEvent public static void finished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !eligible(player)) return;
        ItemStack finished = event.getItem();
        if (finished.isEmpty() || !finished.isEdible() || WaterSources.nativeHydration(finished).isPresent()
                || ThirstHelper.itemRestoresThirst(finished)) return;
        var profile = ServingHydration.resolve(player, finished);
        if (profile.isPresent() && claim(event)) CompletedFoodHydration.completeNativeCup(player, profile.get());
    }
    /** Spice posts this only AFTER KC's actual placed FoodData.eat AND block debit.
     * Native Thirst's Player.eat interception is not called for KC's placed-food path. */
    private static final class Placed {
    @SubscribeEvent public static void placed(NativePlacedServingEvent event) {
        ServerPlayer player = event.player();
        if (!eligible(player)) return;
        ItemStack serving = event.serving();
        var profile = ServingHydration.resolve(player, serving);
        if (profile.isEmpty() && ThirstHelper.itemRestoresThirst(serving)) {
            int h = ThirstHelper.getThirst(serving), q = ThirstHelper.getQuenched(serving);
            if (h >= 0 && h <= 64 && q >= 0 && q <= 128)
                profile = java.util.Optional.of(new ServingHydration.Profile(h, q));
        }
        if (profile.isEmpty() || !claim(event)) return;
        var whole = profile.get();
        CompletedFoodHydration.completeNativeCup(player, new ServingHydration.Profile(
                whole.hydration() * event.fraction(), whole.quenching() * event.fraction()));
    }
    }
    @SubscribeEvent public static synchronized void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) { COMMITTED.clear(); }
    @SubscribeEvent public static synchronized void stopped(ServerStoppedEvent event) { COMMITTED.clear(); }
}
