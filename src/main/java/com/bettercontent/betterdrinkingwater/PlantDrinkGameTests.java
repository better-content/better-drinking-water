package com.bettercontent.betterdrinkingwater;

import com.mojang.authlib.GameProfile;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;

@PrefixGameTestTemplate(false)
public final class PlantDrinkGameTests {
    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void registeredPlantStockResolvesWithNativeRecipes(GameTestHelper helper) {
        PlantDrinks.validateLoaded();
        int tested = 0;
        for (var profile : DrinkCatalogue.entries().values()) {
            if (!PlantDrinks.contains(profile.id()) || !profile.available()) continue;
            tested++;
            var name = profile.id().substring(profile.id().indexOf(':') + 1);
            helper.assertTrue(ForgeRegistries.FLUIDS.containsKey(new ResourceLocation(profile.id())), "Plant fluid registered " + name);
            for (String suffix : new String[]{"_fill", "_empty", "_pour"})
                helper.assertTrue(helper.getLevel().getRecipeManager().byKey(new ResourceLocation(WaterSurvival.MOD_ID,
                        "plant/" + name + suffix)).isPresent(), "Native serializer loaded plant recipe " + name + suffix);
            helper.assertTrue(WaterSources.accepts(item(profile.id())), "Every registered plant item is admitted " + name);
        }
        helper.assertTrue(tested > 30, "Fixture actually loads substantial plant stock, not an empty conditional catalogue");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void nativeFinishConservesVesselsAndPlainJuiceDoesNotHeal(GameTestHelper helper) {
        var player = player(helper);
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        var juice = item("better_drinking_water:melon_juice");
        player.setHealth(8); player.getFoodData().setFoodLevel(10); player.getFoodData().setSaturation(0);
        thirst.setThirst(20);
        var denied = juice.getItem().finishUsingItem(juice, player.level(), player);
        helper.assertTrue(denied == juice && juice.getCount() == 1 && player.getHealth() == 8,
                "Full thirst cannot purchase a manual serving or healing");
        thirst.setThirst(10);
        var result = juice.getItem().finishUsingItem(juice, player.level(), player);
        helper.assertTrue(result.is(Items.GLASS_BOTTLE) && juice.isEmpty() && thirst.getThirst() == 16,
                "Native finish consumes exactly one 250mB bottle and restores six thirst");
        helper.assertTrue(player.getHealth() == 8 && player.getActiveEffects().isEmpty(), "Plain melon carries no healing or potion effect");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 11, "Manual explicit nutrition releases once, not via super plus entitlement");
        thirst.setThirst(10);
        var brew = item("better_drinking_water:summerheart_cordial");
        brew.getItem().finishUsingItem(brew, player.level(), player);
        helper.assertTrue(player.getHealth() == 10, "Named specialty preserves two HP full-serving heal");
        thirst.setThirst(10);
        var reserve = item("better_drinking_water:summerheart_reserve");
        reserve.getItem().finishUsingItem(reserve, player.level(), player);
        helper.assertTrue(player.getHealth() == 14, "Secondary preparation preserves four HP full-serving heal");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void cleansingAndVesselsAreCompletionSafeUnderNativeReserve(GameTestHelper helper) {
        var player = player(helper);
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        var source = item("better_drinking_water:violetguard_tea");
        var debit = WaterSources.debit(player, source);
        helper.assertTrue(debit != null && debit.remaining().isEmpty() && debit.remainder().is(Items.GLASS_BOTTLE),
                "One upfront item debit produces exactly its glass vessel");
        PlayerWaterReserve.write(player, debit.portion());
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 1200));
        for (int i = 0; i < 6; i++) {
            thirst.setThirst(19);
            WaterDrinking.sip(player, () -> { throw new AssertionError("Paid serving must not repurchase"); }, 0);
            helper.assertTrue(player.hasEffect(MobEffects.POISON) == (i < 5), "Poison cleansing happens only at paid serving completion");
        }
        helper.assertTrue(PlayerWaterReserve.read(player) == null, "Completed serving cannot replay cleansing");
        helper.succeed();
    }
    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void voidstepNeverBlinksBeforeCompletionAndHonoursNativeVeto(GameTestHelper helper) {
        var player = player(helper);
        var stack = item("better_drinking_water:voidstep_elixir");
        var paid = WaterSources.debit(player, stack).portion().beverage();
        var before = player.position();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<net.minecraftforge.event.entity.EntityTeleportEvent.ChorusFruit> veto = event -> {
            if (event.getEntity() == player) { calls.incrementAndGet(); event.setCanceled(true); }
        };
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.NORMAL,
                false, net.minecraftforge.event.entity.EntityTeleportEvent.ChorusFruit.class, veto);
        try {
            paid.release(player, 0, .5);
            helper.assertTrue(calls.get() == 0 && player.position().equals(before), "No native blink callback on a partial serving");
            paid.release(player, .5, 1);
            helper.assertTrue(calls.get() == 1 && player.position().equals(before), "Completion invokes native event once; veto stops all destination attempts");
            helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "Timed protection remains a distinct proportional entitlement");
        } finally { net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(veto); }
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void everyActivePlantManualFinishUsesProfileThirstExactlyOnce(GameTestHelper helper) {
        int tested = 0;
        for (var profile : DrinkCatalogue.entries().values()) {
            if (!profile.id().startsWith(WaterSurvival.MOD_ID + ":") || !profile.available()) continue;
            var stack = item(profile.id());
            if (!(stack.getItem() instanceof PlantDrinkItem)) continue;
            var player = player(helper);
            var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
            thirst.setThirst(0); thirst.setQuenched(0);
            thirst.drink(player, profile.hydration(), profile.quenching());
            int expectedThirst = thirst.getThirst(), expectedQuenched = thirst.getQuenched();
            thirst.setThirst(0); thirst.setQuenched(0);
            var original = stack.copy();
            var result = stack.getItem().finishUsingItem(stack, player.level(), player);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish(player, original, 32, result));
            helper.assertTrue(thirst.getThirst() == expectedThirst && thirst.getQuenched() == expectedQuenched,
                    "Manual plus real native Finish event uses catalogue hydration/quenching once: " + profile.id());
            helper.assertTrue(stack.isEmpty() && result.is(Items.GLASS_BOTTLE), "One payment and vessel: " + profile.id());
            tested++;
        }
        helper.assertTrue(tested > 30, "Native fixture must exercise substantial active custom drink stock");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "plant-drink-test"));
    }
    private static ItemStack item(String id) {
        var key = new ResourceLocation(id);
        if (!ForgeRegistries.ITEMS.containsKey(key)) throw new AssertionError("Missing native item " + id);
        return new ItemStack(ForgeRegistries.ITEMS.getValue(key));
    }
}
