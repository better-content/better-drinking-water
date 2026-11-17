package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativeCupIntake;
import dev.ghen.thirst.content.thirst.PlayerThirst;
import dev.ghen.thirst.content.thirst.PlayerThirstManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Thirst's actual whole-item Finish authority yields only to Water's committed cup debit;
 * tooltips and every untagged/unsupported native cup retain the original Thirst path. */
@Mixin(value = PlayerThirstManager.class, remap = false)
public abstract class NativeCupThirstFinishMixin {
    @Redirect(method = "drink(Lnet/minecraftforge/event/entity/living/LivingEntityUseItemEvent$Finish;)V",
            at = @At(value = "INVOKE", target = "Ldev/ghen/thirst/content/thirst/PlayerThirst;drink(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V", remap = false), require = 1)
    private static void water$onlyAfterNativePaid(ItemStack source, Player player, LivingEntityUseItemEvent.Finish event) {
        if (!NativeCupIntake.takePaidHydration(player, event.getItem())) PlayerThirst.drink(source, player);
    }
}
