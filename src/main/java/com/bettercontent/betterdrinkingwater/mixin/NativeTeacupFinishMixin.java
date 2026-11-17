package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativeCupIntake;
import com.github.ysbbbbbb.kaleidoscopecookery.item.TeacupItem;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/** Exact KC nonedible native tea item; no generic item callback owner. */
@Mixin(TeacupItem.class)
public abstract class NativeTeacupFinishMixin {
    @WrapMethod(method = "finishUsingItem")
    private ItemStack water$paidFinish(ItemStack stack, Level level, LivingEntity actor, Operation<ItemStack> nativeFinish) {
        return NativeCupIntake.finish(stack, level, actor, () -> nativeFinish.call(stack, level, actor));
    }
}
