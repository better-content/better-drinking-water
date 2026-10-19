package com.bettercontent.betterdrinkingwater;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

/** Fixed two-tier penalties. No compile-time dependency on the optional Industrial Heat provider. */
public final class DirtyWaterEffects {
    private static final ResourceLocation MALNOURISHMENT =
            new ResourceLocation("better_industrial_heat", "malnourishment");

    private DirtyWaterEffects() {}

    public static boolean apply(Player player, int purity) {
        if (!player.level().isClientSide && WaterQuality.normalize(purity) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 1200, 7));
            var malnourishment = ForgeRegistries.MOB_EFFECTS.getValue(MALNOURISHMENT);
            if (malnourishment != null) {
                player.addEffect(new MobEffectInstance(malnourishment, 500, 9));
            }
        }
        // Dirty water still hydrates, independent of Thirst's old chance/debuff config.
        return true;
    }
}
