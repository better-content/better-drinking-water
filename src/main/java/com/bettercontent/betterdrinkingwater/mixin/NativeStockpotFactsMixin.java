package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativeDrinkFacts;
import com.bettercontent.betterdrinkingwater.NativeDrinkProduction;
import com.bettercontent.betterdrinkingwater.NativeCardPaidInputs;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/** The assembled result field is bound BEFORE KC writes it or clears paid inputs. */
@Mixin(value = StockpotBlockEntity.class, remap = false)
public abstract class NativeStockpotFactsMixin {
    @Shadow private ResourceLocation soupBaseId;
    @Shadow private ItemStack result;
    @Shadow public abstract NonNullList<ItemStack> getInputs();
    @Unique private CompoundTag water$base = new CompoundTag();
    @Unique private CompoundTag water$pendingBase = new CompoundTag();
    @Unique private CompoundTag water$inputs = new CompoundTag();
    @Unique private ItemStack water$pendingIngredient = ItemStack.EMPTY;
    @Unique private int water$pendingSlot = -1;

    @Inject(method = "addSoupBase(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), require = 1)
    private void water$baseHead(Level level, LivingEntity actor, ItemStack source, CallbackInfoReturnable<Boolean> ci) {
        water$pendingBase = new CompoundTag();
        if (!level.isClientSide && !source.isEmpty() && !(actor instanceof net.minecraft.world.entity.player.Player player && player.isCreative())) {
            try {
                if (source.is(Items.MILK_BUCKET)) water$pendingBase = NativeDrinkProduction.base("minecraft:milk", 1000, source, "stockpot_base");
                else if (source.is(Items.WATER_BUCKET)) water$pendingBase = NativeDrinkProduction.base("minecraft:water", 1000, source, "stockpot_base");
            } catch (RuntimeException unsupported) { NativeDrinkProduction.unsupported("stockpot base", unsupported); }
        }
    }
    @Inject(method = "addSoupBase(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), require = 1)
    private void water$baseReturn(Level level, LivingEntity actor, ItemStack source, CallbackInfoReturnable<Boolean> ci) {
        if (!level.isClientSide && ci.getReturnValueZ()) {
            water$base = water$pendingBase.copy();
            if (water$base.contains("fluid") && soupBaseId == null) water$base = new CompoundTag();
            ((StockpotBlockEntity)(Object)this).setChanged();
        }
        water$pendingBase = new CompoundTag();
    }
    @Inject(method = "removeSoupBase(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), require = 1)
    private void water$removeBase(Level level, LivingEntity actor, ItemStack vessel, CallbackInfoReturnable<Boolean> ci) {
        if (!level.isClientSide && ci.getReturnValueZ()) { water$base = new CompoundTag(); water$inputs = new CompoundTag(); ((StockpotBlockEntity)(Object)this).setChanged(); }
    }
    @Inject(method = "takeOutProduct(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), require = 1)
    private void water$lastCup(Level level, LivingEntity actor, ItemStack carrier, CallbackInfoReturnable<Boolean> ci) {
        if (!level.isClientSide && ci.getReturnValueZ() && getInputs().stream().allMatch(ItemStack::isEmpty)) {
            water$base = new CompoundTag(); water$inputs = new CompoundTag();
        }
    }
    @Inject(method = "setStatus(I)V", at = @At("RETURN"), require = 1)
    private void water$reset(int next, CallbackInfo ci) {
        if (next == 0) { water$base = new CompoundTag(); water$inputs = new CompoundTag(); water$pendingIngredient = ItemStack.EMPTY; water$pendingSlot = -1; }
    }
    @Inject(method = "addIngredient(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), require = 1)
    private void water$inputHead(Level level, LivingEntity actor, ItemStack source, CallbackInfoReturnable<Boolean> ci) {
        water$pendingIngredient = ItemStack.EMPTY; water$pendingSlot = -1;
        if (level.isClientSide || source.isEmpty()) return;
        List<ItemStack> slots = getInputs();
        for (int i = 0; i < slots.size(); i++) if (slots.get(i).isEmpty()) {
            water$pendingSlot = i;
            try {
                NativeDrinkFacts.validateStack(source);
                water$pendingIngredient = source.copyWithCount(1);
            } catch (RuntimeException unsupported) { NativeDrinkProduction.unsupported("stockpot input", unsupported); }
            break;
        }
    }
    @Inject(method = "addIngredient(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), require = 1)
    private void water$inputReturn(Level level, LivingEntity actor, ItemStack source, CallbackInfoReturnable<Boolean> ci) {
        if (!level.isClientSide && ci.getReturnValueZ() && water$pendingSlot >= 0
                && water$pendingSlot < getInputs().size() && !water$pendingIngredient.isEmpty()) {
            ItemStack actual = getInputs().get(water$pendingSlot);
            if (!actual.isEmpty() && actual.getCount() == 1 && actual.is(water$pendingIngredient.getItem())) {
                water$inputs.put(Integer.toString(water$pendingSlot),
                        NativeDrinkFacts.bounded(water$pendingIngredient.save(new CompoundTag())));
                ((StockpotBlockEntity)(Object)this).setChanged();
            }
        }
        water$pendingIngredient = ItemStack.EMPTY; water$pendingSlot = -1;
    }
    @Inject(method = "addAllIngredients(Ljava/util/List;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("RETURN"), require = 1)
    private void water$cardActualPaidInputs(List<ItemStack> cardTemplates, LivingEntity actor, CallbackInfo ci) {
        var actualPaid = NativeCardPaidInputs.paidFor(getInputs());
        if (actualPaid.isEmpty()) {
            if (getInputs().stream().anyMatch(stack -> !stack.isEmpty()))
                NativeDrinkProduction.unsupported("card input payment", new IllegalStateException("No matching actual inventory extraction"));
            return;
        }
        CompoundTag captured = new CompoundTag();
        actualPaid.forEach((slot, paid) -> captured.put(Integer.toString(slot),
                NativeDrinkFacts.bounded(paid.save(new CompoundTag()))));
        water$inputs = NativeDrinkFacts.bounded(captured);
        ((StockpotBlockEntity)(Object)this).setChanged();
    }
    @Inject(method = "removeIngredient(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), require = 1)
    private void water$inputRemoved(Level level, LivingEntity actor, CallbackInfoReturnable<Boolean> ci) {
        if (!level.isClientSide && ci.getReturnValueZ()) {
            List<ItemStack> slots = getInputs();
            for (String key : List.copyOf(water$inputs.getAllKeys())) {
                int slot = Integer.parseInt(key);
                if (slot >= slots.size() || slots.get(slot).isEmpty()) water$inputs.remove(key);
            }
            ((StockpotBlockEntity)(Object)this).setChanged();
        }
    }
    @Inject(method = {"saveAdditional(Lnet/minecraft/nbt/CompoundTag;)V", "m_183515_(Lnet/minecraft/nbt/CompoundTag;)V"}, at = @At("TAIL"), require = 1)
    private void water$save(CompoundTag tag, CallbackInfo ci) {
        if (!water$base.isEmpty()) tag.put(NativeDrinkFacts.BASE_KEY, NativeDrinkFacts.bounded(water$base));
        if (!water$inputs.isEmpty()) tag.put(NativeDrinkFacts.INPUTS_KEY, NativeDrinkFacts.bounded(water$inputs));
    }
    @Inject(method = {"load(Lnet/minecraft/nbt/CompoundTag;)V", "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V"}, at = @At("TAIL"), require = 1)
    private void water$load(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(NativeDrinkFacts.BASE_KEY) && !tag.contains(NativeDrinkFacts.BASE_KEY, Tag.TAG_COMPOUND))
            throw new IllegalArgumentException("Corrupt known stockpot base type");
        if (tag.contains(NativeDrinkFacts.INPUTS_KEY) && !tag.contains(NativeDrinkFacts.INPUTS_KEY, Tag.TAG_COMPOUND))
            throw new IllegalArgumentException("Corrupt known stockpot input type");
        water$base = NativeDrinkFacts.bounded(tag.getCompound(NativeDrinkFacts.BASE_KEY));
        water$inputs = NativeDrinkFacts.bounded(tag.getCompound(NativeDrinkFacts.INPUTS_KEY));
        water$pendingBase = new CompoundTag(); water$pendingIngredient = ItemStack.EMPTY; water$pendingSlot = -1;
    }
    @Redirect(method = "applyRecipe(Lnet/minecraft/world/level/Level;Lcom/github/ysbbbbbb/kaleidoscopecookery/crafting/container/StockpotContainer;Lcom/github/ysbbbbbb/kaleidoscopecookery/crafting/recipe/StockpotRecipe;)V",
            at = @At(value = "FIELD", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/StockpotBlockEntity;result:Lnet/minecraft/world/item/ItemStack;", opcode = Opcodes.PUTFIELD), require = 1)
    private void water$nativeCandidate(StockpotBlockEntity owner, ItemStack nativeResult) {
        result = owner.getLevel() != null && !owner.getLevel().isClientSide()
                ? NativeDrinkProduction.safeDecorate("stockpot", (net.minecraft.server.level.ServerLevel)owner.getLevel(), nativeResult, water$base, getInputs(), water$inputs)
                : nativeResult;
    }
}
