package com.bettercontent.betterdrinkingwater;

import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Manual and automatic servings use exactly the same entitlement/actions, never native fruit effects. */
final class PlantDrinkItem extends Item {
    PlantDrinkItem(Properties properties) { super(properties); }
    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<net.minecraft.network.chat.Component> lines, TooltipFlag flag) {
        String role = PlantDrinks.role(WaterSources.id(stack));
        var profile = DrinkCatalogue.find(stack);
        if (role.isEmpty()) return;
        var line = net.minecraft.network.chat.Component.translatable("tooltip.better_drinking_water.plant." + role);
        if (profile != null && role.equals("buff")) {
            boolean first = true;
            for (var dose : profile.effects()) {
                var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(new net.minecraft.resources.ResourceLocation(dose.id()));
                if (effect == null) continue;
                line.append(first ? " — " : ", ").append(net.minecraft.network.chat.Component.translatable(effect.getDescriptionId()));
                if (dose.chance() < 1) line.append(" (" + Math.round(dose.chance() * 100) + "%)");
                first = false;
            }
            if (!profile.action().equals("none")) line.append(first ? " — " : "; ")
                    .append(net.minecraft.network.chat.Component.translatable("tooltip.better_drinking_water.action." + profile.action()));
        }
        lines.add(line.withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public int getUseDuration(ItemStack stack) { return 32; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server && (!BeverageServing.allowed(server)
                || server.getCapability(ModCapabilities.PLAYER_THIRST).map(t -> t.getThirst() >= 20).orElse(true)))
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !BeverageServing.allowed(player)) return stack;
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElse(null);
        var drink = DrinkCatalogue.find(stack);
        if (thirst == null || thirst.getThirst() >= 20 || drink == null) return stack;
        var serving = BeverageServing.purchase(player, stack, drink);
        if (serving == null) return stack;
        // Commit item payment before releasing any effect or event. No call to super: that would
        // couple native FoodData/Diet awards to these already explicit proportional entitlements.
        if (!player.getAbilities().instabuild) stack.shrink(1);
        serving.release(player, 0, 1);
        thirst.drink(player, drink.hydration(), drink.quenching());
        thirst.updateThirstData(player);
        if (player.getAbilities().instabuild) return stack;
        if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        return stack;
    }
}
