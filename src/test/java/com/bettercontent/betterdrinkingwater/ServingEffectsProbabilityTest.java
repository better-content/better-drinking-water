package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.api.ServingEffects;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/** Real BeverageChance selection, but no Minecraft/Forge bootstrap or active-player simulation. */
final class ServingEffectsProbabilityTest {
    private static ResourceLocation id(String value) { return new ResourceLocation(value); }
    private static final ResourceLocation SOURCE = id("better_drinking_water:paid_tea");
    private record NativeDose(String id, double chance, int group, int amplifier) {}
    private record Draws(int draws, long nextRandom, List<ServingEffects.Selected> selected) {}
    private static Draws draw(boolean restricted) {
        Random random = new Random(71342); int draws = 0; var selected = new ArrayList<ServingEffects.Selected>();
        List<NativeDose> doses = List.of(new NativeDose("minecraft:speed", .75, 7, 3), new NativeDose("kaleidoscope_cookery:vigor", .25, -7, 2),
                new NativeDose("brewinandchewin:tipsy", .5, 0, 9), new NativeDose("minecraft:bad_omen", 1, 0, 4));
        for (int serving = 0; serving < 100; serving++) {
            var groups = new HashMap<Integer, Double>();
            for (NativeDose effect : doses) {
                double roll;
                if (effect.group() == 0) { roll = random.nextDouble(); draws++; }
                else if (groups.containsKey(Math.abs(effect.group()))) roll = groups.get(Math.abs(effect.group()));
                else { roll = random.nextDouble(); draws++; groups.put(Math.abs(effect.group()), roll); }
                if (!BeverageChance.selected(roll, effect.chance(), effect.group())) continue;
                // Native harmful amplifier selection is already complete before entitlement restriction.
                var dose = new ServingEffects.Selected(id(effect.id()), 2400, effect.amplifier());
                if (restricted) ServingEffects.restrict(SOURCE, dose).ifPresent(selected::add); else selected.add(dose);
            }
        }
        return new Draws(draws, random.nextLong(), List.copyOf(selected));
    }
    @Test void publicApiDoesNotRerollOrChangeHarmfulSelection() {
        ResourceLocation owner = id("better_cooking:test_effect_policy");
        ServingEffects.register(owner, (source, selected) -> {
            assertEquals(SOURCE, source, "actual paid source ID");
            if (selected.id().equals(id("kaleidoscope_cookery:vigor"))) return ServingEffects.Bounds.SUPPRESS;
            if (selected.id().equals(id("minecraft:speed"))) return new ServingEffects.Bounds(false, 1200, 0);
            return ServingEffects.Bounds.IDENTITY;
        });
        assertThrows(IllegalStateException.class, () -> ServingEffects.register(owner, (s, e) -> ServingEffects.Bounds.IDENTITY));
        Draws baseline = draw(false), restricted = draw(true);
        assertEquals(300, baseline.draws(), "one shared signed-group roll plus two independent rolls per serving");
        assertEquals(baseline.draws(), restricted.draws(), "restriction consumes no additional probability draws");
        assertEquals(baseline.nextRandom(), restricted.nextRandom(), "subsequent native random stream unchanged");
        assertEquals(baseline.selected().stream().filter(e -> e.id().getPath().equals("tipsy") || e.id().getPath().equals("bad_omen")).toList(),
                restricted.selected().stream().filter(e -> e.id().getPath().equals("tipsy") || e.id().getPath().equals("bad_omen")).toList(), "harmful probabilities/order/duration/native amplifiers unchanged");
        assertTrue(restricted.selected().stream().filter(e -> e.id().getPath().equals("speed")).allMatch(e -> e.duration() == 1200 && e.amplifier() == 0));
        assertEquals(baseline.selected().stream().filter(e -> e.id().getPath().equals("speed")).count(), restricted.selected().stream().filter(e -> e.id().getPath().equals("speed")).count(), "benefit selection chance unchanged");
        assertTrue(restricted.selected().stream().noneMatch(e -> e.id().getPath().equals("vigor")), "suppressed benefit never published");
        var cap = ServingEffects.restrict(SOURCE, new ServingEffects.Selected(id("minecraft:speed"), 2400, 3)).orElseThrow();
        assertEquals(cap, ServingEffects.restrict(SOURCE, cap).orElseThrow(), "old paid reserve reread is idempotent");
        ServingEffects.freeze(); ServingEffects.freeze();
        assertThrows(IllegalStateException.class, () -> ServingEffects.register(id("late:provider"), (s, e) -> ServingEffects.Bounds.IDENTITY));
        assertThrows(NullPointerException.class, () -> ServingEffects.register(null, (s, e) -> ServingEffects.Bounds.IDENTITY));
        assertThrows(NullPointerException.class, () -> ServingEffects.register(id("null:provider"), null));
        assertThrows(NullPointerException.class, () -> ServingEffects.restrict(null, cap));
        assertThrows(NullPointerException.class, () -> ServingEffects.restrict(SOURCE, null));
        assertThrows(NullPointerException.class, () -> new ServingEffects.Selected(null, 20, 0));
        assertThrows(IllegalArgumentException.class, () -> new ServingEffects.Selected(id("minecraft:speed"), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ServingEffects.Bounds(false, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ServingEffects.Bounds(false, 1200, 32));
    }
}
