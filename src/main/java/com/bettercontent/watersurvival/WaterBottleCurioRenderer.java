package com.bettercontent.watersurvival;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

@Mod.EventBusSubscriber(modid = WaterSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WaterBottleCurioRenderer implements ICurioRenderer {
    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CuriosRendererRegistry.register(Items.POTION, WaterBottleCurioRenderer::new));
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot,
            PoseStack pose, RenderLayerParent<T, M> parent, MultiBufferSource buffers,
            int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {
        if (!WaterBottleCurio.SLOT.equals(slot.identifier()) || !slot.visible() ||
                !WaterBottleCurio.isWaterBottle(stack) || !(parent.getModel() instanceof HumanoidModel<?> humanoid)) return;

        pose.pushPose();
        humanoid.body.translateAndRotate(pose);
        // The bottle hangs on the wearer's left hip and follows body movement.
        pose.translate(0.36D, 0.72D, 0.12D);
        pose.mulPose(Axis.ZP.rotationDegrees(15.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(0.58F, 0.58F, 0.58F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, slot.entity().level(), slot.entity().getId());
        pose.popPose();
    }
}
