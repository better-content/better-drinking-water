package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.WaterQuality;
import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Collapse native quality reads, writes, effects and presentation to two endpoints. */
@Mixin(value = WaterPurity.class, remap = false)
public abstract class WaterPurityMixin {
    @Inject(method = {
            "getPurity(Lnet/minecraft/world/item/ItemStack;)I",
            "getPurity(Lnet/minecraftforge/fluids/FluidStack;)I",
            "getBlockPurity(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"
    }, at = @At("RETURN"), cancellable = true)
    private static void waterSurvival$quality(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(WaterQuality.normalize(cir.getReturnValue()));
    }

    @Inject(method = "getBlockPurity(Lnet/minecraft/world/level/block/state/BlockState;)I", at = @At("RETURN"), cancellable = true)
    private static void waterSurvival$blockQuality(CallbackInfoReturnable<Integer> cir) {
        int purity = cir.getReturnValue();
        cir.setReturnValue(purity == -1 ? -1 : WaterQuality.normalize(purity));
    }

    @ModifyVariable(method = {"getPurityText", "getPurityColor"},
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int waterSurvival$qualityArgument(int purity) {
        return purity == -1 ? -1 : WaterQuality.normalize(purity);
    }

    @ModifyVariable(method = "givePurityEffects(Lnet/minecraft/world/entity/player/Player;I)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int waterSurvival$effectQuality(int purity) {
        return WaterQuality.normalize(purity);
    }

    @Inject(method = "addPurity(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private static void waterSurvival$writeItem(ItemStack stack, int purity, CallbackInfoReturnable<ItemStack> cir) {
        cir.getReturnValue().getOrCreateTag().putInt("Purity", WaterQuality.normalize(purity));
    }

    @Inject(method = "addPurity(Lnet/minecraftforge/fluids/FluidStack;I)Lnet/minecraftforge/fluids/FluidStack;", at = @At("RETURN"))
    private static void waterSurvival$writeFluid(FluidStack stack, int purity, CallbackInfoReturnable<FluidStack> cir) {
        cir.getReturnValue().getOrCreateTag().putInt("Purity", WaterQuality.normalize(purity));
    }

    // Native code skips effects entirely for untagged water. It is dirty too.
    @Inject(method = "givePurityEffects(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void waterSurvival$untaggedEffects(Player player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (WaterPurity.isWaterFilledContainer(stack)) {
            cir.setReturnValue(WaterPurity.givePurityEffects(player, WaterPurity.getPurity(stack)));
        }
    }
}
