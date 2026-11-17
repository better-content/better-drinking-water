package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativePlacedCups;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class NativeCupPlacementMixin {
    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), require = 1)
    private void water$boundActual(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> ci) {
        NativePlacedCups.validatePlacement(context.getLevel(), context.getItemInHand());
    }
    @Redirect(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;setPlacedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"), require = 1)
    private void water$original(Block block, Level level, BlockPos pos, BlockState state, LivingEntity actor, ItemStack held) {
        ItemStack captured = NativePlacedCups.known(state) ? NativePlacedCups.snapshotPlacement(held) : ItemStack.EMPTY;
        block.setPlacedBy(level, pos, state, actor, held);
        if (!captured.isEmpty()) NativePlacedCups.placed(level, pos, state, captured);
    }
}
