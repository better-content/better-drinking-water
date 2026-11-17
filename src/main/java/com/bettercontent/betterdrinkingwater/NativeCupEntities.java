package com.bettercontent.betterdrinkingwater;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.Objects;

/** Registers only an additional BE factory on KC's existing native block IDs. */
public final class NativeCupEntities {
    private NativeCupEntities() {}
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, WaterSurvival.MOD_ID);
    public static final RegistryObject<BlockEntityType<NativeCupBlockEntity>> CUPS = TYPES.register("native_drink_cups", () -> {
        Block[] blocks = java.util.stream.Stream.of("barley_tea", "tieguanyin", "biluochun", "oolong",
                "sakura_fubuki", "butter_tea", "flower_tea", "mystery_tea", "clay_pot_milk_tea")
                .map(id -> Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("kaleidoscope_cookery", id)),
                        "Missing pinned native cup block " + id)).toArray(Block[]::new);
        return BlockEntityType.Builder.of(NativeCupBlockEntity::new, blocks).build(null);
    });
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
