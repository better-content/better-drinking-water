package com.bettercontent.betterdrinkingwater.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

import java.util.Objects;

/** Observes authoritative player progress through an unsafe-water episode. */
public final class WaterSafetyEvent extends Event {
    public enum Stage { THIRST_LOST, PURIFIED_WATER_CONSUMED }

    private final ServerPlayer player;
    private final Stage stage;
    private final String episodeId;

    public WaterSafetyEvent(ServerPlayer player, Stage stage, String episodeId) {
        this.player = Objects.requireNonNull(player, "player");
        this.stage = Objects.requireNonNull(stage, "stage");
        this.episodeId = Objects.requireNonNull(episodeId, "episodeId");
    }

    public ServerPlayer getPlayer() { return player; }
    public Stage getStage() { return stage; }
    public String getEpisodeId() { return episodeId; }
}
