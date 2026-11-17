package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativePlacedCups;
import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.ClayPotMilkTeaBlock;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Milk inherits the declaring getDrops from BlockBehaviour. */
@Mixin(BlockBehaviour.class)
public abstract class NativeMilkCupDropsMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;", at = @At("RETURN"), cancellable = true, require = 1)
    private void water$drops(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> ci) {
        if (state.getBlock() instanceof ClayPotMilkTeaBlock)
            ci.setReturnValue(NativePlacedCups.drops(state, params, ci.getReturnValue()));
    }
}
