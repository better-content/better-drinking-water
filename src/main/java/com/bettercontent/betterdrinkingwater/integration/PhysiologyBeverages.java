package com.bettercontent.betterdrinkingwater.integration;

import com.bettercontent.betterdrinkingwater.api.BeverageEvents;
import com.bettercontent.bettersurvivalphysiology.compat.SpiceBridge;
import com.bettercontent.betterdrinkingwater.NativeDrinkFacts;
import com.bettercontent.betterdrinkingwater.PlantDrinks;
import com.bettercontent.betterdrinkingwater.WaterSources;
import com.bettercontent.bettersurvivalphysiology.diagnostics.RuntimeDiagnostics;
import com.bettercontent.bettersurvivalphysiology.diagnostics.PlaytestLog;
import com.bettercontent.bettersurvivalphysiology.metabolism.ConsumableProfiles;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import com.bettercontent.bettersurvivalphysiology.nutrition.ServingVectors;
import com.bettercontent.bettersurvivalphysiology.runtime.SalienceEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Loaded only when Better Drinking Water is present. Never runs native whole-item completion. */
public final class PhysiologyBeverages implements com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.Provider {
    public static final String DIET = "diet";
    public static final String SUGAR = "sugar";
    public static final String ALCOHOL = "alcohol";
    public static final String EDIBLE = "edible";
    public static final String ITEM = "item";

    private PhysiologyBeverages() {}

    public static void register() {
        com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.register(
                new net.minecraft.resources.ResourceLocation("better_drinking_water", "beverage_profiles"),
                new PhysiologyBeverages());
        MinecraftForge.EVENT_BUS.register(PhysiologyBeverages.class);
        SpiceBridge.registerExternalCompletionOwner(new net.minecraft.resources.ResourceLocation(
                "better_survival_physiology", "typed_beverage"), PhysiologyBeverages::ownsCompleted);
    }

    /** New native plant bottles release their explicit typed entitlement themselves. */
    @Override public boolean manages(net.minecraft.world.item.ItemStack stack) {
        return com.bettercontent.betterdrinkingwater.PlantDrinks.contains(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** Spice and native Diet whole-Finish listeners yield ONLY after an actual
     * fully paid KC receipt has a typed Profile owner. This must not replace
     * manages(), which controls plant-only source/nutrition classification. */
    @Override public boolean completedExternally(net.minecraft.world.item.ItemStack stack) {
        return ownsCompleted(stack);
    }
    public static boolean ownsCompleted(net.minecraft.world.item.ItemStack stack) {
        var claim = com.bettercontent.betterdrinkingwater.NativeDrinkFacts.read(stack);
        return NativeCupOwnership.typedWholeCompletion(
                PlantDrinks.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()),
                claim.isPresent(), claim.map(com.bettercontent.betterdrinkingwater.NativeDrinkFacts.Receipt::complete).orElse(false));
    }

    @Override public java.util.Map<String, Double> plantGains(net.minecraft.world.item.ItemStack stack) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        java.util.Map<String, Double> gains = new java.util.LinkedHashMap<>(plantSources(id));
        double sugar = com.bettercontent.betterdrinkingwater.PlantDrinks.nutrient(id, SUGAR);
        gains = new java.util.LinkedHashMap<>(new ServingVectors.SourceServing(gains).canonicalIngredient(sugar, 0).gains());
        double units = SpiceBridge.resolveUnits(null, stack);
        if (units > 0) gains.put("spice", SpiceBridge.dietGain(units));
        return java.util.Map.copyOf(gains);
    }

    @Override public double alcoholPotency(String itemId) {
        return com.bettercontent.bettersurvivalphysiology.config.SalienceConfig.alcoholPotencies()
                .getOrDefault(itemId, 0.0);
    }

    @Override public double alcoholReduction(double alcohol, double carbs) {
        return AlcoholPolicy.reduction(alcohol, carbs);
    }

    @Override public double alcoholImpairment(double alcohol, double carbs) {
        return AlcoholPolicy.impairment(alcohol, carbs);
    }

    @Override public double tickAlcohol(double alcohol, double carbs) {
        return AlcoholPolicy.clearance(alcohol, carbs);
    }

    @Override public void updateBlackout(net.minecraft.server.level.ServerPlayer player,
            com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState state, float load) {
        if (!AlcoholPolicy.blackout(load)) return;
        state.blackout = true;
        state.blackoutX = player.getX(); state.blackoutY = player.getY(); state.blackoutZ = player.getZ();
    }

    @Override public void clearBlackout(com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState state,
            float load) {
        if (state.blackout && AlcoholPolicy.sober(load)) state.blackout = false;
    }

    @Override public void applyNativeAlcohol(net.minecraft.server.level.ServerPlayer player,
            net.minecraft.world.item.ItemStack consumed,
            com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState state) {
        DietBridge.awardAlcohol(player, ConsumableProfiles.alcohol(consumed));
        float load = DietBridge.snapshot(player).alcohol();
        if (AlcoholPolicy.blackout(load)) {
            updateBlackout(player, state, load);
            SalienceEvents.alcoholBlackoutActivated(player);
        }
    }

