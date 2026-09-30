package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.WaterBottleCurio;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Plain drinkable water can fill the Curios water slot as a real stack. */
@Mixin(ItemStack.class)
public abstract class WaterPotionStackSizeMixin {
    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void waterSurvival$stackDrinkableWater(final CallbackInfoReturnable<Integer> callback) {
        if (WaterBottleCurio.isWaterBottle((ItemStack) (Object) this)) callback.setReturnValue(64);
    }

    @Inject(method = "split", at = @At("RETURN"))
    private void waterSurvival$keepFractionOnOpenedBottle(
            final int amount, final CallbackInfoReturnable<ItemStack> callback) {
        WaterBottleCurio.keepOpenedBottleInExtractedStack(
                (ItemStack) (Object) this, callback.getReturnValue());
    }
}
