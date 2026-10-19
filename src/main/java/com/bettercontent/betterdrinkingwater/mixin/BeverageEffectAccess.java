package com.bettercontent.betterdrinkingwater.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Duration-only extension must not replay absorption or custom on-apply actions. */
@Mixin(MobEffectInstance.class)
public interface BeverageEffectAccess {
    @Accessor("duration") void drinks$duration(int duration);
}
