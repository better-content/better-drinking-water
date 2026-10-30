package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.api.ServingHydration;
import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/** Deliberately bounded native-container integration, not an arbitrary inventory search. */
public final class WaterSources {
    private static final Set<String> FLUID_ITEMS = Set.of(
            "tconstruct:copper_can", "tconstruct:seared_fuel_tank", "tconstruct:seared_fuel_gauge",
            "tconstruct:seared_ingot_tank", "tconstruct:seared_ingot_gauge", "tconstruct:seared_lantern",
            "tconstruct:seared_casting_tank", "tconstruct:scorched_fuel_tank", "tconstruct:scorched_fuel_gauge",
            "tconstruct:scorched_ingot_tank", "tconstruct:scorched_ingot_gauge", "tconstruct:scorched_lantern",
            "createdieselgenerators:canister", "pneumaticcraft:small_tank", "pneumaticcraft:medium_tank",
            "pneumaticcraft:large_tank", "pneumaticcraft:huge_tank");
    private static final Set<String> EMPTY_ITEMS = Set.of(
            "minecraft:glass_bottle", "minecraft:bucket", "thirst:terracotta_bowl", "aether:skyroot_bucket",
            "tconstruct:copper_can");

    private WaterSources() {}

    public static String id(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    public static boolean isEmptyVessel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!EMPTY_ITEMS.contains(id(stack)))
            return DrinkCatalogue.entries().values().stream().anyMatch(drink -> drink.remainder().equals(id(stack)));
        // Copper cans retain their item identity when filled.
        return !id(stack).equals("tconstruct:copper_can") || stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(handler -> handler.getFluidInTank(0).isEmpty()).orElse(false);
    }

    public static boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (DrinkCatalogue.find(stack) != null) return true;
        if (WaterBottleCurio.isWaterBottle(stack) || id(stack).equals("thirst:terracotta_water_bowl")
                || stack.is(Items.WATER_BUCKET) || id(stack).equals("aether:skyroot_water_bucket")) return true;
        if (!FLUID_ITEMS.contains(id(stack))) return false;
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
            boolean waterOrEmpty = true;
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fluid = handler.getFluidInTank(i);
                waterOrEmpty &= fluid.isEmpty() || isWater(fluid);
            }
            return waterOrEmpty;
        }).orElse(false);
    }

    /** Native manual intake and automatic reserve purchases share the authored finite quantities.
     * Drinkable potions retain their native effects; splash/lingering projectiles are never drinks.
     */
    public static java.util.Optional<ServingHydration.Profile> nativeHydration(ItemStack stack) {
        if (stack.isEmpty()) return java.util.Optional.empty();
        var drink = DrinkCatalogue.find(stack);
        if (drink != null) return java.util.Optional.of(new ServingHydration.Profile(drink.hydration(), drink.quenching()));
        if (stack.is(Items.POTION) || id(stack).equals("thirst:terracotta_water_bowl"))
            return java.util.Optional.of(new ServingHydration.Profile(4, 5));
        if (stack.is(Items.WATER_BUCKET) || id(stack).equals("aether:skyroot_water_bucket"))
            return java.util.Optional.of(new ServingHydration.Profile(16, 20));
        return java.util.Optional.empty();
    }

    /** Resolve a finite beverage on a private copy; caller commits the debit before releasing credit. */
    static Debit debit(ServerPlayer player, ItemStack source) {
        var drink = DrinkCatalogue.find(source);
        if (drink == null) return debit(source);
        if (!BeverageServing.allowed(player)) return null;
        var serving = BeverageServing.purchase(player, source, drink);
        if (serving == null) return null;
        ItemStack remaining = source.copy();
        remaining.shrink(1);
        ItemStack remainder = drink.remainder().isEmpty() ? ItemStack.EMPTY : item(drink.remainder());
        return new Debit(remaining, remainder,
                new PlayerWaterReserve.Reserve(drink.hydration(), drink.quenching(), 0, 3, serving));
    }

    /** Compute a water debit on a private copy; caller installs remaining/returns remainder before crediting it. */
    static Debit debit(ItemStack source) {
        if (!accepts(source)) return null;
        boolean bottle = WaterBottleCurio.isWaterBottle(source);
        boolean bowl = id(source).equals("thirst:terracotta_water_bowl");
        if (bottle || bowl) {
            int hydration = 4, quenched = 5;
            ItemStack remainder = bottle ? new ItemStack(Items.GLASS_BOTTLE) : item("thirst:terracotta_bowl");
            ItemStack remaining = source.copy();
            remaining.shrink(1);
            double legacy = bottle && source.hasTag()
                    ? WaterBottleConsumption.normalizeFraction(source.getTag().getDouble("BetterContentSippedFraction")) : 0;
            if (remaining.hasTag()) remaining.getTag().remove("BetterContentSippedFraction");
            return new Debit(remaining, remainder,
                    new PlayerWaterReserve.Reserve(hydration, quenched, legacy, WaterPurity.getPurity(source.copy())));
        }
        if (source.is(Items.WATER_BUCKET) || id(source).equals("aether:skyroot_water_bucket")) {
            ItemStack remaining = source.copy();
            remaining.shrink(1);
            ItemStack empty = source.is(Items.WATER_BUCKET) ? new ItemStack(Items.BUCKET) : item("aether:skyroot_bucket");
            return new Debit(remaining, empty, new PlayerWaterReserve.Reserve(16, 20, 0, WaterPurity.getPurity(source.copy())));
        }
        ItemStack single = source.copyWithCount(1);
        var handler = single.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
        if (handler == null) return null;
        FluidStack preview = handler.drain(250, IFluidHandler.FluidAction.SIMULATE);
        if (portion(preview) == null) return null;
        FluidStack drained = handler.drain(preview, IFluidHandler.FluidAction.EXECUTE);
        if (!preview.isFluidStackIdentical(drained)) return null;
        ItemStack updated = handler.getContainer(); // Native handlers can replace the item itself.
        if (source.getCount() == 1) return new Debit(updated, ItemStack.EMPTY, portion(drained));
        ItemStack remaining = source.copy();
        remaining.shrink(1);
        return new Debit(remaining, updated, portion(drained));
    }

    static boolean isWater(FluidStack fluid) {
        return !fluid.isEmpty() && fluid.getFluid() == Fluids.WATER;
    }

    /** 250 mB is one bowl portion (4 hydration / 5 quenching); round down, never mint water. */
    static PlayerWaterReserve.Reserve portion(FluidStack fluid) {
        if (!isWater(fluid) || fluid.getAmount() > 250) return null;
        int hydration = fluid.getAmount() * 4 / 250;
        int quenched = fluid.getAmount() * 5 / 250;
        return hydration == 0 ? null : new PlayerWaterReserve.Reserve(hydration, quenched, 0, WaterPurity.getPurity(fluid.copy()));
    }

    private static ItemStack item(String id) {
        var key = new ResourceLocation(id);
        return ForgeRegistries.ITEMS.containsKey(key) ? new ItemStack(ForgeRegistries.ITEMS.getValue(key)) : ItemStack.EMPTY;
    }

    record Debit(ItemStack remaining, ItemStack remainder, PlayerWaterReserve.Reserve portion) {}
}
