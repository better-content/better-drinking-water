package com.bettercontent.betterdrinkingwater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Authored resource contracts only: no Minecraft bootstrap or retained audit outputs as fixtures. */
class DrinkCatalogueResourceTest {
    private static final Set<String> ACTIONS = Set.of("none", "milk", "poison", "heal2", "heal4", "cure_one",
            "cure_harmful_one", "cure_harmful_two", "booze", "dread_nog", "rest_reset");

    @Test
    void versionedCatalogueHasUniqueDisjointSupportedAndExcludedIds() throws Exception {
        var root = resource("/drinks.json");
        assertEquals(1, root.get("version").getAsInt());
        var supported = profiles(root);
        Set<String> excluded = new HashSet<>();
        for (var raw : root.getAsJsonArray("excluded")) {
            var row = raw.getAsJsonObject();
            String id = row.get("id").getAsString();
            assertId(id);
            assertTrue(excluded.add(id), "Duplicate exclusion: " + id);
            assertFalse(supported.containsKey(id), "An excluded item cannot also be automatically sipped: " + id);
            assertFalse(row.get("reason").getAsString().isBlank(), "Exclusions need an explicit reason: " + id);
        }
        assertFalse(supported.isEmpty());
        assertFalse(excluded.isEmpty());
    }

    @Test
    void allSupportedProfilesHaveBoundedExplicitEntitlements() throws Exception {
        for (var entry : profiles(resource("/drinks.json")).entrySet()) {
            var row = entry.getValue();
            assertId(entry.getKey());
            boundedInt(row, "hydration", 1, 64);
            boundedInt(row, "quenching", 0, 128);
            String remainder = row.get("remainder").getAsString();
            if (!remainder.isEmpty()) assertId(remainder);
            assertTrue(ACTIONS.contains(row.get("action").getAsString()), "Unknown action: " + entry.getKey());
            assertFalse(row.get("source").getAsString().isBlank(), "Profiles must identify their authored/native basis");
            var effects = row.getAsJsonArray("effects");
            assertTrue(effects.size() <= 16);
            Set<String> unique = new HashSet<>();
            for (var raw : effects) {
                var effect = raw.getAsJsonObject();
                String id = effect.get("id").getAsString();
                assertId(id);
                assertTrue(unique.add(id), "Duplicate effect: " + entry.getKey() + "/" + id);
                boundedInt(effect, "duration", 1, 1200000);
                boundedInt(effect, "amplifier", 0, 31);
                double chance = effect.get("chance").getAsDouble();
                assertTrue(Double.isFinite(chance) && chance >= 0 && chance <= 1);
                if (effect.has("group")) boundedInt(effect, "group", -16, 16);
                assertNotEquals("minecraft:instant_health", id,
                        "Instant healing belongs in fractional heal2/heal4 actions, not timed potion grants");
            }
        }
    }

    @Test
    void coffeeTypoIsNotAnAliasAndRealStrongCoffeeHasItsNativeProfile() throws Exception {
        var rows = profiles(resource("/drinks.json"));
        assertFalse(rows.containsKey("farmersrespite:stong_coffee"));
        var coffee = rows.get("farmersrespite:strong_coffee");
        assertNotNull(coffee);
        assertEffect(coffee, "farmersrespite:caffeinated", 3000, 2);
        assertEquals(22, rows.get("farmersrespite:rose_hip_tea").get("quenching").getAsInt());
    }

    @Test
    void sixteenNativeBrewsAreSupportedButTankardsAndMealsAreNot() throws Exception {
        var rows = profiles(resource("/drinks.json"));
        Set<String> names = Set.of("beer", "bloody_mary", "dread_nog", "egg_grog", "glittering_grenadine",
                "kombucha", "mead", "pale_jane", "red_rum", "rice_wine", "saccharine_rum", "salty_folly",
                "steel_toe_stout", "strongroot_ale", "vodka", "withering_dross");
        Set<String> actual = new HashSet<>();
        rows.keySet().stream().filter(id -> id.startsWith("brewinandchewin:"))
                .forEach(id -> actual.add(id.substring("brewinandchewin:".length())));
        assertEquals(names, actual, "Only the native sixteen finite brews, not ingredients or solid food");
        for (String name : names) assertEquals("brewinandchewin:tankard",
                rows.get("brewinandchewin:" + name).get("remainder").getAsString());
        for (String id : Set.of("brewinandchewin:tankard", "minecraft:mushroom_stew", "minecraft:sugar"))
            assertFalse(rows.containsKey(id), "Empty vessels and arbitrary edible items are not beverages: " + id);
        Set<String> excluded = new HashSet<>();
        for (var raw : resource("/drinks.json").getAsJsonArray("excluded"))
            excluded.add(raw.getAsJsonObject().get("id").getAsString());
        for (String id : Set.of("brewinandchewin:apple_jelly", "brewinandchewin:glow_berry_marmalade",
                "brewinandchewin:sweet_berry_jam", "fruitsdelight:apple_jelly", "farmersdelight:bone_broth",
                "tconstruct:meat_soup", "fruitsdelight:bayberry_soup", "ubesdelight:fish_sauce_bottle",
                "ubesdelight:halo_halo", "upgrade_aquatic:mulberry_jam_bottle")) {
            assertFalse(rows.containsKey(id), "A jar/bowl/ingredient is not automatically a beverage: " + id);
            assertTrue(excluded.contains(id), "Known unsupported consumable must have an authored exclusion: " + id);
        }
    }

