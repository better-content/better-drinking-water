package com.bettercontent.betterdrinkingwater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Authored finite servings. Discovery never admits arbitrary edible or fluid-capable items. */
final class DrinkCatalogue {
    static final List<String> ACTIONS = List.of("none", "milk", "poison", "heal2", "heal4", "cure_one",
            "cure_harmful_one", "cure_harmful_two", "booze", "dread_nog", "rest_reset");
    record Effect(String id, int duration, int amplifier, double chance, int group) {
        Effect(String id, int duration, int amplifier, double chance) { this(id, duration, amplifier, chance, 0); }
    }
    record Drink(String id, int hydration, int quenching, String remainder, List<Effect> effects, String action) {
        boolean available() {
            if (!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id))) return false;
            if (!remainder.isEmpty() && !ForgeRegistries.ITEMS.containsKey(new ResourceLocation(remainder))) return false;
            return effects.stream().allMatch(e -> ForgeRegistries.MOB_EFFECTS.containsKey(new ResourceLocation(e.id()))
                    && !ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(e.id())).isInstantenous());
        }
    }
    private static final Map<String, Drink> DRINKS = load();
    private DrinkCatalogue() {}

    static Drink find(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Drink drink = DRINKS.get(WaterSources.id(stack));
        return drink != null && drink.available() ? drink : null;
    }

    static Map<String, Drink> entries() { return DRINKS; }

    static void validateLoaded() {
        for (Drink drink : DRINKS.values()) {
            if (ForgeRegistries.ITEMS.containsKey(new ResourceLocation(drink.id())) && !drink.available())
                throw new IllegalStateException("Unresolved loaded drink profile: " + drink.id());
        }
    }

    private static Map<String, Drink> load() {
        try (var input = DrinkCatalogue.class.getResourceAsStream("/drinks.json")) {
            if (input == null) throw new IllegalStateException("Missing authored drinks.json");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported drinks catalogue version");
            Map<String, Drink> drinks = new LinkedHashMap<>();
            for (var value : root.getAsJsonArray("drinks")) {
                JsonObject row = value.getAsJsonObject();
                String id = row.get("id").getAsString(), remainder = row.get("remainder").getAsString();
                new ResourceLocation(id);
                if (!remainder.isEmpty()) new ResourceLocation(remainder);
                int hydration = row.get("hydration").getAsInt(), quenching = row.get("quenching").getAsInt();
                if (hydration < 1 || hydration > 64 || quenching < 0 || quenching > 128)
                    throw new IllegalArgumentException("Invalid drink quantities: " + id);
                String action = row.get("action").getAsString();
                if (!ACTIONS.contains(action)) throw new IllegalArgumentException("Unknown action: " + id);
                List<Effect> effects = new ArrayList<>();
                for (var raw : row.getAsJsonArray("effects")) {
                    var e = raw.getAsJsonObject();
                    String effect = e.get("id").getAsString();
                    new ResourceLocation(effect);
                    int duration = e.get("duration").getAsInt(), amplifier = e.get("amplifier").getAsInt();
                    double chance = e.get("chance").getAsDouble();
                    int group = e.has("group") ? e.get("group").getAsInt() : 0;
                    if (Math.abs((long) group) > 16 || duration < 1 || duration > 1200000 || amplifier < 0 || amplifier > 31
                            || !Double.isFinite(chance) || chance < 0 || chance > 1)
                        throw new IllegalArgumentException("Invalid effect: " + id);
                    effects.add(new Effect(effect, duration, amplifier, chance, group));
                }
                if (effects.size() > 16 || effects.stream().map(Effect::id).distinct().count() != effects.size())
                    throw new IllegalArgumentException("Duplicate/excessive drink effects: " + id);
                Drink drink = new Drink(id, hydration, quenching, remainder, List.copyOf(effects), action);
                if (drinks.putIfAbsent(id, drink) != null) throw new IllegalArgumentException("Duplicate drink: " + id);
            }
            return Map.copyOf(drinks);
        } catch (Exception error) {
            throw new IllegalStateException("Invalid authored beverage catalogue", error);
        }
    }
}
