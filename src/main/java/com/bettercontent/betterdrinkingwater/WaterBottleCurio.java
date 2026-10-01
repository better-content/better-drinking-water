package com.bettercontent.betterdrinkingwater;

import dev.ghen.thirst.api.ThirstHelper;
import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import com.bettercontent.betterdrinkingwater.WaterSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import top.theillusivec4.curios.api.CuriosApi;

public final class WaterBottleCurio {
    public static final String SLOT = "water";
    public static final String BACKPACK_SLOT = "better_backpack";
    public static final ResourceLocation PREDICATE = new ResourceLocation(WaterSurvival.MOD_ID, "water_bottle");
    private static final String FRACTION_KEY = "BetterContentSippedFraction";

    private WaterBottleCurio() {}

    public static void registerPredicate() {
        CuriosApi.registerCurioPredicate(PREDICATE, result -> isWaterBottle(result.stack()));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 10 != 0) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getStacksHandler(SLOT).ifPresent(slot -> {
            final ItemStack equipped = slot.getStacks().getStackInSlot(0);
            if (!isWaterBottle(equipped) || !ThirstHelper.itemRestoresThirst(equipped)) return;
            final ItemStack stack = equipped;
            player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent(thirst -> {
                final int bottleThirst = Math.max(0, ThirstHelper.getThirst(stack));
                final int bottleQuenched = Math.max(0, ThirstHelper.getQuenched(stack));
                int missingThirst = Math.max(0, 20 - thirst.getThirst());
                if (bottleThirst == 0 || missingThirst == 0) return;

                final ItemStack remainingBottles = stack.copy();
                double fraction = getBottleFraction(stack);
                int thirstRestored = 0;
                int quenchedRestored = 0;
                int bottlesConsumed = 0;

                while (missingThirst > 0 && !remainingBottles.isEmpty()) {
                    if (fraction == 0.0D && !WaterPurity.givePurityEffects(player, remainingBottles)) {
                        remainingBottles.shrink(1);
                        bottlesConsumed++;
                        break;
                    }

                    final WaterBottleConsumption.Result result = WaterBottleConsumption.calculate(
                            missingThirst, bottleThirst, bottleQuenched, fraction);
                    if (result.thirstRestored() == 0) break;
                    thirstRestored += result.thirstRestored();
                    quenchedRestored += result.quenchedRestored();
                    missingThirst -= result.thirstRestored();
                    fraction = result.remainingFraction();
                    if (!result.bottleCompleted()) break;
                    remainingBottles.shrink(1);
                    bottlesConsumed++;
                }

                if (thirstRestored > 0) {
                    thirst.drink(player, thirstRestored, quenchedRestored);
                    thirst.updateThirstData(player);
                    if (WaterPurity.getPurity(stack) == WaterPurity.MAX_PURITY) WaterSafetyEpisodes.purifiedDrunk(player);
                }
                if (bottlesConsumed > 0) {
                    // The fraction belongs to the first bottle still in the stack.
                    // A completed bottle must not leave its fraction on the next one.
                    if (!remainingBottles.isEmpty()) setBottleFraction(remainingBottles, fraction);
                    slot.getStacks().setStackInSlot(0, remainingBottles);
                    returnEmptyBottles(player, bottlesConsumed);
                } else if (thirstRestored > 0) {
                    setBottleFraction(remainingBottles, fraction);
                    slot.getStacks().setStackInSlot(0, remainingBottles);
                }
            });
        }));
    }

    static double getBottleFraction(final ItemStack bottle) {
        return bottle.hasTag()
                ? WaterBottleConsumption.normalizeFraction(bottle.getTag().getDouble(FRACTION_KEY))
                : 0.0D;
    }

    public static void keepOpenedBottleInExtractedStack(final ItemStack original, final ItemStack extracted) {
        if (!original.isEmpty() && isWaterBottle(extracted) && getBottleFraction(extracted) > 0.0D) {
            clearBottleFraction(original);
        }
    }

    private static void setBottleFraction(final ItemStack bottle, final double fraction) {
        final double normalized = WaterBottleConsumption.normalizeFraction(fraction);
        if (normalized == 0.0D) clearBottleFraction(bottle);
        else bottle.getOrCreateTag().putDouble(FRACTION_KEY, normalized);
    }

    private static void clearBottleFraction(final ItemStack bottle) {
        if (bottle.hasTag()) bottle.getTag().remove(FRACTION_KEY);
    }

    static void returnEmptyBottles(final ServerPlayer player, final int count) {
        final ItemStack emptyBottles = new ItemStack(Items.GLASS_BOTTLE, count);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getStacksHandler(BACKPACK_SLOT).ifPresent(slot -> {
            if (slot.getStacks().getSlots() == 0) return;
            final ItemStack backpack = slot.getStacks().getStackInSlot(0);
            if (!(backpack.getItem() instanceof BackpackItem)) return;
            backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).ifPresent(wrapper -> {
                final ItemStack remainder = wrapper.getInventoryHandler().insertItem(emptyBottles.copy(), false);
                emptyBottles.setCount(remainder.getCount());
            });
        }));
        for (int index = 0; index < 9 && !emptyBottles.isEmpty(); index++) {
            final ItemStack current = player.getInventory().getItem(index);
            if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, emptyBottles)) continue;
            final int room = current.isEmpty() ? emptyBottles.getMaxStackSize() : current.getMaxStackSize() - current.getCount();
            final int inserted = Math.min(room, emptyBottles.getCount());
            if (inserted <= 0) continue;
            if (current.isEmpty()) {
                final ItemStack placed = emptyBottles.copy();
                placed.setCount(inserted);
                player.getInventory().setItem(index, placed);
            } else current.grow(inserted);
            emptyBottles.shrink(inserted);
        }
        if (!emptyBottles.isEmpty()) player.drop(emptyBottles, false);
    }

    public static boolean isWaterBottle(ItemStack stack) {
        return stack.is(Items.POTION)
                && PotionUtils.getPotion(stack) == Potions.WATER
                && WaterPurity.isWaterFilledContainer(stack);
    }

}
