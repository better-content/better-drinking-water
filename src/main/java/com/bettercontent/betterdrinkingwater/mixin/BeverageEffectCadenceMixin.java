package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.BeverageEffectCadence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only explicitly managed beverage periodic effects change scheduling; native effects otherwise remain native. */
@Mixin(MobEffectInstance.class)
public abstract class BeverageEffectCadenceMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;isDurationEffectTick(II)Z"))
    private boolean drinks$phase(MobEffect effect, int duration, int amplifier, LivingEntity entity, Runnable changed) {
        return BeverageEffectCadence.shouldTick(entity, effect, duration, amplifier);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void drinks$ended(LivingEntity entity, Runnable changed, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) BeverageEffectCadence.ended(entity, ((MobEffectInstance) (Object) this).getEffect());
    }
}
