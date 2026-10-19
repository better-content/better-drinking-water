package com.bettercontent.betterdrinkingwater;

import com.mojang.authlib.GameProfile;
import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

@PrefixGameTestTemplate(false)
public final class WaterSourceGameTests {
    private WaterSourceGameTests() {}

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void bowlAndBucketDebitOnceAndReturnTheirOwnVessel(GameTestHelper helper) {
        for (boolean bucket : new boolean[]{false, true}) {
            var player = player(helper);
            var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
            var curios = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            var slot = curios.getStacksHandler("water").orElseThrow().getStacks();
            var empty = curios.getStacksHandler("empty_bottle").orElseThrow().getStacks();
            ItemStack water = bucket ? new ItemStack(Items.WATER_BUCKET) : item("thirst:terracotta_water_bowl");
            WaterPurity.addPurity(water, 3);
            helper.assertTrue(slot.isItemValid(0, water), "Native slot must admit the filled vessel");
            slot.setStackInSlot(0, water);
            thirst.setThirst(20);
            tick(player);
            helper.assertTrue(!slot.getStackInSlot(0).isEmpty() && PlayerWaterReserve.read(player) == null,
                    "Full thirst must not debit any vessel");
            thirst.setThirst(19);
            thirst.setQuenched(0);
            tick(player);
            var reserve = PlayerWaterReserve.read(player);
            helper.assertTrue(slot.getStackInSlot(0).isEmpty() && reserve != null && reserve.purity() == 3,
                    "First sip pays once and retains source quality");
            helper.assertTrue(empty.getStackInSlot(0).is(bucket ? Items.BUCKET : item("thirst:terracotta_bowl").getItem()),
                    "Return the actual empty, never a glass bottle substitute");
            int total = bucket ? 16 : 4;
            helper.assertTrue(reserve.thirst() == total, "Bucket and bowl have bounded hydration portions");
            for (int sip = 1; sip < total; sip++) {
                thirst.setThirst(19);
                tick(player);
            }
            helper.assertTrue(PlayerWaterReserve.read(player) == null && empty.getStackInSlot(0).getCount() == 1,
                    "Fractional sipping cannot duplicate the empty vessel");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void nativeCopperCanStackIsDebitedWithoutChangingUnopenedCans(GameTestHelper helper) {
        var cans = item("tconstruct:copper_can");
        cans.setCount(3);
        var handler = cans.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElseThrow();
        helper.assertTrue(handler.fill(water(270, 3), IFluidHandler.FluidAction.EXECUTE) == 270,
                "Pinned native cans must hold 90 mB each");
        var debit = WaterSources.debit(cans);
        helper.assertTrue(debit != null && debit.remaining().getCount() == 2 && debit.portion().thirst() == 1
                        && debit.portion().purity() == 3 && WaterSources.isEmptyVessel(debit.remainder()),
                "Exactly one native can is paid; its empty is returned and quality retained");
        helper.assertTrue(handler.getFluidInTank(0).getAmount() == 270,
                "Planning a copied debit cannot mutate the equipped source");
        var left = debit.remaining().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElseThrow();
        helper.assertTrue(left.getFluidInTank(0).getAmount() == 180 && WaterPurity.getPurity(left.getFluidInTank(0)) == 3,
                "Unopened cans retain their exact water and purity");
        helper.assertTrue(!WaterSources.accepts(new ItemStack(Items.COD_BUCKET))
                        && !WaterSources.accepts(new ItemStack(Items.LAVA_BUCKET)),
                "Creature and non-water buckets must not be consumed by the generic adapter");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void nativeTinkerTankUsesItsFluidNotItsOuterItemTag(GameTestHelper helper) {
        var tank = item("tconstruct:seared_fuel_tank");
        // Native TankItem's setter preserves full FluidStack NBT; no shadow item-owned quality.
        slimeknights.tconstruct.smeltery.item.TankItem.setTank(tank, water(1000, 3));
        tank.getOrCreateTag().putInt("Purity", 0);
        var debit = WaterSources.debit(tank);
        helper.assertTrue(debit != null && debit.portion().purity() == 3 && debit.portion().thirst() == 4,
                "Fluid quality wins over a stale container tag");
        var remaining = debit.remaining().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElseThrow();
        helper.assertTrue(remaining.getFluidInTank(0).getAmount() == 750,
                "Tank purchase removes only a bounded 250 mB portion");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void bucketTankTransfersPreserveQualityWithoutRefillInheritance(GameTestHelper helper) {
        for (int purity : new int[]{0, 3}) {
            var tank = new FluidTank(2000);
            var bucket = WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), purity);
            var bucketHandler = bucket.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElseThrow();
            FluidStack extracted = bucketHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(extracted.getAmount() == 1000 && WaterPurity.getPurity(extracted) == purity,
                    "Forge bucket extraction must not discard purity");
            tank.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
            var filled = FluidUtil.getFilledBucket(tank.drain(1000, IFluidHandler.FluidAction.EXECUTE));
            helper.assertTrue(filled.is(Items.WATER_BUCKET) && WaterPurity.getPurity(filled) == purity,
                    "Tank-to-bucket conversion must retain quality");
            bucketHandler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(WaterPurity.getPurity(bucketHandler.getContainer()) == 0,
                    "Refilling with untagged water cannot inherit purified status");
        }
        var untagged = new FluidStack(Fluids.WATER, 1000);
        FluidUtil.getFilledBucket(untagged);
        helper.assertTrue(!untagged.hasTag(), "Bucket conversion must not mutate the supplied fluid's metadata");
        var plainBucket = new ItemStack(Items.WATER_BUCKET);
        plainBucket.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElseThrow().getFluidInTank(0);
        helper.assertTrue(!plainBucket.hasTag(), "Capability inspection must not mutate an untagged bucket");
        var tank = new FluidTank(2000);
        tank.fill(water(1000, 3), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(tank.fill(water(1000, 0), IFluidHandler.FluidAction.EXECUTE) == 0,
                "Native NBT-sensitive tanks must retain their quality separation");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void directCauldronDrinkingIsPaidAndFractional(GameTestHelper helper) {
        var player = player(helper);
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        BlockPos relative = new BlockPos(2, 2, 2);
        BlockPos pos = helper.absolutePos(relative);
        var state = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2)
                .setValue(WaterPurity.BLOCK_PURITY, 4);
        helper.setBlock(relative, state);
        thirst.setThirst(20);
        helper.assertTrue(!BlockWaterDrinking.drink(player, pos, Direction.UP)
                        && helper.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 2,
                "Full thirst must not consume cauldron water");
        player.getPersistentData().remove("better_drinking_water:last_block_drink");
        thirst.setThirst(19);
        helper.assertTrue(BlockWaterDrinking.drink(player, pos, Direction.UP), "Cauldron should hydrate");
        helper.assertTrue(thirst.getThirst() == 20 && PlayerWaterReserve.read(player).purity() == 3
                        && helper.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 1,
                "Pay one level and retain its unused clean portion");
        helper.assertTrue(!BlockWaterDrinking.drink(player, pos, Direction.UP), "Rapid repeat must be throttled");
        thirst.setThirst(19);
        tick(player);
        helper.assertTrue(thirst.getThirst() == 20 && helper.getLevel().getBlockState(pos)
                        .getValue(LayeredCauldronBlock.LEVEL) == 1,
                "Paid cauldron reserve works without a vessel and without a second debit");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void directCreateTankDrinksStoredQualityAndDebitsNativeTank(GameTestHelper helper) {
        var player = player(helper);
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        var relative = new BlockPos(2, 2, 2);
        var pos = helper.absolutePos(relative);
        helper.setBlock(relative, com.simibubi.create.AllBlocks.FLUID_TANK.getDefaultState());
        var handler = helper.getLevel().getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                .resolve().orElseThrow();
        handler.fill(water(1000, 3), IFluidHandler.FluidAction.EXECUTE);
        thirst.setThirst(19);
        helper.assertTrue(BlockWaterDrinking.drink(player, pos, Direction.UP), "Native tank should hydrate");
        helper.assertTrue(handler.getFluidInTank(0).getAmount() == 750 && PlayerWaterReserve.read(player).purity() == 3,
                "Direct drinking must use stored fluid quality, not surrounding biome quality");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void fluidDebitNeverCreatesCreditForMissingOrDifferentWater(GameTestHelper helper) {
        for (FluidStack fluid : new FluidStack[]{FluidStack.EMPTY, new FluidStack(Fluids.LAVA, 1000), water(50, 3)}) {
            var tank = new FluidTank(1000);
            tank.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
            int before = tank.getFluidAmount();
            helper.assertTrue(BlockWaterDrinking.drain(tank) == null && tank.getFluidAmount() == before,
                    "Empty, non-water and sub-minimum volumes must not be consumed or credited");
        }
        var tank = new FluidTank(1000);
        tank.fill(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(BlockWaterDrinking.drain(tank).purity() == 0 && tank.isEmpty(),
                "Unknown water must stay dirty and still pay its real volume");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void bowlsAndBucketsRemainPurifiableWithoutVesselGenerators(GameTestHelper helper) {
        for (var input : new ItemStack[]{new ItemStack(Items.WATER_BUCKET), item("thirst:terracotta_water_bowl")}) {
            for (boolean tagged : new boolean[]{false, true}) {
                var dirty = input.copy();
                if (tagged) WaterPurity.addPurity(dirty, 0);
                var inventory = new net.minecraft.world.SimpleContainer(dirty);
                for (var type : java.util.List.of(net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING,
                        net.minecraft.world.item.crafting.RecipeType.SMELTING)) {
                    var recipe = helper.getLevel().getRecipeManager().getRecipeFor(type, inventory, helper.getLevel());
                    helper.assertTrue(recipe.isPresent(), "Dirty and untagged vessels need a boiling route");
                    helper.assertTrue(tagged || !dirty.hasTag(), "Recipe matching must not mutate the input vessel");
                    var result = recipe.orElseThrow().getResultItem(helper.getLevel().registryAccess());
                    helper.assertTrue(result.is(input.getItem()) && WaterPurity.getPurity(result) == 3,
                            "One boiling step must purify only the recipe's filled vessel");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void directGestureRespectsDenialAndBothHands(GameTestHelper helper) {
        var player = player(helper);
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        var relative = new BlockPos(2, 2, 2);
        var pos = helper.absolutePos(relative);
        helper.setBlock(relative, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        thirst.setThirst(19);
        player.setShiftKeyDown(true);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                Direction.UP, pos, false);
        var denied = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                player, net.minecraft.world.InteractionHand.MAIN_HAND, pos, hit);
        denied.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
        BlockWaterDrinking.onInteract(denied);
        helper.assertTrue(!denied.isCanceled() && PlayerWaterReserve.read(player) == null,
                "A prior protection denial must not be overridden");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.STONE));
        var occupied = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                player, net.minecraft.world.InteractionHand.MAIN_HAND, pos, hit);
        BlockWaterDrinking.onInteract(occupied);
        helper.assertTrue(!occupied.isCanceled() && thirst.getThirst() == 19,
                "An occupied offhand must retain ordinary interaction semantics");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
        var accepted = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                player, net.minecraft.world.InteractionHand.MAIN_HAND, pos, hit);
        BlockWaterDrinking.onInteract(accepted);
        helper.assertTrue(accepted.isCanceled() && thirst.getThirst() == 20
                        && helper.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 2,
                "Only the intended gesture may consume one water level");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "water-source"));
    }

    private static ItemStack item(String id) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
    }

    private static FluidStack water(int amount, int purity) {
        return WaterPurity.addPurity(new FluidStack(Fluids.WATER, amount), purity);
    }

    private static void tick(ServerPlayer player) {
        player.tickCount += 10 - player.tickCount % 10;
        WaterBottleCurio.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }
}
