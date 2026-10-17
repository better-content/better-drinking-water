package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.WaterSurvival;
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
        // Use the top of the buildable world so unrelated terrain and structures cannot
        // cover the collector in the generated GameTest world.
        final int relativeY = level.getMaxBuildHeight() - 3 - helper.absolutePos(BlockPos.ZERO).getY();
        final BlockPos relativePos = new BlockPos(2, relativeY, 2);
        final BlockPos worldPos = helper.absolutePos(relativePos);
        makeBiomeRainy(level, worldPos);
        level.setWeatherParameters(0, 1200, true, false);
        level.setRainLevel(1.0F);
        final BlockState state = RainCollectorRegistry.RAIN_COLLECTOR.get().defaultBlockState();
        helper.setBlock(relativePos, state);
        helper.runAfterDelay(2, () -> {
            if (!level.canSeeSky(worldPos.above()) || !level.isRainingAt(worldPos.above())) {
                helper.fail("Exposed collector setup did not have rain and sky access");
                return;
            }
            RainCollectorRegistry.RAIN_COLLECTOR.get().tick(state, level, worldPos, RandomSource.create(1L));
            helper.assertBlockProperty(relativePos, RainCollectorBlock.LEVEL, 1);
            helper.succeed();
        });
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
        // The fixture is embedded underground. Cap melt targets so random overhead
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
    public static void waterCurioKeepsPrepaidReserveAcrossSwapAndReload(final GameTestHelper helper) {
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
        helper.assertTrue(waterSlot.getStacks().getStackInSlot(0).getCount() == 1,
                "The first sip must debit one bottle immediately");
        helper.assertTrue(closeTo(PlayerWaterReserve.read(player).consumed(), 1.0D / 6.0D),
                "The player must own the remaining water");
        helper.assertTrue(!waterSlot.getStacks().getStackInSlot(0).getTag().contains("BetterContentSippedFraction"),
                "Remaining bottles must not carry sip progress");
        helper.assertTrue(CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
                .getStacksHandler(WaterBottleCurio.EMPTY_BOTTLE_SLOT).orElseThrow()
                .getStacks().getStackInSlot(0).getCount() == 1,
                "The empty must be returned on the first sip, not reserve exhaustion");

        final ItemStack unopened = waterSlot.getStacks().extractItem(0, 1, false);
        final ServerPlayer other = fakePlayer(helper, "reserve-recipient");
        helper.assertTrue(PlayerWaterReserve.read(other) == null, "Another player must not inherit credit");
        // Exercise native entity save/load and ServerPlayer's respawn/Forge clone path.
        var beforeReload = PlayerWaterReserve.read(player);
        player.load(player.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
        helper.assertTrue(beforeReload.equals(PlayerWaterReserve.read(player)),
                "Native entity save/load must preserve exact credit");
        final ServerPlayer replacement = fakePlayer(helper, "reserve-clone");
        replacement.restoreFrom(player, false);
        helper.assertTrue(PlayerWaterReserve.read(player).equals(PlayerWaterReserve.read(replacement)),
                "Respawn must preserve exact credit without replaying refunds");
        for (int use = 0; use < 5; use++) {
            thirst.setThirst(19);
            tickWaterCurio(player);
        }
        helper.assertTrue(thirst.getThirst() == 20, "The sixth partial use should still top off thirst");
        helper.assertTrue(thirst.getQuenched() == 8, "Six small sips must award exactly one bottle's quenched total");
        helper.assertTrue(waterSlot.getStacks().getStackInSlot(0).isEmpty(),
                "Paid water must work with an empty slot");
        helper.assertTrue(PlayerWaterReserve.read(player) == null, "Completed credit must be cleared");
        helper.assertTrue(unopened.getCount() == 1, "Removing unused bottles must not consume them");
        final var emptySlot = CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
                .getStacksHandler(WaterBottleCurio.EMPTY_BOTTLE_SLOT).orElseThrow();
        helper.assertTrue(emptySlot.getStacks().getStackInSlot(0).is(Items.GLASS_BOTTLE)
                        && emptySlot.getStacks().getStackInSlot(0).getCount() == 1,
                "Completing a fractional bottle should return one empty bottle to its dedicated slot");
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 0,
                "Empties must prefer their dedicated slot over the hotbar");
        thirst.setThirst(19);
        tickWaterCurio(player);
        helper.assertTrue(thirst.getThirst() == 19, "No reserve and no bottle must grant nothing");
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
        waterSlot.getStacks().setStackInSlot(0, purifiedWaterBottles(2));
        helper.assertTrue(waterSlot.getStacks().getStackInSlot(0).getMaxStackSize() == 64,
                "Stacked water bottles must retain their stack capacity");
        thirst.setThirst(0);
        thirst.setQuenched(0);

        tickWaterCurio(player);
        helper.assertTrue(thirst.getThirst() == 12, "Two stacked bottles should restore twelve thirst points");
        helper.assertTrue(waterSlot.getStacks().getStackInSlot(0).isEmpty(), "Both used bottles should leave the water slot");
        final var emptySlot = curios.getStacksHandler(WaterBottleCurio.EMPTY_BOTTLE_SLOT).orElseThrow();
        helper.assertTrue(emptySlot.getStacks().getStackInSlot(0).is(Items.GLASS_BOTTLE)
                        && emptySlot.getStacks().getStackInSlot(0).getCount() == 2,
                "Both empties should stack in the dedicated Curios slot");
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 0,
                "Stacked empties must not spill into the hotbar while their slot has room");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void legacyFractionRequiresDebitAndAppliesOnlyOnce(final GameTestHelper helper) {
        final ServerPlayer player = fakePlayer(helper, "legacy-reserve");
        final var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        final var slot = CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
                .getStacksHandler(WaterBottleCurio.SLOT).orElseThrow();
        final ItemStack original = purifiedWaterBottles(2);
        original.getOrCreateTag().putDouble("BetterContentSippedFraction", 0.5D);
        slot.getStacks().setStackInSlot(0, original);
        thirst.setThirst(20);
        tickWaterCurio(player);
        helper.assertTrue(slot.getStacks().getStackInSlot(0).getCount() == 2 && PlayerWaterReserve.read(player) == null,
                "Full thirst must not charge or migrate a bottle");
        thirst.setThirst(19);
        thirst.setQuenched(0);
        tickWaterCurio(player);
        helper.assertTrue(slot.getStacks().getStackInSlot(0).getCount() == 1,
                "Migration must debit one bottle");
        helper.assertTrue(closeTo(PlayerWaterReserve.read(player).consumed(), 2.0D / 3.0D),
                "Migration must honor the already consumed half");
        helper.assertTrue(!slot.getStacks().getStackInSlot(0).getTag().contains("BetterContentSippedFraction"),
                "Legacy progress must not leak onto the next bottle");
        thirst.setThirst(18);
        tickWaterCurio(player);
        helper.assertTrue(PlayerWaterReserve.read(player) == null && thirst.getQuenched() == 4,
                "Legacy remaining half must grant only its remaining quenched credit");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void emptyBottleSlotStacksAndOverflowFallsBackToHotbar(final GameTestHelper helper) {
        final ServerPlayer player = fakePlayer(helper, "empty-overflow");
        final var emptySlot = CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
                .getStacksHandler(WaterBottleCurio.EMPTY_BOTTLE_SLOT).orElseThrow();
        helper.assertTrue(emptySlot.getStacks().isItemValid(0, new ItemStack(Items.GLASS_BOTTLE)),
                "The restored slot must accept glass bottles");
        helper.assertTrue(!emptySlot.getStacks().isItemValid(0, new ItemStack(Items.STONE)),
                "The empty-bottle slot must reject unrelated items");
        emptySlot.getStacks().setStackInSlot(0, new ItemStack(Items.GLASS_BOTTLE, 63));
        WaterBottleCurio.returnEmptyBottles(player, 3);
        helper.assertTrue(emptySlot.getStacks().getStackInSlot(0).getCount() == 64,
                "Returned empties should fill the dedicated stack without exceeding 64");
        helper.assertTrue(player.getInventory().getItem(0).is(Items.GLASS_BOTTLE)
                        && player.getInventory().getItem(0).getCount() == 2,
                "Only the overflow should fall back to the hotbar");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void transferringSplitBottlesCannotTransferCredit(final GameTestHelper helper) {
        final ServerPlayer owner = fakePlayer(helper, "reserve-owner");
        final ServerPlayer recipient = fakePlayer(helper, "reserve-transfer");
        final var ownerThirst = owner.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        final var recipientThirst = recipient.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        final var ownerSlot = CuriosApi.getCuriosInventory(owner).resolve().orElseThrow()
                .getStacksHandler(WaterBottleCurio.SLOT).orElseThrow();
        final var recipientSlot = CuriosApi.getCuriosInventory(recipient).resolve().orElseThrow()
                .getStacksHandler(WaterBottleCurio.SLOT).orElseThrow();
        ownerSlot.getStacks().setStackInSlot(0, purifiedWaterBottles(3));
        ownerThirst.setThirst(19);
        tickWaterCurio(owner);
        var credit = PlayerWaterReserve.read(owner);
        ItemStack extracted = ownerSlot.getStacks().extractItem(0, 2, false);
        ItemStack transferred = extracted.split(1);
        helper.assertTrue(ItemStack.isSameItemSameTags(transferred, purifiedWaterBottles(1)),
                "Splitting must leave ordinary untouched water bottles");
        recipientSlot.getStacks().setStackInSlot(0, transferred);
        recipientThirst.setThirst(19);
        tickWaterCurio(recipient);
        helper.assertTrue(recipientSlot.getStacks().getStackInSlot(0).isEmpty()
                        && PlayerWaterReserve.read(recipient).consumed() == credit.consumed(),
                "Recipient must pay a separate whole bottle for their own reserve");
        helper.assertTrue(credit.equals(PlayerWaterReserve.read(owner)), "Transfers cannot change the owner's credit");
        ownerSlot.getStacks().insertItem(0, extracted, false);
        ownerSlot.getStacks().insertItem(0, purifiedWaterBottles(1), false);
        ownerThirst.setThirst(19);
        tickWaterCurio(owner);
        helper.assertTrue(ownerSlot.getStacks().getStackInSlot(0).getCount() == 2
                        && closeTo(PlayerWaterReserve.read(owner).consumed(), 2.0D / 6.0D),
                "Re-equipping and merging must spend old credit, not debit or reset it");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void emptyOverflowDropsWhenHotbarIsFull(final GameTestHelper helper) {
        final ServerPlayer player = fakePlayer(helper, "drop-overflow");
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        final var empty = CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
                .getStacksHandler(WaterBottleCurio.EMPTY_BOTTLE_SLOT).orElseThrow();
        empty.getStacks().setStackInSlot(0, new ItemStack(Items.GLASS_BOTTLE, 64));
        for (int index = 0; index < 9; index++) player.getInventory().setItem(index, new ItemStack(Items.STONE, 64));
        var bounds = new net.minecraft.world.phys.AABB(pos).inflate(3);
        int before = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds,
                entity -> entity.getItem().is(Items.GLASS_BOTTLE)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        WaterBottleCurio.returnEmptyBottles(player, 2);
        int after = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds,
                entity -> entity.getItem().is(Items.GLASS_BOTTLE)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(after - before == 2 && empty.getStacks().getStackInSlot(0).getCount() == 64,
                "Full storage must drop exactly the overflow without losing or duplicating empties");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void malformedReservesCannotGrantWater(final GameTestHelper helper) {
        final ServerPlayer player = fakePlayer(helper, "malformed-reserve");
        final var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        for (String field : new String[]{"version", "thirst", "quenched", "consumed", "purity", "missing"}) {
            PlayerWaterReserve.write(player, new PlayerWaterReserve.Reserve(6, 8, 0.5D, 3));
            var data = player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG)
                    .getCompound(PlayerWaterReserve.KEY);
            switch (field) {
                case "version" -> data.putInt(field, 2);
                case "thirst" -> data.putInt(field, 0);
                case "quenched" -> data.putInt(field, -1);
                case "consumed" -> data.putDouble(field, Double.NaN);
                case "purity" -> data.putInt(field, 4);
                default -> data.remove("thirst");
            }
            thirst.setThirst(19);
            tickWaterCurio(player);
            helper.assertTrue(thirst.getThirst() == 19 && PlayerWaterReserve.read(player) == null,
                    "Malformed " + field + " must grant no water");
        }
        for (double invalid : new double[]{-0.1D, 1.0D, Double.POSITIVE_INFINITY}) {
            PlayerWaterReserve.write(player, new PlayerWaterReserve.Reserve(6, 8, invalid, 3));
            tickWaterCurio(player);
            helper.assertTrue(thirst.getThirst() == 19 && PlayerWaterReserve.read(player) == null,
                    "Invalid progress must grant no water");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty", timeoutTicks = 200)
    public static void purityIsChargedOnceAndCannotBeUpgradedBySwap(final GameTestHelper helper) {
        final ServerPlayer player = fakePlayer(helper, "purity-reserve");
        final var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        final var curios = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        final var slot = curios.getStacksHandler(WaterBottleCurio.SLOT).orElseThrow();
        final var empty = curios.getStacksHandler(WaterBottleCurio.EMPTY_BOTTLE_SLOT).orElseThrow();
        var poison = dev.ghen.thirst.foundation.config.CommonConfig.DIRTY_POISON_PERCENTAGE;
        var nausea = dev.ghen.thirst.foundation.config.CommonConfig.DIRTY_NAUSEA_PERCENTAGE;
        var allow = dev.ghen.thirst.foundation.config.CommonConfig.QUENCH_THIRST_WHEN_DEBUFFED;
        // ConfigValue.set writes Forge's autosaving file; its reload watcher can
        // clear cached overrides between this test's writes and the native effect roll.
        // Scope the synchronous fixture to real ConfigValues backed by an in-memory
        // copy, so temporary percentages never reach the watched common.toml file.
        final var config = net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets()
                .get(net.minecraftforge.fml.config.ModConfig.Type.COMMON).stream()
                .filter(candidate -> candidate.getModId().equals("thirst")
                        && candidate.getSpec() instanceof net.minecraftforge.common.ForgeConfigSpec candidateSpec
                        && candidateSpec.getValues().get(poison.getPath()) == poison)
                .findFirst().orElseThrow();
        final var spec = (net.minecraftforge.common.ForgeConfigSpec) config.getSpec();
        final var originalConfig = config.getConfigData();
        final var oldPoison = poison.get();
        final var oldNausea = nausea.get();
        final var oldAllow = allow.get();
        try {
            spec.setConfig(copyConfig(originalConfig));
            poison.set(100);
            nausea.set(100);
            allow.set(false);
            spec.afterReload(); // Exercise the cache reset used by the native file watcher.
            helper.assertTrue(poison.get().intValue() == 100 && nausea.get().intValue() == 100 && !allow.get(),
                    "Native config overrides must survive cache invalidation");
            helper.assertTrue(originalConfig.get(poison.getPath()).equals(oldPoison)
                            && originalConfig.get(nausea.getPath()).equals(oldNausea)
                            && originalConfig.get(allow.getPath()).equals(oldAllow),
                    "Purity fixture must not mutate the watched native config");
            slot.getStacks().setStackInSlot(0, WaterPurity.addPurity(purifiedWaterBottles(1), 0));
            thirst.setThirst(19);
            tickWaterCurio(player);
            helper.assertTrue(thirst.getThirst() == 19 && PlayerWaterReserve.read(player) == null
                            && slot.getStacks().getStackInSlot(0).isEmpty() && empty.getStacks().getStackInSlot(0).getCount() == 1,
                    "Rejected unsafe water must be paid and refunded without creating credit");
            player.removeAllEffects();
            allow.set(true);
            spec.afterReload();
            slot.getStacks().setStackInSlot(0, WaterPurity.addPurity(purifiedWaterBottles(1), 0));
            tickWaterCurio(player);
            helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                    "Charging unsafe water must evaluate its effects");
            player.removeAllEffects();
            slot.getStacks().setStackInSlot(0, purifiedWaterBottles(1));
            WaterSafetyEpisodes.thirstLost(player);
            thirst.setThirst(19);
            tickWaterCurio(player);
            helper.assertTrue(PlayerWaterReserve.read(player).purity() == 0 && !player.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                    "Later sips must retain purity without rerolling effects");
            helper.assertTrue(!WaterSafetyEpisodes.remembered(player).isEmpty(),
                    "Equipping pure water must not complete safety while drinking a dirty reserve");
            thirst.setThirst(16);
            tickWaterCurio(player);
            helper.assertTrue(PlayerWaterReserve.read(player) == null && slot.getStacks().getStackInSlot(0).getCount() == 1,
                    "Old reserve must drain before charging the replacement");
            thirst.setThirst(19);
            tickWaterCurio(player);
            helper.assertTrue(WaterSafetyEpisodes.remembered(player).isEmpty()
                            && empty.getStacks().getStackInSlot(0).getCount() == 3,
                    "Actual purified hydration must complete safety, with one empty per debit");
        } finally {
            spec.setConfig(originalConfig);
        }
        helper.succeed();
    }

    private static com.electronwill.nightconfig.core.CommentedConfig copyConfig(
            final com.electronwill.nightconfig.core.UnmodifiableConfig source) {
        final var copy = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        for (var entry : source.entrySet()) {
            final Object value = entry.getValue();
            copy.set(java.util.List.of(entry.getKey()),
                    value instanceof com.electronwill.nightconfig.core.UnmodifiableConfig section
                            ? copyConfig(section) : value);
        }
        return copy;
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
