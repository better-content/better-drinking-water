package com.bettercontent.betterdrinkingwater.mixin;

import com.bettercontent.betterdrinkingwater.NativeDrinkFacts;
import com.bettercontent.betterdrinkingwater.NativeDrinkProduction;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;
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

/** Original KC inputs at payment; result is attached before native result-field install. */
@Mixin(value = TeapotBlockEntity.class, remap = false)
public abstract class NativeTeapotFactsMixin {
    @Shadow private ResourceLocation teaFluidId;
    @Shadow private ItemStack input;
    @Shadow private ItemStack result;
    @Unique private CompoundTag water$base = new CompoundTag();
    @Unique private CompoundTag water$pendingBase = new CompoundTag();
    @Unique private CompoundTag water$inputs = new CompoundTag();
    @Unique private ItemStack water$pendingIngredient = ItemStack.EMPTY;

    @Inject(method = "addTeaFluid(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), require = 1)
    private void water$fillHead(Level level, LivingEntity actor, ItemStack source, CallbackInfoReturnable<Boolean> ci) {
        water$pendingBase = new CompoundTag();
        if (level.isClientSide || source.isEmpty() || actor instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) return;
        source.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
            if (handler.getTanks() == 0 || handler.getFluidInTank(0).getAmount() < 1000) return;
            var fluid = handler.getFluidInTank(0).getFluid();
            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
            if (id != null) try { water$pendingBase = NativeDrinkProduction.base(id.toString(), 1000, source, "placed_item"); }
            catch (RuntimeException unsupported) { NativeDrinkProduction.unsupported("teapot base", unsupported); }
        });
    }
    @Inject(method = "addTeaFluid(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), require = 1)
    private void water$fillReturn(Level level, LivingEntity actor, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (!level.isClientSide && ci.getReturnValueZ()) {
            water$base = water$pendingBase.copy();
            if (!water$base.isEmpty() && !water$base.getString("fluid").equals(teaFluidId.toString())) water$base = new CompoundTag();
            ((TeapotBlockEntity)(Object)this).setChanged();
        }
        water$pendingBase = new CompoundTag();
    }
    @Inject(method = "receiveDripstoneFluid(Lnet/minecraft/world/level/material/Fluid;)Z", at = @At("RETURN"), require = 1)
    private void water$drip(Fluid actual, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValueZ()) {
            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(actual);
            water$base = NativeDrinkProduction.base(id == null ? "" : id.toString(), 0, ItemStack.EMPTY, "dripstone_unknown");
            ((TeapotBlockEntity)(Object)this).setChanged();
        }
    }
    @Inject(method = "removeTeaFluid(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), require = 1)
    private void water$removeBase(Level level, LivingEntity actor, ItemStack vessel, CallbackInfoReturnable<Boolean> ci) {
        if (!level.isClientSide && ci.getReturnValueZ()) { water$base = new CompoundTag(); ((TeapotBlockEntity)(Object)this).setChanged(); }
    }
    @Inject(method = "addIngredient(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), require = 1)
    private void water$ingredientHead(Level level, LivingEntity actor, ItemStack actual, CallbackInfoReturnable<Boolean> ci) {
        water$pendingIngredient = ItemStack.EMPTY;
        if (!level.isClientSide && !actual.isEmpty()) try {
            NativeDrinkFacts.validateStack(actual);
            water$pendingIngredient = actual.copyWithCount(1);
        } catch (RuntimeException unsupported) { NativeDrinkProduction.unsupported("teapot paid input", unsupported); }
    }
    @Inject(method = "addIngredient(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), require = 1)
    private void water$ingredientReturn(Level level, LivingEntity actor, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (!level.isClientSide && ci.getReturnValueZ()) water$bindIngredient();
        water$pendingIngredient = ItemStack.EMPTY;
    }
    @Inject(method = "insertIngredient(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), require = 1)
    private void water$automationHead(ItemStack actual, CallbackInfo ci) {
        water$pendingIngredient = ItemStack.EMPTY;
        if (!actual.isEmpty()) try {
            NativeDrinkFacts.validateStack(actual);
            water$pendingIngredient = actual.copyWithCount(1);
        } catch (RuntimeException unsupported) { NativeDrinkProduction.unsupported("teapot automation input", unsupported); }
    }
    @Inject(method = "insertIngredient(Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"), require = 1)
    private void water$automationReturn(ItemStack actual, CallbackInfo ci) {
        if (!water$pendingIngredient.isEmpty()) water$bindIngredient();
        water$pendingIngredient = ItemStack.EMPTY;
    }
    @Unique private void water$bindIngredient() {
        if (!water$pendingIngredient.isEmpty() && !input.isEmpty() && input.getCount() == 1
                && input.is(water$pendingIngredient.getItem())) {
            water$inputs.put("0", NativeDrinkFacts.bounded(water$pendingIngredient.save(new CompoundTag())));
            ((TeapotBlockEntity)(Object)this).setChanged();
        }
    }
    @Inject(method = "removeIngredient(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), require = 1)
    private void water$removeIngredient(Level level, LivingEntity actor, CallbackInfoReturnable<Boolean> ci) {
        if (!level.isClientSide && ci.getReturnValueZ()) { water$inputs = new CompoundTag(); ((TeapotBlockEntity)(Object)this).setChanged(); }
    }
    @Inject(method = {"saveAdditional(Lnet/minecraft/nbt/CompoundTag;)V", "m_183515_(Lnet/minecraft/nbt/CompoundTag;)V"}, at = @At("TAIL"), require = 1)
    private void water$save(CompoundTag tag, CallbackInfo ci) {
        if (!water$base.isEmpty()) tag.put(NativeDrinkFacts.BASE_KEY, NativeDrinkFacts.bounded(water$base));
        if (!water$inputs.isEmpty()) tag.put(NativeDrinkFacts.INPUTS_KEY, NativeDrinkFacts.bounded(water$inputs));
    }
    @Inject(method = {"load(Lnet/minecraft/nbt/CompoundTag;)V", "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V"}, at = @At("TAIL"), require = 1)
    private void water$load(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(NativeDrinkFacts.BASE_KEY) && !tag.contains(NativeDrinkFacts.BASE_KEY, Tag.TAG_COMPOUND))
            throw new IllegalArgumentException("Corrupt known native teapot base type");
        water$base = NativeDrinkFacts.bounded(tag.getCompound(NativeDrinkFacts.BASE_KEY));
        if (tag.contains(NativeDrinkFacts.INPUTS_KEY) && !tag.contains(NativeDrinkFacts.INPUTS_KEY, Tag.TAG_COMPOUND))
            throw new IllegalArgumentException("Corrupt known native teapot input type");
        water$inputs = NativeDrinkFacts.bounded(tag.getCompound(NativeDrinkFacts.INPUTS_KEY));
        water$pendingIngredient = ItemStack.EMPTY; water$pendingBase = new CompoundTag();
    }
    @Inject(method = "getDrops()Ljava/util/List;", at = @At("RETURN"), require = 1)
    private void water$portableFact(CallbackInfoReturnable<List<ItemStack>> ci) {
        if (water$base.isEmpty()) return;
        for (ItemStack drop : ci.getReturnValue()) {
            if (!drop.is(com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems.TEAPOT.get())) continue;
            CompoundTag nativeBlock = BlockItem.getBlockEntityData(drop);
            if (nativeBlock == null) continue;
            try {
                CompoundTag candidate = NativeDrinkFacts.bounded(nativeBlock);
                candidate.put(NativeDrinkFacts.BASE_KEY, NativeDrinkFacts.bounded(water$base));
                // Existing KC portable native BE data remains authoritative.
                BlockItem.setBlockEntityData(drop, ((TeapotBlockEntity)(Object)this).getType(), candidate);
            } catch (RuntimeException unsupported) {
                NativeDrinkProduction.unsupported("portable teapot drop fact", unsupported);
            }
        }
    }
    @Redirect(method = "tick(Lnet/minecraft/world/level/Level;)V", at = @At(value = "FIELD",
            target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/TeapotBlockEntity;result:Lnet/minecraft/world/item/ItemStack;",
            opcode = Opcodes.PUTFIELD), require = 2)
    private void water$nativeCandidate(TeapotBlockEntity owner, ItemStack nativeResult) {
        result = owner.getLevel() != null && !owner.getLevel().isClientSide()
                ? NativeDrinkProduction.safeDecorate("teapot", (net.minecraft.server.level.ServerLevel)owner.getLevel(), nativeResult, water$base, List.of(input), water$inputs)
                : nativeResult;
    }
}
