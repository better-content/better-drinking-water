package com.bettercontent.betterdrinkingwater;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/** Replenishing duration must not reset periodic healing/damage/saturation-spending phase. */
public final class BeverageEffectCadence {
    private static final String KEY = "better_drinking_water:effect_cadence";
    private static final Set<String> PERIODIC = Set.of("minecraft:regeneration", "minecraft:poison",
            "minecraft:wither", "brewinandchewin:sweet_heart", "farmersdelight:comfort");
    private BeverageEffectCadence() {}

    static void manage(ServerPlayer player, MobEffect effect, int amplifier, boolean continuing) {
        String id = ForgeRegistries.MOB_EFFECTS.getKey(effect).toString();
        if (!PERIODIC.contains(id)) return;
        CompoundTag clocks = player.getPersistentData().getCompound(KEY);
        CompoundTag clock = clocks.getCompound(id);
        if (!continuing || clock.getInt("amplifier") != amplifier) clock = new CompoundTag();
        clock.putInt("amplifier", amplifier);
        clocks.put(id, clock);
        player.getPersistentData().put(KEY, clocks);
    }

    /** Called at the real native effect tick, not an additional healing/damage tick. */
    public static boolean shouldTick(LivingEntity entity, MobEffect effect, int duration, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) return effect.isDurationEffectTick(duration, amplifier);
        String id = ForgeRegistries.MOB_EFFECTS.getKey(effect).toString();
        if (!PERIODIC.contains(id)) return effect.isDurationEffectTick(duration, amplifier);
        CompoundTag clocks = player.getPersistentData().getCompound(KEY);
        if (!clocks.contains(id)) return effect.isDurationEffectTick(duration, amplifier);
        CompoundTag clock = clocks.getCompound(id);
        if (clock.getInt("amplifier") != amplifier) {
            clocks.remove(id);
            return effect.isDurationEffectTick(duration, amplifier);
        }
        int elapsed = Math.floorMod(clock.getInt("elapsed"), 4000) + 1;
        // 4000 is a common multiple of all authored base periods (20,25,40,50,80).
        clock.putInt("elapsed", elapsed);
        return effect.isDurationEffectTick(elapsed, amplifier);
    }

    public static void ended(LivingEntity entity, MobEffect effect) {
        if (entity instanceof ServerPlayer player) {
            String id = ForgeRegistries.MOB_EFFECTS.getKey(effect).toString();
            player.getPersistentData().getCompound(KEY).remove(id);
        }
    }
}
