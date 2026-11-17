package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativePlacedCups;
import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.TeacupBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TeacupBlock.class)
public abstract class NativeTeacupPickupMixin {
    @Inject(method = "use(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), require = 1)
    private void water$scope(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                             BlockHitResult hit, CallbackInfoReturnable<InteractionResult> ci) {
        NativePlacedCups.requireUse(state, level, pos, player, hand);
    }
    @Redirect(method = "use(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/util/ItemUtils;getItemToLivingEntity(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V", remap = false), require = 2)
    private void water$pickup(LivingEntity target, ItemStack nativeCandidate, BlockState state, Level level,
                              BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemUtils.getItemToLivingEntity(target, NativePlacedCups.pickup(level, pos, state, player, nativeCandidate));
    }
    @Redirect(method = "use(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;popResourceFromFace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;)V"), require = 1)
    private void water$face(Level level, BlockPos pos, Direction face, ItemStack nativeCandidate,
                            BlockState state, Level actual, BlockPos at, Player player,
                            InteractionHand hand, BlockHitResult hit) {
        Block.popResourceFromFace(level, pos, face, NativePlacedCups.pickup(level, pos, state, player, nativeCandidate));
    }
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;", at = @At("RETURN"), cancellable = true, require = 1)
    private void water$drops(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> ci) {
        ci.setReturnValue(NativePlacedCups.drops(state, params, ci.getReturnValue()));
    }
}
