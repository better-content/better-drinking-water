package com.bettercontent.betterdrinkingwater.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/** Server-only integration; never impersonates a native whole-item use event. */
public final class BeverageEvents {
    private BeverageEvents() {}

    /** Resolve whole-serving nutrition once, before payment, into bounded persistent data. */
    public static final class Profile extends Event {
        private final ServerPlayer player;
        private final ItemStack stack;
        private final CompoundTag nutrition;
        public Profile(ServerPlayer player, ItemStack stack, CompoundTag nutrition) {
            this.player = player; this.stack = stack.copyWithCount(1); this.nutrition = nutrition;
        }
        public ServerPlayer player() { return player; }
        public ItemStack stack() { return stack.copy(); }
        public CompoundTag nutrition() { return nutrition; }
    }

    /** Runs before purchase AND before releasing an already-paid beverage fraction. */
    @Cancelable
    public static final class Check extends Event {
        private final ServerPlayer player;
        public Check(ServerPlayer player) { this.player = player; }
        public ServerPlayer player() { return player; }
    }

    /** Paid consumed fraction; nutrition is a snapshot, not a fresh lookup of the equipped item. */
    public static final class Sip extends Event {
        private final ServerPlayer player;
        private final CompoundTag nutrition;
        private final double from, to;
        public Sip(ServerPlayer player, CompoundTag nutrition, double from, double to) {
            this.player = player; this.nutrition = nutrition.copy(); this.from = from; this.to = to;
        }
        public ServerPlayer player() { return player; }
        public CompoundTag nutrition() { return nutrition.copy(); }
        public double from() { return from; }
        public double to() { return to; }
        public double fraction() { return to - from; }
    }
}
