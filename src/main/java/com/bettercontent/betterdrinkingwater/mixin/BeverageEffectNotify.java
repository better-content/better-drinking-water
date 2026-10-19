package com.bettercontent.betterdrinkingwater.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface BeverageEffectNotify {
    @Invoker("onEffectUpdated") void drinks$updated(MobEffectInstance effect, boolean reapplyAttributes, Entity source);
}
