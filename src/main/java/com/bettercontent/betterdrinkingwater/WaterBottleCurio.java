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
    public static final String EMPTY_BOTTLE_SLOT = "empty_bottle";
    public static final ResourceLocation EMPTY_BOTTLE_PREDICATE = new ResourceLocation(WaterSurvival.MOD_ID, "empty_bottle");
    public static final String BACKPACK_SLOT = "better_backpack";
    public static final ResourceLocation PREDICATE = new ResourceLocation(WaterSurvival.MOD_ID, "water_bottle");
    private static final String FRACTION_KEY = "BetterContentSippedFraction";

    private WaterBottleCurio() {}

    public static void registerPredicate() {
        CuriosApi.registerCurioPredicate(PREDICATE, result -> isWaterBottle(result.stack()));
        CuriosApi.registerCurioPredicate(EMPTY_BOTTLE_PREDICATE, result -> result.stack().is(Items.GLASS_BOTTLE));
    }

    @SubscribeEvent
    public static void onClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (!event.getEntity().level().isClientSide) PlayerWaterReserve.copy(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 10 != 0) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent(thirst -> {
            int missing = Math.max(0, 20 - thirst.getThirst());
            if (missing == 0) return;
            PlayerWaterReserve.Reserve reserve = PlayerWaterReserve.read(player);
            PlayerWaterReserve.write(player, reserve); // Discard malformed/unsupported records.
            int restored = 0, quenched = 0;
            boolean purified = false;
            while (missing > 0) {
                if (reserve == null) {
                    var slot = CuriosApi.getCuriosInventory(player).resolve()
                            .flatMap(handler -> handler.getStacksHandler(SLOT)).orElse(null);
                    if (slot == null) break;
                    ItemStack bottle = slot.getStacks().getStackInSlot(0).copy();
                    if (!isWaterBottle(bottle) || !ThirstHelper.itemRestoresThirst(bottle)) break;
                    int total = ThirstHelper.getThirst(bottle), totalQuenched = ThirstHelper.getQuenched(bottle);
                    int purity = WaterPurity.getPurity(bottle);
                    if (total <= 0 || total > 20 || totalQuenched < 0 || totalQuenched > 20
                            || purity < 0 || purity > WaterPurity.MAX_PURITY) break;
                    double legacy = bottle.hasTag()
                            ? WaterBottleConsumption.normalizeFraction(bottle.getTag().getDouble(FRACTION_KEY)) : 0;
                    ItemStack remaining = bottle.copy();
                    remaining.shrink(1);
                    if (remaining.hasTag()) remaining.getTag().remove(FRACTION_KEY);
                    slot.getStacks().setStackInSlot(0, remaining);
                    returnEmptyBottles(player, 1);
                    if (!WaterPurity.givePurityEffects(player, bottle)) break;
                    reserve = new PlayerWaterReserve.Reserve(total, totalQuenched, legacy, purity);
                }
                var result = WaterBottleConsumption.calculate(missing, reserve.thirst(), reserve.quenched(), reserve.consumed());
                restored += result.thirstRestored();
                quenched += result.quenchedRestored();
                missing -= result.thirstRestored();
                purified |= result.thirstRestored() > 0 && reserve.purity() == WaterPurity.MAX_PURITY;
                reserve = result.bottleCompleted() ? null : new PlayerWaterReserve.Reserve(
                        reserve.thirst(), reserve.quenched(), result.remainingFraction(), reserve.purity());
                PlayerWaterReserve.write(player, reserve);
                if (result.thirstRestored() == 0) break;
            }
            if (restored > 0) {
                thirst.drink(player, restored, quenched);
                thirst.updateThirstData(player);
                if (purified) WaterSafetyEpisodes.purifiedDrunk(player);
            }
        });
    }

    static void returnEmptyBottles(final ServerPlayer player, final int count) {
        final ItemStack emptyBottles = new ItemStack(Items.GLASS_BOTTLE, count);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getStacksHandler(EMPTY_BOTTLE_SLOT).ifPresent(slot -> {
            for (int index = 0; index < slot.getStacks().getSlots() && !emptyBottles.isEmpty(); index++) {
                final ItemStack remainder = slot.getStacks().insertItem(index, emptyBottles.copy(), false);
                emptyBottles.setCount(remainder.getCount());
            }
        }));
        if (emptyBottles.isEmpty()) return;
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
