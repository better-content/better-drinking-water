package com.bettercontent.betterdrinkingwater;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(WaterSurvival.MOD_ID)
public final class WaterSurvival {
    public static final String MOD_ID = "better_drinking_water";

    public WaterSurvival() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        RainCollectorRegistry.BLOCKS.register(bus);
        RainCollectorRegistry.ITEMS.register(bus);
        bus.addListener(this::registerGameTests);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(DrinkCatalogue::validateLoaded));
        MinecraftForge.EVENT_BUS.register(SnowMeltHandler.class);
        MinecraftForge.EVENT_BUS.register(WaterBottleCurio.class);
        MinecraftForge.EVENT_BUS.register(BlockWaterDrinking.class);
        MinecraftForge.EVENT_BUS.register(WaterContainerTooltip.class);
        MinecraftForge.EVENT_BUS.register(WaterSafetyEvents.class);
        WaterBottleCurio.registerPredicate();
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(WaterSurvivalGameTests.class);
        event.register(WaterSourceGameTests.class);
        event.register(BeverageGameTests.class);
    }
}
