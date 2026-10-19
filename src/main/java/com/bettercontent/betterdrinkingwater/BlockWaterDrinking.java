package com.bettercontent.betterdrinkingwater;

import dev.ghen.thirst.content.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/** Direct access to ordinary reservoirs, not arbitrary machine process buffers. */
public final class BlockWaterDrinking {
    private static final String COOLDOWN = "better_drinking_water:last_block_drink";
    private static final Set<String> TANKS = Set.of(
            "create:fluid_tank", "createdieselgenerators:canister", "createdieselgenerators:oil_barrel",
            "pneumaticcraft:small_tank", "pneumaticcraft:medium_tank", "pneumaticcraft:large_tank",
            "pneumaticcraft:huge_tank", "ae2:sky_stone_tank",
            "tconstruct:seared_fuel_tank", "tconstruct:seared_fuel_gauge", "tconstruct:seared_ingot_tank",
            "tconstruct:seared_ingot_gauge", "tconstruct:seared_lantern", "tconstruct:seared_casting_tank",
            "tconstruct:scorched_fuel_tank", "tconstruct:scorched_fuel_gauge", "tconstruct:scorched_ingot_tank",
            "tconstruct:scorched_ingot_gauge", "tconstruct:scorched_lantern");

    private BlockWaterDrinking() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY
                || event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()
                || !event.getEntity().getMainHandItem().isEmpty()
                || !event.getEntity().getOffhandItem().isEmpty()) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!isCauldron(state) && !TANKS.contains(id(state))) return;
        // Claim the gesture on both sides; an empty/blocked reservoir must not open its GUI as a second action.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        drink(player, event.getPos(), event.getFace());
    }

    static boolean drink(ServerPlayer player, BlockPos pos, Direction face) {
        var level = player.serverLevel();
        if (player.isSpectator() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, face, player.getMainHandItem())) return false;
        long now = level.getGameTime();
        var data = player.getPersistentData();
        if (data.contains(COOLDOWN) && now >= data.getLong(COOLDOWN) && now - data.getLong(COOLDOWN) < 10) return false;
        BlockState state = level.getBlockState(pos);
        if (!isCauldron(state) && !TANKS.contains(id(state))) return false;
        data.putLong(COOLDOWN, now);
        boolean drank = WaterDrinking.sip(player, () -> {
            if (isCauldron(state)) {
                int purity = state.hasProperty(WaterPurity.BLOCK_PURITY)
                        ? WaterQuality.normalize(WaterPurity.getBlockPurity(state)) : 0;
                int amount = state.getValue(LayeredCauldronBlock.LEVEL);
                BlockState remaining;
                if (amount > 1) remaining = state.setValue(LayeredCauldronBlock.LEVEL, amount - 1);
                else {
                    ResourceLocation emptyId = state.is(Blocks.WATER_CAULDRON) ? new ResourceLocation("minecraft:cauldron")
                            : new ResourceLocation(id(state).replace("water_cauldron", "cauldron"));
                    if (!ForgeRegistries.BLOCKS.containsKey(emptyId)) return null;
                    remaining = ForgeRegistries.BLOCKS.getValue(emptyId).defaultBlockState();
                }
                level.setBlock(pos, remaining, Block.UPDATE_ALL);
                return new PlayerWaterReserve.Reserve(4, 5, 0, purity);
            }
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) return null;
            // Respect the clicked side. Do not fall back to an unrestricted internal capability.
            var handler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, face).resolve().orElse(null);
            return handler == null ? null : drain(handler);
        }, 1);
        if (drank) level.playSound(null, pos, SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8F, 1.0F);
        return drank;
    }

    static PlayerWaterReserve.Reserve drain(IFluidHandler handler) {
        FluidStack preview = handler.drain(250, IFluidHandler.FluidAction.SIMULATE);
        if (WaterSources.portion(preview) == null) return null;
        FluidStack drained = handler.drain(preview, IFluidHandler.FluidAction.EXECUTE);
        // Grant only what was actually extracted; purity must agree with the inspected portion.
        if (!WaterSources.isWater(drained) || drained.getAmount() > preview.getAmount()
                || WaterPurity.getPurity(drained.copy()) != WaterPurity.getPurity(preview.copy())) return null;
        return WaterSources.portion(drained);
    }

    static boolean hasDirectAccess(String id) {
        return TANKS.contains(id) || id.equals("minecraft:cauldron")
                || (id.startsWith("everythingcopper:") && id.endsWith("copper_cauldron"));
    }

    private static boolean isCauldron(BlockState state) {
        return state.hasProperty(LayeredCauldronBlock.LEVEL)
                && (state.is(Blocks.WATER_CAULDRON) || (id(state).startsWith("everythingcopper:")
                && id(state).endsWith("copper_water_cauldron")));
    }

    private static String id(BlockState state) {
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return key == null ? "" : key.toString();
    }
}
