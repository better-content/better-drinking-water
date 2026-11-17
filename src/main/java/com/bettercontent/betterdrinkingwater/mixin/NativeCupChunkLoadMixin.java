package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativeCupBlockEntity;
import com.bettercontent.betterdrinkingwater.NativePlacedCups;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Do not let a wrong saved BE type become an apparently unknown native cup. */
@Mixin(LevelChunk.class)
public abstract class NativeCupChunkLoadMixin {
    @Inject(method = "promotePendingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At("HEAD"), cancellable = true, require = 1)
    private void water$knownType(BlockPos pos, CompoundTag saved, CallbackInfoReturnable<BlockEntity> ci) {
        LevelChunk chunk = (LevelChunk)(Object)this;
        var state = chunk.getBlockState(pos);
        if (!NativePlacedCups.known(state)) return;
        String id = saved.getString("id");
        if (id.equals("better_drinking_water:native_drink_cups")
                || id.equals("DUMMY") && !saved.contains(NativeCupBlockEntity.KEY)) return;
        var body = NativeCupBlockEntity.rejectedSavedType(pos, state, saved);
        chunk.addAndRegisterBlockEntity(body);
        ci.setReturnValue(body);
    }
}
