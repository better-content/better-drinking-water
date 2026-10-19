package com.bettercontent.betterdrinkingwater;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Nonshipping stand-in: tests cross-mod effect application, not Industrial Heat's drain implementation. */
@Mod.EventBusSubscriber(modid = WaterSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NativeMalnourishmentFixture {
    @SubscribeEvent
    public static void register(RegisterEvent event) {
        if (!ModList.get().isLoaded("better_industrial_heat")) {
            event.register(ForgeRegistries.Keys.MOB_EFFECTS, helper -> helper.register(
                    new ResourceLocation("better_industrial_heat", "malnourishment"), new FixtureEffect()));
        }
    }

    private static final class FixtureEffect extends MobEffect {
        private FixtureEffect() { super(MobEffectCategory.HARMFUL, 0x9B875D); }
    }
}