    @Test
    void fruitsProfilesUseNormalizedStrengthAndNamedDurations() throws Exception {
        var rows = profiles(resource("/drinks.json"));
        for (var entry : rows.entrySet()) if (entry.getKey().startsWith("fruitsdelight:")) {
            for (var raw : entry.getValue().getAsJsonArray("effects")) {
                var effect = raw.getAsJsonObject();
                String id = effect.get("id").getAsString();
                assertEquals(0, effect.get("amplifier").getAsInt(),
                        "Fruits effect encoding must normalize native potency to zero-based amplifier");
                assertFalse(Set.of("minecraft:health_boost", "fruitsdelight:cycling", "fruitsdelight:digesting").contains(id),
                        "Unsupported scaling/lifecycle effects must remain suppressed: " + id);
                if (Set.of("minecraft:speed", "minecraft:haste", "minecraft:strength", "minecraft:jump_boost",
                        "minecraft:regeneration", "minecraft:resistance", "minecraft:fire_resistance",
                        "minecraft:water_breathing", "minecraft:invisibility", "minecraft:night_vision",
                        "minecraft:absorption", "minecraft:luck", "minecraft:slow_falling", "minecraft:conduit_power",
                        "minecraft:dolphins_grace", "minecraft:hero_of_the_village", "minecraft:saturation").contains(id))
                    assertTrue(effect.get("duration").getAsInt() <= 1200,
                            "Normalized vanilla-beneficial Fruits effects have a 1200-tick ceiling");
            }
        }
        assertEffect(rows.get("fruitsdelight:pear_juice"), "fruitsdelight:lozenge", 200, 0);
        assertEffect(rows.get("fruitsdelight:mangosteen_tea"), "fruitsdelight:sliding", 800, 0);
        assertEffect(rows.get("fruitsdelight:peach_tea"), "fruitsdelight:heal_aura", 100, 0);
        assertEffect(rows.get("fruitsdelight:mango_tea"), "minecraft:strength", 200, 0);
        assertEffect(rows.get("fruitsdelight:mango_tea"), "fruitsdelight:rage_aura", 200, 0);
        assertEffect(rows.get("fruitsdelight:bellini_cocktail"), "minecraft:nausea", 100, 0);
        assertTrue(rows.get("fruitsdelight:kiwi_juice").getAsJsonArray("effects").isEmpty());
        assertTrue(rows.get("fruitsdelight:hamimelon_juice").getAsJsonArray("effects").isEmpty());
    }

    @Test
    void conciseCompatibilityLineAndDrinksSlotNameAreExact() throws Exception {
        var language = resource("/assets/better_drinking_water/lang/en_us.json");
        assertEquals("Drinks", language.get("curios.identifier.water").getAsString());
        assertEquals("Drinks slot compatible", language.get("tooltip.better_drinking_water.slot_compatible").getAsString());
    }

    private static JsonObject resource(String path) throws Exception {
        try (var input = DrinkCatalogueResourceTest.class.getResourceAsStream(path)) {
            assertNotNull(input, "Missing authored resource " + path);
            try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    private static Map<String, JsonObject> profiles(JsonObject root) {
        Map<String, JsonObject> rows = new HashMap<>();
        for (var raw : root.getAsJsonArray("drinks")) {
            var row = raw.getAsJsonObject();
            String id = row.get("id").getAsString();
            assertNull(rows.putIfAbsent(id, row), "Duplicate supported id: " + id);
        }
        return rows;
    }

    private static void assertId(String id) {
        assertTrue(id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), "Malformed namespaced id: " + id);
    }

    private static void boundedInt(JsonObject row, String key, int min, int max) {
        double value = row.get(key).getAsDouble();
        assertTrue(Double.isFinite(value) && value == Math.rint(value) && value >= min && value <= max,
                "Out-of-range or nonintegral " + key + ": " + value);
    }

    private static void assertEffect(JsonObject profile, String id, int duration, int amplifier) {
        assertNotNull(profile, "Required named beverage profile");
        var effect = profile.getAsJsonArray("effects").asList().stream().map(raw -> raw.getAsJsonObject())
                .filter(row -> row.get("id").getAsString().equals(id)).findFirst().orElseThrow();
        assertEquals(duration, effect.get("duration").getAsInt(), id + " duration");
        assertEquals(amplifier, effect.get("amplifier").getAsInt(), id + " amplifier");
    }
}
