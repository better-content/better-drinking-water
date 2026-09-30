package com.bettercontent.betterdrinkingwater;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class SnowMeltHandler {
    private static final long MELT_DELAY_TICKS = 5L * 20L;
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> PENDING = new HashMap<>();

    private SnowMeltHandler() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(final BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        final BlockPos placedPos = event.getPos();
        final BlockState placed = level.getBlockState(placedPos);
        if (placed.getBlock() instanceof CampfireBlock) {
            scheduleAroundCampfire(level, placedPos);
        } else if (placed.is(Blocks.SNOW_BLOCK)) {
            scheduleCandidate(level, placedPos);
        }
    }

    @SubscribeEvent
    public static void onCampfireInteraction(final PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getBlockState(event.getPos()).getBlock() instanceof CampfireBlock) {
            scheduleAroundCampfire(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(final TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        final Map<BlockPos, Long> positions = PENDING.get(level.dimension());
        if (positions == null || positions.isEmpty()) return;
        final long now = level.getGameTime();
        final Iterator<Map.Entry<BlockPos, Long>> iterator = positions.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<BlockPos, Long> entry = iterator.next();
            if (entry.getValue() > now) continue;
            final BlockPos snowPos = entry.getKey();
            final BlockState snow = level.getBlockState(snowPos);
            if (!snow.is(Blocks.SNOW_BLOCK) || !hasCampfire(level, snowPos, false)) {
                iterator.remove();
                continue;
            }
            if (hasCampfire(level, snowPos, true)) {
                level.setBlockAndUpdate(snowPos, Blocks.WATER.defaultBlockState());
                iterator.remove();
            } else {
                entry.setValue(now + MELT_DELAY_TICKS);
            }
        }
        if (positions.isEmpty()) PENDING.remove(level.dimension());
    }

    static void scheduleCandidate(final ServerLevel level, final BlockPos candidate) {
        if (!level.getBlockState(candidate).is(Blocks.SNOW_BLOCK)) return;
        if (!hasCampfire(level, candidate, false)) return;
        PENDING.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .put(candidate.immutable(), level.getGameTime() + MELT_DELAY_TICKS);
    }

    static void scheduleAroundCampfire(final ServerLevel level, final BlockPos campfirePos) {
        for (BlockPos candidate : BlockPos.betweenClosed(campfirePos.offset(-1, -1, -1),
                campfirePos.offset(1, 1, 1))) {
            scheduleCandidate(level, candidate);
        }
    }

    private static boolean hasCampfire(final ServerLevel level, final BlockPos snowPos, final boolean litOnly) {
        for (BlockPos candidate : BlockPos.betweenClosed(snowPos.offset(-1, -1, -1),
                snowPos.offset(1, 1, 1))) {
            final BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof CampfireBlock && (!litOnly || state.getValue(CampfireBlock.LIT))) {
                return true;
            }
        }
        return false;
    }
}
