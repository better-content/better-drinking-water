package com.bettercontent.betterdrinkingwater;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/** A paid bottle, owned exclusively by the player. Never stores an item stack. */
final class PlayerWaterReserve {
    static final String KEY = "better_drinking_water:reserve";
    record Reserve(int thirst, int quenched, double consumed, int purity) {}

    private PlayerWaterReserve() {}

    static Reserve read(Player player) {
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);
        if (!data.contains("version", Tag.TAG_INT) || data.getInt("version") != 1
                || !data.contains("thirst", Tag.TAG_INT) || !data.contains("quenched", Tag.TAG_INT)
                || !data.contains("consumed", Tag.TAG_DOUBLE) || !data.contains("purity", Tag.TAG_INT)) return null;
        int thirst = data.getInt("thirst"), quenched = data.getInt("quenched"), purity = data.getInt("purity");
        double consumed = data.getDouble("consumed");
        if (thirst <= 0 || thirst > 20 || quenched < 0 || quenched > 20 || purity < 0 || purity > 3
                || !Double.isFinite(consumed) || consumed < 0 || consumed >= 1) return null;
        return new Reserve(thirst, quenched, consumed, purity);
    }

    static void write(Player player, Reserve reserve) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (reserve == null) persisted.remove(KEY);
        else {
            CompoundTag data = new CompoundTag();
            data.putInt("version", 1);
            data.putInt("thirst", reserve.thirst());
            data.putInt("quenched", reserve.quenched());
            data.putDouble("consumed", reserve.consumed());
            data.putInt("purity", reserve.purity());
            persisted.put(KEY, data);
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    static void copy(Player original, Player replacement) {
        write(replacement, read(original));
    }
}
