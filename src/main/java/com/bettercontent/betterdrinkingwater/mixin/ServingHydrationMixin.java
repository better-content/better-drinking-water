package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.api.ServingHydration;
import dev.ghen.thirst.content.thirst.PlayerThirst;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers Thirst's edible/potion/DrinkableItem and Finish-event paths at their shared entry.
 * Only the provider's completed-serving call may credit managed food. Unmanaged defaults stay native.
 */
@Mixin(value = PlayerThirst.class, remap = false)
public abstract class ServingHydrationMixin {
    @Inject(method = "drink(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void managedFood(ItemStack stack, Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && ServingHydration.resolve(serverPlayer, stack).isPresent()) ci.cancel();
    }
}
