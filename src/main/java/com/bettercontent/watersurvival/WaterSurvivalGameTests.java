package com.bettercontent.watersurvival;

import com.bettercontent.watersurvival.WaterSurvival;
import com.mojang.authlib.GameProfile;
import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@PrefixGameTestTemplate(false)
public final class WaterSurvivalGameTests {
    private WaterSurvivalGameTests() {
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void exposedCollectorFillsOneChargePerPulse(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos relativePos = new BlockPos(2, 200, 2);
        final BlockPos worldPos = helper.absolutePos(relativePos);
        makeBiomeRainy(level, worldPos);
        level.setWeatherParameters(0, 1200, true, false);
        level.setRainLevel(1.0F);
        final BlockState state = RainCollectorRegistry.RAIN_COLLECTOR.get().defaultBlockState();
        helper.setBlock(relativePos, state);
        RainCollectorRegistry.RAIN_COLLECTOR.get().tick(state, level, worldPos, RandomSource.create(1L));
        helper.assertBlockProperty(relativePos, RainCollectorBlock.LEVEL, 1);
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void coveredCollectorDoesNotFill(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos relativePos = new BlockPos(2, 200, 2);
        final BlockPos worldPos = helper.absolutePos(relativePos);
        level.setWeatherParameters(0, 1200, true, false);
        level.setRainLevel(1.0F);
        final BlockState state = RainCollectorRegistry.RAIN_COLLECTOR.get().defaultBlockState();
        helper.setBlock(relativePos, state);
        helper.setBlock(relativePos.above(2), Blocks.STONE);
        RainCollectorRegistry.RAIN_COLLECTOR.get().tick(state, level, worldPos, RandomSource.create(2L));
        helper.assertBlockProperty(relativePos, RainCollectorBlock.LEVEL, 0);
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 180)
    public static void snowInCampfireCubeMeltsWithoutExtinguishing(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos campfirePos = new BlockPos(3, 3, 3);
        final BlockPos lowerCorner = campfirePos.offset(-1, -1, -1);
        final BlockPos upperCorner = campfirePos.offset(1, 1, 1);
        final BlockPos directlyAbove = campfirePos.above();
        final BlockPos outside = campfirePos.offset(2, 0, 0);
        helper.setBlock(lowerCorner, Blocks.SNOW_BLOCK);
        helper.setBlock(upperCorner, Blocks.SNOW_BLOCK);
        helper.setBlock(directlyAbove, Blocks.SNOW_BLOCK);
        helper.setBlock(outside, Blocks.SNOW_BLOCK);
        // The one-block template is embedded underground. Cap melt targets so random overhead
        // gravel cannot fall into the resulting water before the delayed assertion runs.
        helper.setBlock(lowerCorner.above(), Blocks.STONE);
        helper.setBlock(upperCorner.above(), Blocks.STONE);
        helper.setBlock(directlyAbove.above(), Blocks.STONE);
        helper.setBlock(campfirePos, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        SnowMeltHandler.scheduleAroundCampfire(level, helper.absolutePos(campfirePos));
        helper.runAfterDelay(125, () -> {
            helper.assertBlockPresent(Blocks.WATER, lowerCorner);
            helper.assertBlockPresent(Blocks.WATER, upperCorner);
            helper.assertBlockPresent(Blocks.WATER, directlyAbove);
            helper.assertBlockPresent(Blocks.SNOW_BLOCK, outside);
            helper.assertBlockProperty(campfirePos, CampfireBlock.LIT, true);
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void waterCurioKeepsFractionWithOpenedBottleAcrossSwapAndReload(final GameTestHelper helper) {
        final ServerPlayer player = fakePlayer(helper, "fractional-top-off");
        final var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve()
                .orElseThrow(() -> new IllegalStateException("Mock player is missing the Thirst capability"));
        final var waterSlot = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.getStacksHandler(WaterBottleCurio.SLOT))
                .orElseThrow(() -> new IllegalStateException("Mock player is missing the water Curios slot"));
        waterSlot.getStacks().setStackInSlot(0, purifiedWaterBottles(2));
        helper.assertTrue(waterSlot.getStacks().getStackInSlot(0).getMaxStackSize() == 64,
                "Purified water bottles must support a real stack in the Curios slot");
        thirst.setThirst(19);
        thirst.setQuenched(0);

        tickWaterCurio(player);
        helper.assertTrue(thirst.getThirst() == 20, "One missing thirst point should be restored immediately");
        helper.assertTrue(waterSlot.getStacks().getStackInSlot(0).getCount() == 2,
                "A partial sip must keep the equipped bottle stack together");
        helper.assertTrue(closeTo(WaterBottleCurio.getBottleFraction(waterSlot.getStacks().getStackInSlot(0)), 1.0D / 6.0D),
                "The opened bottle should store one sixth of its contents");

        final ItemStack opened = waterSlot.getStacks().extractItem(0, 2, false);
        thirst.setThirst(19);
        tickWaterCurio(player);
        helper.assertTrue(thirst.getThirst() == 19, "An empty water slot must not provide hydration");
        helper.assertTrue(closeTo(WaterBottleCurio.getBottleFraction(opened), 1.0D / 6.0D),
                "Removing the bottle must carry its fractional progress with it");
        waterSlot.getStacks().setStackInSlot(0, purifiedWaterBottles(1));
        tickWaterCurio(player);
        helper.assertTrue(closeTo(WaterBottleCurio.getBottleFraction(waterSlot.getStacks().getStackInSlot(0)), 1.0D / 6.0D),
                "A fresh bottle must start at zero even after a partial bottle was removed");
        final ItemStack fresh = waterSlot.getStacks().extractItem(0, 1, false);
        helper.assertTrue(closeTo(WaterBottleCurio.getBottleFraction(fresh), 1.0D / 6.0D),
                "The swapped bottle must keep only its own sip");
        waterSlot.getStacks().setStackInSlot(0, ItemStack.of(opened.save(new net.minecraft.nbt.CompoundTag())));

        for (int use = 0; use < 5; use++) {
            thirst.setThirst(19);
            tickWaterCurio(player);
        }
        helper.assertTrue(thirst.getThirst() == 20, "The sixth partial use should still top off thirst");
        helper.assertTrue(thirst.getQuenched() == 9, "One completed bottle plus one sip of the swapped bottle should restore nine quenched points");
        helper.assertTrue(waterSlot.getStacks().getStackInSlot(0).getCount() == 1,
                "Exactly the opened bottle should be consumed while the sealed bottle stays equipped");
        helper.assertTrue(WaterBottleCurio.getBottleFraction(waterSlot.getStacks().getStackInSlot(0)) == 0.0D,
                "The next sealed bottle must start with no consumed fraction");
        final var emptyBottleSlot = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.getStacksHandler(WaterBottleCurio.EMPTY_BOTTLE_SLOT))
                .orElseThrow(() -> new IllegalStateException("Mock player is missing the empty-bottle Curios slot"));
        helper.assertTrue(emptyBottleSlot.getStacks().getStackInSlot(0).is(Items.GLASS_BOTTLE)
                        && emptyBottleSlot.getStacks().getStackInSlot(0).getCount() == 1,
                "Completing a fractional bottle should return one empty bottle to its dedicated slot");
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 0,
                "Returned empty bottles must not spill into normal inventory while the dedicated slot has room");
        helper.assertTrue(WaterBottleCurio.getBottleFraction(fresh) == 1.0D / 6.0D,
                "Completing the original bottle must not change the swapped container");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void waterCurioDoesNotBorrowPastAvailableBottle(final GameTestHelper helper) {
        final ServerPlayer player = fakePlayer(helper, "single-bottle-limit");
        final var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve()
                .orElseThrow(() -> new IllegalStateException("Mock player is missing the Thirst capability"));
        final var waterSlot = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.getStacksHandler(WaterBottleCurio.SLOT))
                .orElseThrow(() -> new IllegalStateException("Mock player is missing the water Curios slot"));
        waterSlot.getStacks().setStackInSlot(0, purifiedWaterBottles(1));
        thirst.setThirst(0);
        thirst.setQuenched(0);

        tickWaterCurio(player);
        helper.assertTrue(thirst.getThirst() == 6, "One equipped bottle should restore only its six thirst points");
        helper.assertTrue(waterSlot.getStacks().getStackInSlot(0).isEmpty(), "The only equipped bottle should be consumed");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void waterCurioConsumesMultipleStackedBottlesAndReturnsBothEmpties(final GameTestHelper helper) {
        final ServerPlayer player = fakePlayer(helper, "stacked-bottles");
        final var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve()
                .orElseThrow(() -> new IllegalStateException("Mock player is missing the Thirst capability"));
        final var curios = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        final var waterSlot = curios.getStacksHandler(WaterBottleCurio.SLOT).orElseThrow();
        final var emptySlot = curios.getStacksHandler(WaterBottleCurio.EMPTY_BOTTLE_SLOT).orElseThrow();
        waterSlot.getStacks().setStackInSlot(0, purifiedWaterBottles(2));
        helper.assertTrue(waterSlot.getStacks().getStackInSlot(0).getMaxStackSize() == 64,
                "Stacked water bottles must retain their stack capacity");
        thirst.setThirst(0);
        thirst.setQuenched(0);

        tickWaterCurio(player);
        helper.assertTrue(thirst.getThirst() == 12, "Two stacked bottles should restore twelve thirst points");
        helper.assertTrue(waterSlot.getStacks().getStackInSlot(0).isEmpty(), "Both used bottles should leave the water slot");
        helper.assertTrue(emptySlot.getStacks().getStackInSlot(0).is(Items.GLASS_BOTTLE)
                        && emptySlot.getStacks().getStackInSlot(0).getCount() == 2,
                "Both empties should stack in the second Curios slot");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void splittingAnOpenedStackKeepsTheSipOnOnlyOneStack(final GameTestHelper helper) {
        final ItemStack original = purifiedWaterBottles(2);
        original.getOrCreateTag().putDouble("BetterContentSippedFraction", 1.0D / 6.0D);

        final ItemStack extracted = original.split(1);
        helper.assertTrue(closeTo(WaterBottleCurio.getBottleFraction(extracted), 1.0D / 6.0D),
                "The extracted stack must retain the opened bottle's sip");
        helper.assertTrue(original.getCount() == 1 && WaterBottleCurio.getBottleFraction(original) == 0.0D,
                "The bottle left behind must be sealed");
        helper.succeed();
    }

    private static ServerPlayer fakePlayer(final GameTestHelper helper, final String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bcf-" + name));
    }

    private static void makeBiomeRainy(final ServerLevel level, final BlockPos pos) {
        final LevelChunk chunk = level.getChunkAt(pos);
        final var plains = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        chunk.getSection(chunk.getSectionIndex(pos.getY())).fillBiomesFromNoise(
                (quartX, quartY, quartZ, sampler) -> plains,
                level.getChunkSource().randomState().sampler(), 0, 0, 0);
        chunk.setUnsaved(true);
    }

    private static ItemStack purifiedWaterBottles(final int count) {
        final ItemStack bottles = PotionUtils.setPotion(new ItemStack(Items.POTION, count), Potions.WATER);
        return WaterPurity.addPurity(bottles, 3);
    }

    private static void tickWaterCurio(final ServerPlayer player) {
        player.tickCount += 10 - player.tickCount % 10;
        WaterBottleCurio.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }

    private static boolean closeTo(final double left, final double right) {
        return Math.abs(left - right) < 1.0E-9D;
    }
}
