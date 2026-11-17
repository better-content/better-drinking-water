package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativeDrinkFacts;
import com.bettercontent.betterdrinkingwater.NativeDrinkProduction;
import com.github.ysbbbbbb.kaleidoscopecookery.item.TeapotItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Portable use pays a WORLD source, not the placed BE addTeaFluid path. */
@Mixin(TeapotItem.class)
public abstract class NativePortableTeapotFactsMixin {
    @Unique private static final ThreadLocal<ItemStack> water$bucket = new ThreadLocal<>();

    @Inject(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;", at = @At("HEAD"), require = 1)
    private void water$start(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) {
        water$bucket.remove();
    }
    @Redirect(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"), require = 1)
    private ItemStack water$actualSource(BucketPickup block, LevelAccessor level, BlockPos pos, BlockState state) {
        ItemStack extracted = block.pickupBlock(level, pos, state);
        if (!extracted.isEmpty()) try {
            NativeDrinkFacts.validateStack(extracted);
            water$bucket.set(extracted.copyWithCount(1));
        } catch (RuntimeException unsupported) {
            NativeDrinkProduction.unsupported("portable pickup source", unsupported);
        }
        return extracted;
    }
    @Inject(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;", at = @At("RETURN"), require = 1)
    private void water$done(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) {
        try {
            ItemStack actual = water$bucket.get();
            if (level.isClientSide || player.isCreative() || actual == null || actual.isEmpty()
                    || ci.getReturnValue() == null || !ci.getReturnValue().getResult().consumesAction()) return;
            ItemStack teapot = ci.getReturnValue().getObject();
            var blockData = BlockItem.getBlockEntityData(teapot);
            if (blockData == null || blockData.getString("TeaFluidId").isEmpty()) return;
            actual.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
                if (handler.getTanks() < 1 || handler.getFluidInTank(0).getAmount() < 1000) return;
                var fluid = ForgeRegistries.FLUIDS.getKey(handler.getFluidInTank(0).getFluid());
                if (fluid == null || !blockData.getString("TeaFluidId").equals(fluid.toString())) return;
                try {
                    CompoundTag bound = NativeDrinkProduction.base(fluid.toString(), 1000, actual, "portable_pickup");
                    teapot.getOrCreateTagElement("BlockEntityTag").put(NativeDrinkFacts.BASE_KEY, bound);
                } catch (RuntimeException unsupported) {
                    NativeDrinkProduction.unsupported("portable world-source pickup", unsupported);
                }
            });
        } finally { water$bucket.remove(); }
    }
}
