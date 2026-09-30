package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.api.event.WaterSafetyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

/** Owns correlation for the player's current unsafe-water episode. */
public final class WaterSafetyEpisodes {
    // Keep the legacy NBT name so water-safety episodes already in progress survive the migration.
    private static final String EPISODE_ROOT = "WaterSurvivalThreadEpisode";

    private WaterSafetyEpisodes() {}

    public static void thirstLost(ServerPlayer player) {
        String token = remembered(player);
        if (!shouldStartEpisode(token)) return;
        token = player.getUUID() + ":thirst:" + player.server.getTickCount();
        remember(player, token);
        MinecraftForge.EVENT_BUS.post(new WaterSafetyEvent(player, WaterSafetyEvent.Stage.THIRST_LOST, token));
    }

    public static void purifiedDrunk(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        String token = persisted.getString(EPISODE_ROOT);
        if (!validToken(token)) return;
        MinecraftForge.EVENT_BUS.post(new WaterSafetyEvent(
                player, WaterSafetyEvent.Stage.PURIFIED_WATER_CONSUMED, token));
        persisted.remove(EPISODE_ROOT);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static void remember(ServerPlayer player, String token) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putString(EPISODE_ROOT, token);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    static String remembered(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getString(EPISODE_ROOT);
    }

    static boolean validToken(String token) {
        if (token.isBlank() || token.length() > 128) return false;
        return token.chars().allMatch(character -> character >= 0x21 && character <= 0x7e);
    }

    static boolean shouldStartEpisode(String token) { return !validToken(token); }
}