    @Override public void suppressBrewing(net.minecraft.server.level.ServerPlayer player) {
        BrewingSuppression.suppressNumbedHearts(player);
    }

    private static java.util.Map<String, Double> plantSources(String id) {
        java.util.Map<String, Double> source = new java.util.LinkedHashMap<>();
        for (String name : java.util.List.of("proteins", "grains", "fruits", "sugars", "vegetables", "dairy")) {
            double dose = com.bettercontent.betterdrinkingwater.PlantDrinks.nutrient(id, name);
            if (dose > 0) source.put(name, dose);
        }
        return source;
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void preventNativeWholePlantAward(com.illusivesoulworks.diet.api.DietEvent.ConsumeItemStack event) {
        // Diet's native Finish listener independently calls consume(stack), even without super.
        // These exact new bottles already awarded their typed entitlement. Never veto native drinks.
        if (ownsCompleted(event.getStack())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void profile(BeverageEvents.Profile event) {
        if (event.paidInputs().isPresent()) {
            nativeDrinkProfile(event, event.paidInputs().get());
            return;
        }
        var stack = event.stack();
        CompoundTag gains = new CompoundTag();
        event.nutrition().putDouble(SUGAR, 0.0);
        String drinkId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (com.bettercontent.betterdrinkingwater.PlantDrinks.contains(drinkId)) {
            // Authored 250 mB entitlements. No native whole-food/Diet finish callback is replayed.
            // Plant metadata is one ingredient with source-label overlaps. Collapse to the
            // canonical five BEFORE fractions, then keep the exact captured entitlement.
            double sugar = com.bettercontent.betterdrinkingwater.PlantDrinks.nutrient(drinkId, SUGAR);
            var canonical = new ServingVectors.SourceServing(plantSources(drinkId)).canonicalIngredient(sugar, 0);
            canonical.gains().forEach((group, amount) -> gains.putDouble(group, BeverageNutrition.boundedGain(amount)));
        } else if (stack.is(Items.MILK_BUCKET)) {
            // Explicit physiology award, not an additional native Diet award.
            ServingVectors.milk().gains().forEach(gains::putDouble);
            // Milk has no Vegetable award.
        } else {
            var resolved = ConsumableProfiles.dietGains(event.player(), stack);
            resolved.ifPresent(serving -> {
                for (NutritionSnapshot.Group ordinary : NutritionSnapshot.Group.values()) {
                    if (serving.containsKey(ordinary.id))
                        gains.putDouble(ordinary.id, BeverageNutrition.boundedGain(serving.get(ordinary.id)));
                }
            });
            // Capture once, before payment. Canonical native Carb amounts already represent
            // the sugar-bearing source; no second hidden sugar balance can be awarded.
            if (!gains.contains("carbs")) resolved.ifPresent(serving -> {
                double carbs = ConsumableProfiles.servingSugar(stack, serving);
                if (carbs > 0) gains.putDouble("carbs", BeverageNutrition.boundedGain(carbs));
            });
        }
        // Other groups retain their established fractional entitlement. Spice is captured before payment,
        // then paid consumed portions settle against this dose, never the later held item.
        gains.remove("spice");
        double units = SpiceBridge.resolveUnits(event.player(), stack);
        if (units > 0) {
            gains.putDouble(BeverageSpiceDose.MANTISSA, BeverageSpiceDose.mantissa(units));
            gains.putDouble(BeverageSpiceDose.EXPONENT, BeverageSpiceDose.exponent(units));
        }
        double alcohol = BeverageNutrition.boundedGain(ConsumableProfiles.alcohol(stack));
        if (alcohol > 0) gains.putDouble("alcohol", Math.max(gains.getDouble("alcohol"), alcohol));
        event.nutrition().put(DIET, gains);
        event.nutrition().putDouble(ALCOHOL, 0.0); // legacy profile scalar is not a second award
        event.nutrition().putBoolean(EDIBLE, stack.isEdible());
        event.nutrition().putString(ITEM, net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).toString());
    }

    /** Only the real consumer resolves paid original sources. One native Diet result
     * is already canonical; each source mirror is merged before distinct sources
     * are summed, and only the final per-cup amount is bounded. */
    private static void nativeDrinkProfile(BeverageEvents.Profile event,
            com.bettercontent.betterdrinkingwater.NativeDrinkFacts.Receipt paid) {
        if (!paid.complete()) return; // Explicitly unresolved, not a zero profile.
        java.util.List<net.minecraft.world.item.ItemStack> sources = new java.util.ArrayList<>();
        sources.add(paid.base());
        for (var input : paid.inputs()) sources.add(input.original());
        java.util.Map<String, Double> whole = new java.util.HashMap<>();
        double spiceUnits = 0;
        for (var source : sources) {
            java.util.Map<String, Double> canonical;
            if (source.is(Items.MILK_BUCKET)) canonical = ServingVectors.milk().gains();
            else canonical = ConsumableProfiles.dietGains(event.player(), source)
                    .orElseThrow(() -> new IllegalStateException("Unresolved paid native drink Diet source"));
            var serving = new ServingVectors.CanonicalServing(canonical).resolveNativeMirrors(
                    ConsumableProfiles.servingSugar(source, canonical), ConsumableProfiles.alcohol(source));
            whole = new java.util.HashMap<>(NativePaidBatch.add(
                    whole, serving.gains()));
            spiceUnits += SpiceBridge.resolveUnits(event.player(), source);
        }
        if (!Double.isFinite(spiceUnits) || spiceUnits < 0)
            throw new IllegalArgumentException("Invalid captured paid native drink Spice units");
        CompoundTag result = new CompoundTag(), gains = new CompoundTag();
        NativePaidBatch.perCup(whole, paid.yield())
                .forEach(gains::putDouble);
        double perCupUnits = spiceUnits / paid.yield();
        gains.putDouble(BeverageSpiceDose.MANTISSA, BeverageSpiceDose.mantissa(perCupUnits));
        gains.putDouble(BeverageSpiceDose.EXPONENT, BeverageSpiceDose.exponent(perCupUnits));
        result.putString("vector_type", "canonical_five");
        result.put(DIET, gains);
        result.putDouble(SUGAR, 0); result.putDouble(ALCOHOL, 0);
        result.putBoolean(EDIBLE, false);
        result.putString(ITEM, com.bettercontent.betterdrinkingwater.WaterSources.id(event.stack()));
        event.nutrition().merge(result);
    }

    @SubscribeEvent
    public static void check(BeverageEvents.Check event) {
        if (MetabolicStateStore.get(event.player()).blackout) {
            event.setCanceled(true);
            RuntimeDiagnostics.playerEvent(event.player(), "beverage.check", "vetoed", "blackout");
        }
    }

    @SubscribeEvent
    public static void sip(BeverageEvents.Sip event) {
        if (BeverageNutrition.fraction(event.from(), event.to()) == 0.0) {
            RuntimeDiagnostics.playerEvent(event.player(), "beverage.release", "rejected", "invalid_or_empty_fraction"); return;
        }
        var player = event.player();
        var state = MetabolicStateStore.get(player);
        if (state.blackout) {
            RuntimeDiagnostics.playerEvent(player, "beverage.release", "rejected", "blackout_defense"); return;
        } // Defensive: engine checks before purchase and paid release too.
        var before = DietBridge.snapshot(player);
        double sugarBefore = before.carbs(), alcoholBefore = before.alcohol();
        CompoundTag nutrition = event.nutrition();
        CompoundTag gains = nutrition.getCompound(DIET);
        if (gains.contains(BeverageSpiceDose.MANTISSA) != gains.contains(BeverageSpiceDose.EXPONENT))
            throw new IllegalArgumentException("Incomplete captured Spice dose");
        double wholeUnits = BeverageSpiceDose.decode(gains.getDouble(BeverageSpiceDose.MANTISSA), gains.getDouble(BeverageSpiceDose.EXPONENT));
        double portionUnits = !player.isCreative() && !player.isSpectator()
                ? wholeUnits * BeverageNutrition.fraction(event.from(), event.to()) : 0;
        java.util.Map<String, Double> paid = new java.util.HashMap<>();
        for (NutritionSnapshot.Group group : NutritionSnapshot.Group.values()) {
            if (group == NutritionSnapshot.Group.SPICE) continue; // paid units are not an item preview
            double amount = BeverageNutrition.award(gains.getDouble(group.id), event.from(), event.to());
            if (amount > 0) paid.put(group.id, amount);
        }
        var portion = new com.bettercontent.bettersurvivalphysiology.nutrition.CommittedDietIntake.Portion(
                new ServingVectors.CanonicalServing(paid), portionUnits);
        var diet = DietBridge.tracker(player);
        if (diet.isPresent() && diet.get().isActive()) {
            var id = event.to() == 1.0 && !gains.isEmpty()
                    ? net.minecraft.resources.ResourceLocation.tryParse(nutrition.getString(ITEM)) : null;
            com.bettercontent.bettersurvivalphysiology.nutrition.CommittedDietIntake.commit(
                    player, portion, nutrition.getBoolean(EDIBLE), event.to() == 1.0, id);
        } else {
            SalienceEvents.beverageFeedback(player, before, sugarBefore, alcoholBefore,
                    nutrition.getBoolean(EDIBLE), event.to() == 1.0);
            MetabolicStateStore.save(player);
        }
        if (portionUnits > 0) SpiceBridge.applyCompletedUnits(player, portionUnits);
        if (RuntimeDiagnostics.LOG.enabled("beverage")) {
            NutritionSnapshot settled = DietBridge.snapshot(player);
            double sugarAfter = settled.carbs(), alcoholAfter = settled.alcohol();
            com.bettercontent.bettersurvivalphysiology.diagnostics.FeatureObservation.release(RuntimeDiagnostics.LOG,
                () -> new PlaytestLog.Context("server", player.level().dimension().location().toString(),
                    player.level().getGameTime(), player.getUUID().toString(), ""),
                event.from(), event.to(), sugarBefore, sugarAfter, alcoholBefore, alcoholAfter);
        }
    }
}
