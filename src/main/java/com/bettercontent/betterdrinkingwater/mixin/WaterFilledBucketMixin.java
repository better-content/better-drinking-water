package com.bettercontent.betterdrinkingwater.mixin;

import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidUtil.class, remap = false)
public abstract class WaterFilledBucketMixin {
    @Inject(method = "getFilledBucket", at = @At("RETURN"), cancellable = true)
    private static void waterSurvival$write(FluidStack fluid, CallbackInfoReturnable<ItemStack> cir) {
        if (!fluid.isEmpty() && fluid.getFluid() == Fluids.WATER && !cir.getReturnValue().isEmpty()) {
            cir.setReturnValue(WaterPurity.addPurity(cir.getReturnValue(), WaterPurity.getPurity(fluid.copy())));
        }
    }
}
