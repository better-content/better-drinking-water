package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativeCupIntake;
import com.github.ysbbbbbb.kaleidoscopecookery.item.ClayPotMilkTeaItem;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/** Exact KC nonedible native milk cup; native harmful-effect cure stays intact. */
@Mixin(ClayPotMilkTeaItem.class)
public abstract class NativeMilkTeaFinishMixin {
    @WrapMethod(method = "finishUsingItem")
    private ItemStack water$paidFinish(ItemStack stack, Level level, LivingEntity actor, Operation<ItemStack> nativeFinish) {
        return NativeCupIntake.finish(stack, level, actor, () -> nativeFinish.call(stack, level, actor));
    }
}
