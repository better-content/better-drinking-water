package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.WaterSources;
import dev.ghen.thirst.api.ThirstHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native manual intake/Thirst tooltips cannot resurrect stale keyword/config/native quantities.
 * Changes entitlement, not payment, purity, native effects or completion timing.
 */
@Mixin(value = ThirstHelper.class, remap = false)
public abstract class NativeDrinkHydrationMixin {
    @Inject(method = "getThirst", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hydration(ItemStack stack, CallbackInfoReturnable<Integer> ci) {
        WaterSources.nativeHydration(stack).ifPresent(profile -> ci.setReturnValue((int) profile.hydration()));
    }
    @Inject(method = "getQuenched", at = @At("HEAD"), cancellable = true, remap = false)
    private static void quenching(ItemStack stack, CallbackInfoReturnable<Integer> ci) {
        WaterSources.nativeHydration(stack).ifPresent(profile -> ci.setReturnValue((int) profile.quenching()));
    }
    @Inject(method = "itemRestoresThirst", at = @At("HEAD"), cancellable = true, remap = false)
    private static void finiteDrink(ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) ci.setReturnValue(false);
        else if (WaterSources.nativeHydration(stack).isPresent()) ci.setReturnValue(true);
    }
}
