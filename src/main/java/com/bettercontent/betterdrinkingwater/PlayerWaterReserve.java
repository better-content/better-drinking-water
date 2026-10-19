package com.bettercontent.betterdrinkingwater;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/** A paid water portion, owned exclusively by the player. Never stores an item stack. */
final class PlayerWaterReserve {
    static final String KEY = "better_drinking_water:reserve";
    record Reserve(int thirst, int quenched, double consumed, int purity, BeverageServing beverage) {
        Reserve(int thirst, int quenched, double consumed, int purity) { this(thirst, quenched, consumed, purity, null); }
        Reserve progress(double fraction) { return new Reserve(thirst, quenched, fraction, purity, beverage); }
    }

    private PlayerWaterReserve() {}

    static Reserve read(Player player) {
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);
        if (!data.contains("version", Tag.TAG_INT) || (data.getInt("version") != 1 && data.getInt("version") != 2)
                || !data.contains("thirst", Tag.TAG_INT) || !data.contains("quenched", Tag.TAG_INT)
                || !data.contains("consumed", Tag.TAG_DOUBLE) || !data.contains("purity", Tag.TAG_INT)) return null;
        int thirst = data.getInt("thirst"), quenched = data.getInt("quenched"), purity = data.getInt("purity");
        double consumed = data.getDouble("consumed");
        if (thirst <= 0 || thirst > 64 || quenched < 0 || quenched > 128 || purity < 0 || purity > 3
                || !Double.isFinite(consumed) || consumed < 0 || consumed >= 1) return null;
        BeverageServing beverage = null;
        if (data.getInt("version") == 2) {
            if (!data.contains("beverage", Tag.TAG_COMPOUND)) return null;
            beverage = BeverageServing.read(data.getCompound("beverage"));
            if (beverage == null) return null;
        } else if (data.contains("beverage") || thirst > 20 || quenched > 20) return null;
        return new Reserve(thirst, quenched, consumed, WaterQuality.normalize(purity), beverage);
    }

    static void write(Player player, Reserve reserve) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (reserve == null) persisted.remove(KEY);
        else {
            CompoundTag data = new CompoundTag();
            data.putInt("version", reserve.beverage() == null ? 1 : 2);
            if (reserve.beverage() != null) data.put("beverage", reserve.beverage().save());
            data.putInt("thirst", reserve.thirst());
            data.putInt("quenched", reserve.quenched());
            data.putDouble("consumed", reserve.consumed());
            data.putInt("purity", reserve.purity());
            persisted.put(KEY, data);
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    static void copy(Player original, Player replacement) { copy(original, replacement, false); }

    static void copy(Player original, Player replacement, boolean wasDeath) {
        Reserve reserve = read(original);
        // Water keeps its established persistence. A beverage loses its entire unfinished serving,
        // never retaining hydration while selectively deleting its Draught cost or other consequences.
        write(replacement, wasDeath && reserve != null && reserve.beverage() != null ? null : reserve);
    }
}
