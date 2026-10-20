package com.bettercontent.betterdrinkingwater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Finite authored plant stock; never admits arbitrary food/fluid handlers. */
public final class PlantDrinks {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WaterSurvival.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, WaterSurvival.MOD_ID);
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, WaterSurvival.MOD_ID);
    private static final Map<String, JsonObject> ROWS = load();
    private PlantDrinks() {}
    private static Map<String, JsonObject> load() {
        try (var in = PlantDrinks.class.getResourceAsStream("/plant_drinks.json")) {
            if (in == null) throw new IllegalStateException("Missing plant_drinks.json");
            var root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1 || root.get("serving_mb").getAsInt() != 250)
                throw new IllegalArgumentException("Unsupported plant drink schema/serving");
            Map<String, JsonObject> rows = new LinkedHashMap<>();
            for (var raw : root.getAsJsonArray("drinks")) {
                var row = raw.getAsJsonObject();
                String name = row.get("name").getAsString();
                if (!name.matches("[a-z][a-z0-9_]{0,63}") || rows.putIfAbsent(name, row) != null)
                    throw new IllegalArgumentException("Duplicate/invalid plant drink " + name);
                new ResourceLocation(row.get("item").getAsString());
                if (!row.get("ingredient").isJsonNull()) new ResourceLocation(row.get("ingredient").getAsString());
                int count = row.get("count").getAsInt(), food = row.get("food").getAsInt();
                if (row.get("count").getAsDouble() != count || row.get("food").getAsDouble() != food
                        || count < 1 || count > 4 || food < 0 || food > 8
                        || !row.get("colour").getAsString().matches("[0-9a-f]{6}"))
                    throw new IllegalArgumentException("Invalid plant quantity/colour " + name);
                for (var nutrient : row.getAsJsonObject("nutrition").entrySet()) {
                    double value = nutrient.getValue().getAsDouble();
                    if (!java.util.Set.of("fruits", "vegetables", "grains", "proteins", "dairy", "fats", "sugar").contains(nutrient.getKey())
                            || !Double.isFinite(value) || value < 0 || value > .1)
                        throw new IllegalArgumentException("Invalid plant nutrition " + name);
                }
                if (!java.util.Set.of("buff", "hydration_plus", "unsafe").contains(row.get("role").getAsString()))
                    throw new IllegalArgumentException("Unknown plant drink role " + name);
                for (var mod : row.getAsJsonArray("mods"))
                    if (!mod.getAsString().matches("[a-z][a-z0-9_]{0,63}")) throw new IllegalArgumentException("Invalid required mod");
                if (!java.util.Set.of("juice", "milk", "primary", "secondary", "infusion").contains(row.get("process").getAsString()))
                    throw new IllegalArgumentException("Unknown plant process " + name);
            }
            if (rows.isEmpty() || rows.size() > 256) throw new IllegalArgumentException("Invalid plant catalogue size");
            for (var row : rows.values()) if (!row.get("base").isJsonNull()) {
                if (!rows.containsKey(row.get("base").getAsString()) || row.get("agent").isJsonNull())
                    throw new IllegalArgumentException("Unresolved plant brewing reference");
                String agent = row.get("agent").getAsString();
                new ResourceLocation(agent.startsWith("#") ? agent.substring(1) : agent);
            }
            return java.util.Collections.unmodifiableMap(rows);
        } catch (Exception e) { throw new IllegalStateException("Invalid plant catalogue", e); }
    }
    public static double nutrient(String id, String group) {
        if (!id.startsWith(WaterSurvival.MOD_ID + ":")) return 0;
        var row = ROWS.get(id.substring(id.indexOf(':') + 1));
        return row != null && row.getAsJsonObject("nutrition").has(group)
                ? row.getAsJsonObject("nutrition").get(group).getAsDouble() : 0;
    }
    public static String role(String id) {
        var row = id.startsWith(WaterSurvival.MOD_ID + ":") ? ROWS.get(id.substring(id.indexOf(':') + 1)) : null;
        return row == null ? "" : row.get("role").getAsString();
    }
    public static boolean contains(String id) {
        return id.startsWith(WaterSurvival.MOD_ID + ":") && ROWS.containsKey(id.substring(id.indexOf(':') + 1));
    }
    static void validateLoaded() {
        for (var row : ROWS.values()) {
            boolean available = true;
            for (var mod : row.getAsJsonArray("mods")) available &= net.minecraftforge.fml.ModList.get().isLoaded(mod.getAsString());
            if (!available) continue;
            String item = row.get("item").getAsString();
            if (!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(item))
                    || !ForgeRegistries.FLUIDS.containsKey(new ResourceLocation(WaterSurvival.MOD_ID, row.get("name").getAsString()))
                    || DrinkCatalogue.find(new net.minecraft.world.item.ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(item)))) == null)
                throw new IllegalStateException("Unresolved plant drink registration/profile " + item);
            if (!row.get("ingredient").isJsonNull() && !ForgeRegistries.ITEMS.containsKey(new ResourceLocation(row.get("ingredient").getAsString())))
                throw new IllegalStateException("Unresolved plant ingredient " + row.get("ingredient"));
        }
    }
    static void register(IEventBus bus) {
        ROWS.forEach((name, row) -> {
            boolean available = true;
            for (var mod : row.getAsJsonArray("mods"))
                available &= net.minecraftforge.fml.ModList.get().isLoaded(mod.getAsString());
            if (!available) return;
            if (row.get("item").getAsString().equals(WaterSurvival.MOD_ID + ":" + name))
                ITEMS.register(name, () -> new PlantDrinkItem(properties(row)));
            int colour = (int) Long.parseLong(row.get("colour").getAsString(), 16) | 0xff000000;
            var type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create().density(1050).viscosity(1200)) {
                @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
                        public ResourceLocation getStillTexture() { return new ResourceLocation("minecraft:block/water_still"); }
                        public ResourceLocation getFlowingTexture() { return new ResourceLocation("minecraft:block/water_flow"); }
                        public int getTintColor() { return colour; }
                    });
                }
            });
            RegistryObject<Fluid> source = RegistryObject.create(new ResourceLocation(WaterSurvival.MOD_ID, name), ForgeRegistries.FLUIDS);
            RegistryObject<Fluid> flowing = RegistryObject.create(new ResourceLocation(WaterSurvival.MOD_ID, "flowing_" + name), ForgeRegistries.FLUIDS);
            var props = new ForgeFlowingFluid.Properties(type, source, flowing);
            FLUIDS.register(name, () -> new ForgeFlowingFluid.Source(props));
            FLUIDS.register("flowing_" + name, () -> new ForgeFlowingFluid.Flowing(props));
        });
        ITEMS.register(bus); TYPES.register(bus); FLUIDS.register(bus);
    }
    private static Item.Properties properties(JsonObject row) {
        var properties = new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE);
        int food = row.get("food").getAsInt();
        if (food > 0) properties.food(new FoodProperties.Builder().nutrition(food).saturationMod(.15f).alwaysEat().build());
        return properties;
    }
}
