package com.bettercontent.betterdrinkingwater.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Thirst's ingredient hook dereferences getTag on untagged bowls/buckets. Supply a private dirty copy. */
@Mixin(AbstractCookingRecipe.class)
public abstract class WaterCookingInputMixin {
    @ModifyExpressionValue(method = "matches", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/Container;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack waterSurvival$untaggedWater(ItemStack input) {
        if (!input.hasTag() && WaterPurity.isWaterFilledContainer(input)) {
            return WaterPurity.addPurity(input.copy(), 0);
        }
        return input;
    }
}
