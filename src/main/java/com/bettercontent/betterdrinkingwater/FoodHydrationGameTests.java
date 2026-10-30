package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.api.ServingHydration;
import com.mojang.authlib.GameProfile;
import dev.ghen.thirst.content.thirst.PlayerThirst;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.UUID;

/** Nonshipping native contracts. Native runtime execution is a separate qualification gate. */
@PrefixGameTestTemplate(false)
public final class FoodHydrationGameTests {
    private static boolean registered;
    private static synchronized void register() {
        if (registered) return;
        ServingHydration.register(new ResourceLocation(WaterSurvival.MOD_ID,"food_fixture"), (player, stack) ->
                stack.is(Items.PAPER) && stack.hasTag() && stack.getTag().getBoolean("HydrationFixture")
                        ? Optional.of(new ServingHydration.Profile(.5,.625)) : Optional.empty());
        registered = true;
    }
    private static ServerPlayer player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"food_hydration"));
    }
    private static ItemStack serving() {
        var stack = new ItemStack(Items.PAPER); stack.getOrCreateTag().putBoolean("HydrationFixture",true); return stack;
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void managedPreviewPartialCancellationAndNativeCreditNeverAward(GameTestHelper helper) {
        register(); var player = player(helper); var stack = serving();
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        thirst.setThirst(10); thirst.setQuenched(0);
        dev.ghen.thirst.api.ThirstHelper.addFood(Items.PAPER,4,5);
        for (int i=0;i<20;i++) helper.assertTrue(ServingHydration.resolve(player,stack).isPresent(),"Pure preview resolves");
        // This is the shared native credit entry used by edible/potion and Finish paths.
        PlayerThirst.drink(stack,player);
        helper.assertTrue(thirst.getThirst()==10 && thirst.getQuenched()==0
                        && CompletedFoodHydration.read(player).sequence()==0 && PlayerWaterReserve.read(player)==null,
                "Native/config credits, previews and cancelled/partial use cannot create credit or reserve");
        helper.assertTrue(ServingHydration.completeFood(player,1,stack),"One actual completion accepted");
        helper.assertTrue(thirst.getThirst()==10,"First half point remains fractional");
        helper.assertTrue(!ServingHydration.completeFood(player,1,stack),"Same completion ID is idempotent");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void persistedRemainderSurvivesReconnectAndNeverBecomesDrinkReserve(GameTestHelper helper) {
        register(); var original = player(helper); var stack=serving();
        var a=original.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        a.setThirst(0); a.setQuenched(0); ServingHydration.completeFood(original,1,stack);
        // Rehydrate only the actual persisted compound into a distinct player/capability instance.
        var reconnect=player(helper);
        reconnect.getPersistentData().put(Player.PERSISTED_NBT_TAG,
                original.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy());
        var b=reconnect.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        b.setThirst(0); b.setQuenched(0);
        helper.assertTrue(!ServingHydration.completeFood(reconnect,1,stack),"Reconnect retry cannot replay prior completion");
        helper.assertTrue(ServingHydration.completeFood(reconnect,2,stack),"Fresh completion adds to restored fractions");
        helper.assertTrue(b.getThirst()==1 && b.getQuenched()==1 && PlayerWaterReserve.read(reconnect)==null,
                "Two halves credit1/1 with no paid hydration reserve");
        helper.assertTrue(Math.abs(CompletedFoodHydration.read(reconnect).quenchingRemainder()-.25)<1e-9,
                "Native NBT retains exact fractional quenching residue");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void unmanagedFoodKeepsNativeDefaultsAndFullFoodIsLost(GameTestHelper helper) {
        register(); var player=player(helper); var thirst=player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        var melon=new ItemStack(Items.MELON_SLICE);
        helper.assertTrue(ServingHydration.resolve(player,melon).isEmpty(),"No registered provider means no ownership");
        thirst.setThirst(10); thirst.setQuenched(0); PlayerThirst.drink(melon,player);
        helper.assertTrue(thirst.getThirst()>10,"Unmanaged food keeps Thirst native defaults");
        thirst.setThirst(20); thirst.setQuenched(20);
        ServingHydration.completeFood(player,1,new ServingHydration.Profile(4,5));
        helper.assertTrue(thirst.getThirst()==20 && thirst.getQuenched()==20 && PlayerWaterReserve.read(player)==null,
                "Full meters discard food hydration rather than creating drink reserve");
        thirst.setThirst(10); thirst.setQuenched(0);
        ServingHydration.completeFood(player,2,new ServingHydration.Profile(.5,.625));
        helper.assertTrue(thirst.getThirst()==10 && thirst.getQuenched()==0,"No whole credit banked from full meter food");
        helper.succeed();
    }
}
