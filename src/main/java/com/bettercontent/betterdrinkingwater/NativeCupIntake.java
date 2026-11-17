package com.bettercontent.betterdrinkingwater;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import java.util.function.Supplier;

/** Scoped native item finish; neither forges Forge Finish nor changes KC effects/containers. */
public final class NativeCupIntake {
    private NativeCupIntake() {}
    /** Thirst's later Forge Finish callback must not award the same paid hydration.
     * A one-callback, server-thread token is minted ONLY after actual native debit. */
    private record Paid(ServerPlayer player, ItemStack original, long gameTime) {}
    private static final ThreadLocal<Paid> PAID = new ThreadLocal<>();
    public static boolean takePaidHydration(LivingEntity actor, ItemStack finished) {
        Paid paid = PAID.get();
        if (paid == null || paid.player() != actor || paid.gameTime() != actor.level().getGameTime()
                || !ItemStack.isSameItemSameTags(paid.original(), finished)) return false;
        PAID.remove();
        return true;
    }

    public static ItemStack finish(ItemStack stack, Level level, LivingEntity actor, Supplier<ItemStack> nativeFinish) {
        if (level.isClientSide || !(actor instanceof ServerPlayer player)
                || player instanceof FakePlayer || player.connection == null
                || player.server.getPlayerList().getPlayer(player.getUUID()) != player
                || !NativeDrinkFacts.isCup(stack)) return nativeFinish.get();
        // A malformed CLAIMED receipt is rejected before KC's tea effects, milk cure,
        // physical shrink, or container return. Untagged KC remains native.
        PAID.remove(); // an earlier native use without matching Forge Finish cannot suppress a later use
        var receipt = NativeDrinkFacts.read(stack);
        if (receipt.isEmpty() || player.isCreative() || player.isSpectator()) return nativeFinish.get();
        var facts = receipt.get();
        var hydration = facts.hydration();
        boolean managed = facts.complete() || hydration.isPresent();
        // The existing Check is a prepayment veto. Do NOT consume a claimed,
        // managed cup and discard its cost/benefit after a blackout veto.
        if (!NativeCupAdmission.nativeUseAllowed(managed, managed && BeverageServing.allowed(player))) return stack;
        BeverageServing nutrition = facts.complete() ? BeverageServing.purchaseNative(player, stack, facts) : null;
        int count = stack.getCount();
        ItemStack charged = hydration.isPresent() ? stack.copyWithCount(1) : ItemStack.EMPTY;
        ItemStack nativeResult = nativeFinish.get();
        if (stack.getCount() == count - 1 && count >= 1) {
            // A native stack of one may return an empty cup in another ItemStack; the
            // observed original shrink is the payment, not the returned container.
            hydration.ifPresent(profile -> {
                if (CompletedFoodHydration.completeNativeCup(player, profile))
                    PAID.set(new Paid(player, charged, level.getGameTime()));
            });
            if (nutrition != null) nutrition.releaseNative(player);
        }
        return nativeResult;
    }
}
