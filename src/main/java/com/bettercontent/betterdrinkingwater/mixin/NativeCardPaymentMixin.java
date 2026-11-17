package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativeCardPaidInputs;
import com.github.ysbbbbbb.kaleidoscopecookery.item.RecipeItem;
import com.github.ysbbbbbb.kaleidoscopecookery.item.RecipeItem.RecipeRecord;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Narrow stockpot RecipeItem payment scope; all native inventory selection/debit remains KC's. */
@Mixin(value = RecipeItem.class, remap = false)
public abstract class NativeCardPaymentMixin {
    @Shadow private InteractionResult handlePutRecipe(Player player, RecipeRecord record, Runnable nativeInsertion) {
        throw new AssertionError();
    }
    @Redirect(method = "onPutRecipe(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/item/RecipeItem;handlePutRecipe(Lnet/minecraft/world/entity/player/Player;Lcom/github/ysbbbbbb/kaleidoscopecookery/item/RecipeItem$RecipeRecord;Ljava/lang/Runnable;)Lnet/minecraft/world/InteractionResult;", ordinal = 1, remap = false), require = 1)
    private InteractionResult water$scopedStockpot(RecipeItem owner, Player actor, RecipeRecord card,
                                                   Runnable nativeInsertion, BlockEntity target,
                                                   Player player, ItemStack heldCard) {
        return NativeCardPaidInputs.scoped(() -> handlePutRecipe(actor, card,
                () -> NativeCardPaidInputs.nativeInsertion(nativeInsertion)));
    }
    @Redirect(method = "handlePutRecipe(Lnet/minecraft/world/entity/player/Player;Lcom/github/ysbbbbbb/kaleidoscopecookery/item/RecipeItem$RecipeRecord;Ljava/lang/Runnable;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/IItemHandler;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 1)
    private ItemStack water$actualExtracted(IItemHandler nativeInventory, int slot, int count, boolean simulate) {
        ItemStack paid = nativeInventory.extractItem(slot, count, simulate);
        if (!simulate) NativeCardPaidInputs.extracted(paid);
        return paid;
    }
}
