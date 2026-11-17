package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativeCupBlockEntity;
import com.bettercontent.betterdrinkingwater.NativePlacedCups;
import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.ClayPotMilkTeaBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.TeacupBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/** Reuses KC block ID, model, native state and native action logic. */
@Mixin({TeacupBlock.class, ClayPotMilkTeaBlock.class})
public abstract class NativeCupEntityMixin implements EntityBlock {
    @Override @Nullable public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return NativePlacedCups.known(state) ? new NativeCupBlockEntity(pos, state) : null;
    }
}
