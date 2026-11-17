package com.bettercontent.betterdrinkingwater.api;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/** Synchronous result-commit observation. Age owner sees actual paid originals ephemerally;
 * this event does not store or serialize an additional age child inside Water's receipt. */
public final class NativeDrinkProducedEvent extends Event {
    private final ServerLevel level;
    private final ItemStack result;
    private final List<ItemStack> paid;
    public NativeDrinkProducedEvent(ServerLevel level, ItemStack result, List<ItemStack> paid) {
        this.level = level; this.result = result; this.paid = List.copyOf(paid);
    }
    public ServerLevel level() { return level; }
    /** Mutable actual KC candidate before native result-field installation. */
    public ItemStack result() { return result; }
    /** Fresh bounded copies of actual paid base and ingredients, never card templates. */
    public List<ItemStack> paid() { return paid.stream().map(ItemStack::copy).toList(); }
}
