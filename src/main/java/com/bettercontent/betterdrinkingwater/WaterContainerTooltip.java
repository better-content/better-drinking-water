package com.bettercontent.betterdrinkingwater;

import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Native fluid vessels do not participate in Thirst's item-only tooltip registry. */
public final class WaterContainerTooltip {
    private WaterContainerTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (WaterSources.accepts(stack)) {
            event.getToolTip().add(Component.translatable("tooltip.better_drinking_water.slot_compatible")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (BlockWaterDrinking.hasDirectAccess(WaterSources.id(stack))) {
            event.getToolTip().add(Component.translatable("tooltip.better_drinking_water.direct")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!WaterSources.accepts(stack) || WaterPurity.isWaterFilledContainer(stack)) return;
        stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
            for (int index = 0; index < handler.getTanks(); index++) {
                var fluid = handler.getFluidInTank(index);
                if (!WaterSources.isWater(fluid)) continue;
                event.getToolTip().add(Component.translatable(WaterPurity.getPurity(fluid.copy()) == 3
                        ? "tooltip.better_drinking_water.purified" : "tooltip.better_drinking_water.dirty")
                        .withStyle(ChatFormatting.GRAY));
                break;
            }
        });
    }
}
