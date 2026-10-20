package com.bettercontent.betterdrinkingwater;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.registries.ForgeRegistries;

/** Irreversible actions run only after the paid reserve progress has been committed. */
final class PlantDrinkActions {
    private PlantDrinkActions() {}
    static void release(ServerPlayer player, String action, double from, double to) {
        if (action.equals("mana") && net.minecraftforge.fml.ModList.get().isLoaded("ars_nouveau"))
            Mana.recover(player, 20 * (to - from));
        if (to < 1) return;
        switch (action) {
            case "nausea" -> player.removeEffect(MobEffects.CONFUSION);
            case "orange" -> nativeCompletion(player, "fruitsdelight:recovering");
            case "lemon" -> nativeCompletion(player, "fruitsdelight:refreshing");
            case "blink" -> {
                for (int attempt = 0; attempt < 16; attempt++) {
                    var event = net.minecraftforge.event.ForgeEventFactory.onChorusFruitTeleport(player,
                            player.getX() + (player.getRandom().nextDouble() - .5) * 16,
                            Math.max(player.level().getMinBuildHeight(), Math.min(player.level().getMaxBuildHeight() - 1,
                                    player.getY() + player.getRandom().nextInt(16) - 8)),
                            player.getZ() + (player.getRandom().nextDouble() - .5) * 16);
                    if (event.isCanceled() || player.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true)) break;
                }
            }
            default -> { }
        }
    }
    private static void nativeCompletion(ServerPlayer player, String id) {
        var effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(id));
        if (effect == null) throw new IllegalStateException("Missing completion action effect " + id);
        // Native EffectRemovalEffect performs its exact curated cleansing once on apply. Deliberately
        // not a sliced timed profile: on-apply cleansing must not run on every automatic sip.
        if (player.hasEffect(effect)) effect.addAttributeModifiers(player, player.getAttributes(), 0);
        player.addEffect(new MobEffectInstance(effect, 200, 0));
    }
    private static final class Mana {
        static void recover(ServerPlayer player, double amount) {
            com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player).ifPresent(cap -> {
                cap.addMana(Math.max(0, Math.min(amount, cap.getMaxMana() - cap.getCurrentMana())));
                com.hollingsworth.arsnouveau.common.network.Networking.sendToPlayerClient(
                        new com.hollingsworth.arsnouveau.common.network.PacketUpdateMana(cap.getCurrentMana(),
                                cap.getMaxMana(), cap.getGlyphBonus(), cap.getBookTier()), player);
            });
        }
    }
}
