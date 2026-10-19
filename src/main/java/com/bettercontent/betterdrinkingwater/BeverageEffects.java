package com.bettercontent.betterdrinkingwater;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.bettercontent.betterdrinkingwater.mixin.BeverageEffectAccess;
import com.bettercontent.betterdrinkingwater.mixin.BeverageEffectNotify;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Award only newly consumed ticks, without removing/recreating an existing native effect. */
final class BeverageEffects {
    private BeverageEffects() {}
    static void release(ServerPlayer player, List<DrinkCatalogue.Effect> effects, double from, double to) {
        for (var dose : effects) {
            int earned = FractionalCredit.delta(dose.duration(), from, to);
            if (earned == 0) continue;
            var effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(dose.id()));
            if (effect == null || effect.isInstantenous()) throw new IllegalStateException("Unsupported timed drink effect: " + dose.id());
            var active = player.getEffect(effect);
            if (active != null && (active.isInfiniteDuration() || active.getAmplifier() > dose.amplifier())) continue;
            int duration = active != null && active.getAmplifier() == dose.amplifier()
                    ? Math.max(active.getDuration(), Math.min(dose.duration(), active.getDuration() + earned)) : earned;
            boolean continuing = active != null && active.getAmplifier() == dose.amplifier();
            float absorption = player.getAbsorptionAmount();
            if (continuing) {
                if (duration != active.getDuration()) {
                    ((BeverageEffectAccess) (Object) active).drinks$duration(duration);
                    ((BeverageEffectNotify) player).drinks$updated(active, false, player);
                }
            } else if (!player.addEffect(new MobEffectInstance(effect, duration, dose.amplifier(),
                    active != null && active.isAmbient(), active == null || active.isVisible(), active == null || active.showIcon()))) continue;
            BeverageEffectCadence.manage(player, effect, dose.amplifier(), continuing);
            if (effect == MobEffects.ABSORPTION) {
                // Native on-apply grants a full shield. Replace that onset with this serving's
                // proportional shield entitlement; reapplying short slices cannot mint full shields.
                float cap = 4 * (dose.amplifier() + 1);
                player.setAbsorptionAmount(Math.max(absorption, Math.min(cap, absorption + (float) (cap * (to - from)))));
            }
        }
    }
}
