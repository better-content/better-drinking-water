package com.bettercontent.betterdrinkingwater.mixin;

import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge otherwise drops the bucket item's purity when exposing its fluid capability. */
@Mixin(value = FluidBucketWrapper.class, remap = false)
public abstract class WaterBucketQualityMixin {
    @Shadow protected ItemStack container;

    @Inject(method = "getFluid", at = @At("RETURN"), cancellable = true)
    private void waterSurvival$read(CallbackInfoReturnable<FluidStack> cir) {
        FluidStack fluid = cir.getReturnValue();
        if (!fluid.isEmpty() && fluid.getFluid() == Fluids.WATER) {
            cir.setReturnValue(WaterPurity.addPurity(fluid.copy(), WaterPurity.getPurity(container.copy())));
        }
    }
}
