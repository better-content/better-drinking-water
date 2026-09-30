package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.WaterSurvival;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RainCollectorRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, WaterSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WaterSurvival.MOD_ID);

    public static final RegistryObject<Block> RAIN_COLLECTOR = BLOCKS.register("rain_collector", () ->
            new RainCollectorBlock(BlockBehaviour.Properties.of()
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Item> RAIN_COLLECTOR_ITEM = ITEMS.register("rain_collector", () ->
            new BlockItem(RAIN_COLLECTOR.get(), new Item.Properties()));

    private RainCollectorRegistry() {
    }
}
