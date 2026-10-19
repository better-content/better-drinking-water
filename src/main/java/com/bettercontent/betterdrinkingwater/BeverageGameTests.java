package com.bettercontent.betterdrinkingwater;

import com.mojang.authlib.GameProfile;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.UUID;

/** Native registry, capability, Curios, food and potion contracts, not mock beverage implementations. */
@PrefixGameTestTemplate(false)
public final class BeverageGameTests {
    private BeverageGameTests() {}

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void everyLoadedCatalogueProfileResolvesNativeItemsAndEffects(GameTestHelper helper) {
        DrinkCatalogue.validateLoaded();
        for (String namespace : List.of("farmersdelight", "farmersrespite", "brewinandchewin")) {
            var profiles = DrinkCatalogue.entries().values().stream()
                    .filter(d -> d.id().startsWith(namespace + ":")).toList();
            helper.assertTrue(!profiles.isEmpty(), "Fixture must exercise authored " + namespace + " profiles");
            for (var profile : profiles) {
                var stack = item(profile.id());
                helper.assertTrue(profile.available() && DrinkCatalogue.find(stack) != null && WaterSources.accepts(stack),
                        "Real upstream item and all its effects must load: " + profile.id());
                var debit = WaterSources.debit(player(helper), stack);
                helper.assertTrue(debit != null && debit.portion().beverage() != null,
                        "Real upstream item must buy a validated beverage: " + profile.id());
                helper.assertTrue(stack.getCount() == 1 && debit.remaining().isEmpty(),
                        "Debit planning must copy rather than mutate native input: " + profile.id());
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void tooltipAndNativeCurioSlotShareTheBeverageAdmissionPredicate(GameTestHelper helper) {
        var player = player(helper);
        var slot = CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
                .getStacksHandler("water").orElseThrow().getStacks();
        var beverages = new java.util.ArrayList<ItemStack>();
        for (var profile : DrinkCatalogue.entries().values()) if (profile.available()) beverages.add(item(profile.id()));
        beverages.add(new ItemStack(Items.WATER_BUCKET));
        beverages.add(net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION),
                net.minecraft.world.item.alchemy.Potions.WATER));
        beverages.add(item("thirst:terracotta_water_bowl"));
        beverages.add(new ItemStack(Items.APPLE));
        beverages.add(new ItemStack(Items.LAVA_BUCKET));
        for (var stack : beverages) {
            boolean accepted = WaterSources.accepts(stack);
            helper.assertTrue(slot.isItemValid(0, stack) == accepted,
                    "Curios must use the same admission predicate for " + WaterSources.id(stack));
            var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
            WaterContainerTooltip.onTooltip(new net.minecraftforge.event.entity.player.ItemTooltipEvent(
                    stack, player, lines, net.minecraft.world.item.TooltipFlag.Default.NORMAL));
            long compatibilityLines = lines.stream().filter(line -> line.getContents()
                    instanceof net.minecraft.network.chat.contents.TranslatableContents translated
                    && translated.getKey().equals("tooltip.better_drinking_water.slot_compatible")).count();
            helper.assertTrue(compatibilityLines == (accepted ? 1 : 0),
                    "Compatibility hint must be concise: exactly one line per accepted source");
            boolean advertised = lines.stream().anyMatch(line -> line.getContents()
                    instanceof net.minecraft.network.chat.contents.TranslatableContents translated
                    && translated.getKey().equals("tooltip.better_drinking_water.slot_compatible"));
            helper.assertTrue(advertised == accepted,
                    "Native tooltip must neither hide accepted beverages nor advertise rejected items: " + WaterSources.id(stack));
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void blackoutCancellationPreservesUnpaidSourceAndPaidReserve(GameTestHelper helper) {
        var player = player(helper);
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        var curios = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slot = curios.getStacksHandler("water").orElseThrow().getStacks();
        var empties = curios.getStacksHandler("empty_bottle").orElseThrow().getStacks();
        var source = item("farmersdelight:apple_cider");
        slot.setStackInSlot(0, source.copy());
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0);
        thirst.setThirst(19);
        java.util.function.Consumer<com.bettercontent.betterdrinkingwater.api.BeverageEvents.Check> deny = event -> {
            if (event.player() == player) event.setCanceled(true);
        };
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                net.minecraftforge.eventbus.api.EventPriority.NORMAL, false,
                com.bettercontent.betterdrinkingwater.api.BeverageEvents.Check.class, deny);
        try {
            tick(player);
            helper.assertTrue(ItemStack.matches(source, slot.getStackInSlot(0))
                            && empties.getStackInSlot(0).isEmpty() && PlayerWaterReserve.read(player) == null,
                    "Blackout veto before purchase must preserve source and produce no empty vessel or credit");
            var paid = new PlayerWaterReserve.Reserve(4, 5, 0.25, 3,
                    serving(List.of(effect("minecraft:speed", 1200, 1)), "heal2", 4, 2));
            PlayerWaterReserve.write(player, paid);
            var before = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();
            player.setHealth(4);
            tick(player);
            helper.assertTrue(before.equals(player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG))
                            && ItemStack.matches(source, slot.getStackInSlot(0)) && empties.getStackInSlot(0).isEmpty(),
                    "Blackout veto of paid sipping must not advance progress, repurchase or return another vessel");
            helper.assertTrue(thirst.getThirst() == 19 && player.getFoodData().getFoodLevel() == 10
                            && player.getFoodData().getSaturationLevel() == 0 && player.getHealth() == 4
                            && player.getActiveEffects().isEmpty(),
                    "Cancelled paid sip must release no hydration, food, saturation, healing or potion effects");
        } finally {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(deny);
        }
        tick(player);
        helper.assertTrue(thirst.getThirst() == 20 && PlayerWaterReserve.read(player).consumed() == 0.5
                        && player.hasEffect(MobEffects.MOVEMENT_SPEED),
                "Removing blackout resumes the same paid reserve, proving cancellation did not erase it");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void twentyTwoQuenchDrinkPaysUpfrontAndReturnsOneVessel(GameTestHelper helper) {
        var profile = DrinkCatalogue.entries().values().stream().filter(d -> d.quenching() == 22 && d.available())
                .findFirst().orElseThrow(() -> new AssertionError("Fixture needs a real 22-quenching beverage"));
        var player = player(helper);
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        var curios = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var source = curios.getStacksHandler("water").orElseThrow().getStacks();
        var empty = curios.getStacksHandler("empty_bottle").orElseThrow().getStacks();
        var stack = item(profile.id());
        stack.setCount(2);
        helper.assertTrue(source.isItemValid(0, stack), "Native water slot must admit beverages");
        source.setStackInSlot(0, stack);
        thirst.setThirst(20);
        tick(player);
        helper.assertTrue(source.getStackInSlot(0).getCount() == 2 && PlayerWaterReserve.read(player) == null
                        && empty.getStackInSlot(0).isEmpty(), "Full thirst must not buy food, effects or a vessel");
        int totalQuench = 0;
        for (int sip = 1; sip <= profile.hydration(); sip++) {
            thirst.setThirst(19);
            thirst.setQuenched(0);
            tick(player);
            totalQuench += thirst.getQuenched();
            helper.assertTrue(source.getStackInSlot(0).getCount() == 1,
                    "The entire serving is paid on first sip, never a second time");
            helper.assertTrue(thirst.getThirst() == 20, "Each sip restores only the missing hydration");
            var reserve = PlayerWaterReserve.read(player);
            helper.assertTrue(sip == profile.hydration() ? reserve == null
                            : reserve != null && reserve.quenched() == 22 && reserve.beverage() != null,
                    "22-quenching entitlement must survive native reserve validation until completion");
        }
        helper.assertTrue(totalQuench == 22, "Fractional native thirst credit must total exactly 22, not clamp to 20");
        helper.assertTrue(!profile.remainder().isEmpty() && empty.getStackInSlot(0).is(item(profile.remainder()).getItem())
                        && empty.getStackInSlot(0).getCount() == 1,
                "Upfront payment returns exactly the native vessel once");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void fractionalFoodAndSaturationCannotBeRepeatedAtFullThirst(GameTestHelper helper) {
        var player = player(helper);
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        var food = player.getFoodData();
        food.setFoodLevel(10);
        food.setSaturation(0);
        var serving = serving(List.of(), "none", 4, 2);
        PlayerWaterReserve.write(player, new PlayerWaterReserve.Reserve(4, 5, 0, 3, serving));
        thirst.setThirst(20);
        tick(player);
        helper.assertTrue(food.getFoodLevel() == 10 && food.getSaturationLevel() == 0
                        && PlayerWaterReserve.read(player).consumed() == 0,
                "Full thirst cannot advance an already paid beverage's food or effects");
        for (int i = 1; i <= 4; i++) {
            thirst.setThirst(19);
            tick(player);
            helper.assertTrue(food.getFoodLevel() == 10 + i
                            && Math.abs(food.getSaturationLevel() - i * 0.5F) < 0.00001F,
                    "Food and native saturation must follow the consumed fraction exactly");
        }
        helper.assertTrue(PlayerWaterReserve.read(player) == null, "Completed food has no repeatable reserve");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void effectDurationIsFractionalAndAmplifierIsNotDiluted(GameTestHelper helper) {
        var player = player(helper);
        var serving = serving(List.of(effect("minecraft:speed", 1200, 2)), "none", 0, 0);
        serving.release(player, 0, 0.25);
        var speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
        helper.assertTrue(speed != null && speed.getDuration() == 300 && speed.getAmplifier() == 2,
                "One quarter of a serving buys one quarter duration at its original strength");
        serving.release(player, 0.25, 0.5);
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() == 600,
                "Same-strength native timer must add rather than max/refresh");
        serving.release(player, 0.5, 1);
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() == 1200,
                "Partitioned effect totals its single-serving duration");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void weakerDrinkCannotDowngradeOrExtendStrongerNativeEffect(GameTestHelper helper) {
        var player = player(helper);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 777, 3));
        serving(List.of(effect("minecraft:speed", 1200, 1)), "none", 0, 0).release(player, 0, 1);
        var speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
        helper.assertTrue(speed.getAmplifier() == 3 && speed.getDuration() == 777,
                "A weaker beverage cannot steal or extend a stronger effect's entitlement");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void healTwoIsReleasedAsFractionalHealthNotRepeatedFullPotions(GameTestHelper helper) {
        var player = player(helper);
        player.setHealth(4);
        var serving = serving(List.of(), "heal2", 0, 0);
        for (int i = 0; i < 4; i++) {
            serving.release(player, i / 4.0, (i + 1) / 4.0);
            helper.assertTrue(Math.abs(player.getHealth() - (4.5 + i * 0.5)) < 0.00001,
                    "Each quarter of native melon juice's heal2 entitlement heals exactly half a heart point");
        }
        helper.assertTrue(player.getEffect(MobEffects.HEAL) == null, "Instant healing must not become a ticking potion");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void milkAndCocoaCleansingWaitForCompletion(GameTestHelper helper) {
        for (String action : List.of("milk", "poison", "cure_one", "cure_harmful_one", "cure_harmful_two")) {
            var player = player(helper);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
            var serving = serving(List.of(), action, 0, 0);
            serving.release(player, 0, 0.5);
            helper.assertTrue(player.hasEffect(MobEffects.POISON) && player.hasEffect(MobEffects.MOVEMENT_SPEED),
                    "A partial " + action + " serving cannot cleanse");
            serving.release(player, 0.5, 1);
            if (action.equals("cure_one")) {
                helper.assertTrue(player.getActiveEffects().size() == 1,
                        "FD milk bottle must cure precisely one randomly selected effect, not all effects");
            } else {
                helper.assertTrue(!player.hasEffect(MobEffects.POISON), "Completed serving must cleanse poison");
                helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED) != action.equals("milk"),
                        "Cocoa targets harmful effects only; milk bucket uses native milk curatives");
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void reserveRoundTripAndCloneKeepOnlyTheCorrectEntitlements(GameTestHelper helper) {
        var original = player(helper);
        var beverage = serving(List.of(effect("minecraft:speed", 1200, 1)), "none", 4, 2);
        var reserve = new PlayerWaterReserve.Reserve(6, 22, 0.5, 3, beverage);
        PlayerWaterReserve.write(original, reserve);
        var loaded = PlayerWaterReserve.read(original);
        helper.assertTrue(loaded != null && loaded.thirst() == 6 && loaded.quenched() == 22
                        && loaded.consumed() == 0.5 && loaded.beverage().save().equals(beverage.save()),
                "Validated reserve serialization must preserve the complete remaining beverage");
        var replacement = player(helper);
        WaterBottleCurio.onClone(new PlayerEvent.Clone(replacement, original, false));
        helper.assertTrue(PlayerWaterReserve.read(replacement) != null
                        && PlayerWaterReserve.read(replacement).beverage() != null,
                "Non-death native clone preserves paid beverage progress");
        var deadReplacement = player(helper);
        WaterBottleCurio.onClone(new PlayerEvent.Clone(deadReplacement, original, true));
        helper.assertTrue(PlayerWaterReserve.read(deadReplacement) == null,
                "Death must discard paid beverage benefits, not carry them into a new body");
        PlayerWaterReserve.write(original, new PlayerWaterReserve.Reserve(6, 8, 0.5, 3));
        WaterBottleCurio.onClone(new PlayerEvent.Clone(deadReplacement, original, true));
        helper.assertTrue(PlayerWaterReserve.read(deadReplacement) != null
                        && PlayerWaterReserve.read(deadReplacement).beverage() == null
                        && PlayerWaterReserve.read(deadReplacement).consumed() == 0.5,
                "Ordinary paid water keeps its existing death-preservation behavior");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void corruptBeveragePayloadCannotDegradeIntoFreeWater(GameTestHelper helper) {
        var valid = serving(List.of(effect("minecraft:speed", 1200, 1)), "none", 4, 2).save();
        for (String field : List.of("source", "food", "saturation", "nutrition", "effects", "action")) {
            var broken = valid.copy();
            broken.remove(field);
            helper.assertTrue(BeverageServing.read(broken) == null, "Missing field must fail closed: " + field);
        }
        var nan = valid.copy(); nan.putFloat("saturation", Float.NaN);
        var nutrition = valid.copy(); nutrition.getCompound("nutrition").putDouble("sugar", Double.POSITIVE_INFINITY);
        var unknown = valid.copy(); unknown.getList("effects", 10).getCompound(0).putString("id", "absent:effect");
        var excessive = valid.copy(); excessive.getList("effects", 10).getCompound(0).putInt("duration", 1200001);
        var wrongType = valid.copy(); var strings = new ListTag(); strings.add(StringTag.valueOf("not an effect"));
        wrongType.put("effects", strings);
        for (var payload : List.of(nan, nutrition, unknown, excessive, wrongType)) {
            helper.assertTrue(BeverageServing.read(payload) == null, "Corrupt entitlement must reject, never partially load");
        }
        var player = player(helper);
        var reserve = new PlayerWaterReserve.Reserve(6, 22, 0.5, 3, serving(List.of(), "none", 4, 2));
        for (double invalid : new double[]{-0.1, 1, Double.NaN, Double.POSITIVE_INFINITY}) {
            PlayerWaterReserve.write(player, reserve);
            player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)
                    .getCompound(PlayerWaterReserve.KEY).putDouble("consumed", invalid);
            helper.assertTrue(PlayerWaterReserve.read(player) == null,
                    "Invalid fractional progress cannot restore or duplicate paid entitlement");
        }
        PlayerWaterReserve.write(player, reserve);
        var data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)
                .getCompound(PlayerWaterReserve.KEY);
        // Find the serialized beverage by its typed payload, without coupling to its envelope key.
        String key = data.getAllKeys().stream().filter(k -> data.get(k) instanceof CompoundTag)
                .findFirst().orElseThrow();
        data.put(key, nan);
        helper.assertTrue(PlayerWaterReserve.read(player) == null,
                "A corrupt beverage invalidates its whole reserve, not merely its non-water costs");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void absorptionSlicesCannotReplayFullOnApplyShields(GameTestHelper helper) {
        var player = player(helper);
        var serving = serving(List.of(effect("minecraft:absorption", 1200, 0)), "none", 0, 0);
        serving.release(player, 0, 0.25);
        helper.assertTrue(Math.abs(player.getAbsorptionAmount() - 1) < 0.00001,
                "A quarter of Absorption I grants one shield HP, never the native full four");
        helper.assertTrue(player.getEffect(MobEffects.ABSORPTION).getDuration() == 300,
                "Absorption duration must also be proportional");
        player.setAbsorptionAmount(0); // Explicitly model the shield being spent, without unrelated damage hooks.
        serving.release(player, 0.25, 0.5);
        helper.assertTrue(Math.abs(player.getAbsorptionAmount() - 1) < 0.00001
                        && player.getEffect(MobEffects.ABSORPTION).getDuration() == 600,
                "Duration extension must not replay the native full-shield on-apply callback");
        player.removeEffect(MobEffects.ABSORPTION);
        player.setAbsorptionAmount(0);
        serving.release(player, 0.5, 0.75);
        helper.assertTrue(Math.abs(player.getAbsorptionAmount() - 1) < 0.00001
                        && player.getEffect(MobEffects.ABSORPTION).getDuration() == 300,
                "Reapplication after removal still grants only the newly consumed shield entitlement");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void additiveTimersRespectServingCapAndLongerPreexistingEffects(GameTestHelper helper) {
        var player = player(helper);
        var serving = serving(List.of(effect("minecraft:speed", 1200, 1)), "none", 0, 0);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1100, 1));
        serving.release(player, 0, 0.5);
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() == 1200,
                "Equal-amplifier additive extension must stop at the authored serving cap");
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 1));
        serving.release(player, 0, 1);
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() == 1800,
                "A serving cap must neither truncate nor extend a longer preexisting effect");
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void nativeRegenerationPhaseSurvivesRepeatedDurationTopOffs(GameTestHelper helper) {
        var player = player(helper);
        player.setHealth(4);
        var serving = serving(List.of(effect("minecraft:regeneration", 1200, 0)), "none", 0, 0);
        serving.release(player, 0, 0.01); // Twelve native effect ticks; no natural auto-sip tick is invoked.
        for (int tick = 1; tick <= 150; tick++) {
            var regeneration = player.getEffect(MobEffects.REGENERATION);
            helper.assertTrue(regeneration != null, "Replenished native regeneration must remain active");
            regeneration.tick(player, () -> {}); // Runs the real MobEffectInstance tick and cadence mixin.
            helper.assertTrue(Math.abs(player.getHealth() - (4 + tick / 50)) < 0.00001,
                    "Native regeneration heals once per 50 elapsed ticks, independent of replenished duration: " + tick);
            if (tick % 10 == 0 && tick < 150) serving.release(player, 0, 0.01);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = WaterSurvival.MOD_ID, template = "empty")
    public static void gamblerTeaChoosesOneCorrelatedOutcomeOncePerPurchase(GameTestHelper helper) {
        var player = player(helper);
        var tea = item("farmersrespite:strong_gamblers_tea");
        var profile = DrinkCatalogue.find(tea);
        helper.assertTrue(profile != null, "Real strong gambler tea fixture must be loaded");
        int wins = 0, losses = 0;
        player.getRandom().setSeed(0x6A4B1E2L);
        for (int trial = 0; trial < 128; trial++) {
            var debit = WaterSources.debit(player, tea);
            helper.assertTrue(debit != null, "Each native tea purchase must succeed");
            var serving = debit.portion().beverage();
            boolean glow = serving.effects().stream().anyMatch(e -> e.id().equals("minecraft:glowing"));
            boolean vision = serving.effects().stream().anyMatch(e -> e.id().equals("minecraft:night_vision"));
            boolean wither = serving.effects().stream().anyMatch(e -> e.id().equals("minecraft:wither"));
            helper.assertTrue(glow == vision && glow != wither,
                    "Strong gambler tea shares a win roll and uses its complement for wither, never neither/both");
            if (glow) wins++; else losses++;
            var reloaded = BeverageServing.read(serving.save());
            helper.assertTrue(reloaded != null && reloaded.effects().equals(serving.effects()),
                    "Persistence must preserve resolved outcomes without rerolling probabilities");
            player.removeAllEffects();
            reloaded.release(player, 0, 0.5);
            reloaded.release(player, 0.5, 1);
            helper.assertTrue(player.hasEffect(MobEffects.GLOWING) == glow
                            && player.hasEffect(MobEffects.NIGHT_VISION) == vision
                            && player.hasEffect(MobEffects.WITHER) == wither,
                    "Both fractional releases retain the outcome purchased upfront");
        }
        helper.assertTrue(wins > 0 && losses > 0, "Fixed-seed fixture must actually exercise both outcome branches");
        helper.succeed();
    }

    private static DrinkCatalogue.Effect effect(String id, int duration, int amplifier) {
        return new DrinkCatalogue.Effect(id, duration, amplifier, 1);
    }

    private static BeverageServing serving(List<DrinkCatalogue.Effect> effects, String action, int food, float saturation) {
        return new BeverageServing("minecraft:milk_bucket", food, saturation, new CompoundTag(), effects, action);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "beverage-test"));
    }

    private static ItemStack item(String id) {
        var key = new ResourceLocation(id);
        if (!ForgeRegistries.ITEMS.containsKey(key)) throw new AssertionError("Missing required native fixture item " + id);
        return new ItemStack(ForgeRegistries.ITEMS.getValue(key));
    }

    private static void tick(ServerPlayer player) {
        player.tickCount += 10 - player.tickCount % 10;
        WaterBottleCurio.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }
}
