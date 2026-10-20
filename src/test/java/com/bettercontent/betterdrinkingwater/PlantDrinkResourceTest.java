package com.bettercontent.betterdrinkingwater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlantDrinkResourceTest {
    private static final String NS = "better_drinking_water:";
    @Test void completeMatrixHasDistinctPlainAndSpecialistPreparations() throws Exception {
        var matrix = resource("/plant_drinks.json");
        assertEquals(250, matrix.get("serving_mb").getAsInt());
        Map<String, JsonObject> profiles = new HashMap<>();
        for (var raw : resource("/drinks.json").getAsJsonArray("drinks")) {
            var row=raw.getAsJsonObject(); profiles.put(row.get("id").getAsString(),row);
        }
        Set<String> plants = new HashSet<>();
        for (var raw : matrix.getAsJsonArray("coverage")) {
            var row=raw.getAsJsonObject(); assertTrue(plants.add(row.get("plant").getAsString()));
            if (row.get("plain").isJsonNull()) continue;
            String plain=row.get("plain").getAsString(), brew=row.get("brew").getAsString();
            assertNotEquals(plain,brew); assertTrue(profiles.containsKey(plain)); assertTrue(profiles.containsKey(brew));
            if (!plain.equals(NS+"belladonna_juice")) {
                assertEquals("none",profiles.get(plain).get("action").getAsString());
                assertTrue(profiles.get(plain).getAsJsonArray("effects").isEmpty(),"Safe plain juice must not inherit specialist effects: "+plain);
            }
        }
        assertTrue(plants.containsAll(Set.of("apple","pear","orange","kiwi","mango","lychee","peach","mangosteen","hawberry",
                "bayberry","fig","persimmon","durian","blueberry","cranberry","sweet_berry","glow_berry","mulberry","sourceberry",
                "skyberry","grimberry","lemon","pineapple","melon","hamimelon","chorus","coffee_cherry","rose_hip",
                "carrot","beetroot","tomato","cabbage","onion","garlic","potato","wheat","rice","pumpkin","cane","kelp","ginger","ube",
                "white_apple","firethorn","coconut","olive","desert_turnip","corn")));
        assertTrue(plants.contains("farmersrespite:wither_roots"), "Inspected unavailable native stock remains explicit coverage");
        var roots = matrix.getAsJsonArray("coverage").asList().stream().map(v -> v.getAsJsonObject())
                .filter(r -> r.get("plant").getAsString().equals("farmersrespite:wither_roots")).findFirst().orElseThrow();
        assertTrue(roots.get("plain").isJsonNull() && roots.get("brew").isJsonNull());
        assertTrue(roots.get("route").getAsString().contains("FRItems registers no Wither Roots item"));
        assertEquals("heal2",profiles.get(NS+"summerheart_cordial").get("action").getAsString());
        assertEquals("heal4",profiles.get(NS+"summerheart_reserve").get("action").getAsString());
        assertEquals("orange",profiles.get(NS+"suncrest_tonic").get("action").getAsString());
        assertEquals("lemon",profiles.get(NS+"bittergold_tonic").get("action").getAsString());
        var remedy=profiles.get(NS+"cloudwhite_remedy").getAsJsonArray("effects").get(0).getAsJsonObject();
        assertEquals("aether:remedy",remedy.get("id").getAsString());
        assertEquals(300,remedy.get("duration").getAsInt());
        for (String name : List.of("coconut_milk","olive_juice","groveguard_tonic")) {
            var drink=matrix.getAsJsonArray("drinks").asList().stream().map(v -> v.getAsJsonObject())
                    .filter(r -> r.get("name").getAsString().equals(name)).findFirst().orElseThrow();
            assertEquals(.025,drink.getAsJsonObject("nutrition").get("fats").getAsDouble());
            assertFalse(drink.getAsJsonObject("nutrition").has("sugar"),"No invented Nectar from lipid-bearing stock");
        }
    }
    @Test void nativeVolumesVesselsProcessesAndAgentExceptionsAreConserved() throws Exception {
        Set<String> names = new HashSet<>();
        for (var raw : resource("/plant_drinks.json").getAsJsonArray("drinks")) {
            var row=raw.getAsJsonObject(); String name=row.get("name").getAsString(); assertTrue(names.add(name));
            String process=row.get("process").getAsString(), path="/data/better_drinking_water/recipes/plant/"+name;
            var fill=resource(path+"_fill.json"); var empty=resource(path+"_empty.json");
            assertEquals(250,fill.getAsJsonArray("ingredients").get(1).getAsJsonObject().get("amount").getAsInt());
            assertEquals(250,empty.getAsJsonArray("results").get(1).getAsJsonObject().get("amount").getAsInt());
            assertEquals(fill.getAsJsonArray("ingredients").get(0),empty.getAsJsonArray("results").get(0),"Input/output vessel must be identical: "+name);
            if (process.equals("primary") || process.equals("secondary")) {
                var brew=resource(path+"_brew.json");
                assertEquals(1000,brew.getAsJsonObject("basefluid").get("count").getAsInt());
                assertEquals(1000,brew.getAsJsonObject("result").get("count").getAsInt());
                assertEquals(process.equals("secondary") ? 4800 : 9600,brew.get("fermentingtime").getAsInt());
                assertEquals(1,brew.getAsJsonArray("ingredients").size());
            } else if (process.equals("juice")) {
                var press=resource(path+"_press.json"); var hand=resource(path+"_hand.json");
                assertEquals(250,press.getAsJsonArray("results").get(0).getAsJsonObject().get("amount").getAsInt());
                assertEquals(press.getAsJsonArray("ingredients").size()+1,hand.getAsJsonArray("ingredients").size());
            } else {
                var steep=resource(path+"_steep.json");
                assertEquals(250,steep.getAsJsonObject("base").get("count").getAsInt());
                assertEquals(250,steep.getAsJsonObject("result").get("count").getAsInt());
                assertTrue(steep.getAsJsonArray("ingredients").size()<=4,"Fits actual kettle slots");
            }
        }
        assertTrue(names.size()>100);
        var flowers=resource("/data/better_drinking_water/tags/items/safe_starter_flowers.json").getAsJsonArray("values");
        Set<String> pool=new HashSet<>(); flowers.forEach(v -> assertTrue(pool.add(v.getAsString())));
        assertFalse(pool.contains("minecraft:wither_rose")); assertFalse(pool.contains("minecraft:lily_of_the_valley"));
        assertFalse(pool.contains("minecraft:chorus_flower"));
        assertEquals("minecraft:wither_rose",resource("/data/better_drinking_water/recipes/plant/grimberry_rush_brew.json")
                .getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString());
        assertEquals("minecraft:chorus_flower",resource("/data/better_drinking_water/recipes/plant/voidstep_elixir_brew.json")
                .getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString());
    }
    @Test void everyNativePlantBottleHasReproducibleDetailedDonorArtAndBoundedNutrition() throws Exception {
        for (var raw : resource("/plant_drinks.json").getAsJsonArray("drinks")) {
            var row=raw.getAsJsonObject();
            for (var value : row.getAsJsonObject("nutrition").entrySet()) {
                double dose=value.getValue().getAsDouble(); assertTrue(Double.isFinite(dose)&&dose>=0&&dose<=.1);
            }
            if (!row.get("item").getAsString().startsWith(NS)) continue;
            try (var in=getClass().getResourceAsStream("/assets/better_drinking_water/textures/item/"+row.get("name").getAsString()+".png")) {
                assertNotNull(in); var image=javax.imageio.ImageIO.read(in); assertEquals(16,image.getWidth());assertEquals(16,image.getHeight());
                Set<Integer> colours=new HashSet<>();for(int y=0;y<16;y++) for(int x=0;x<16;x++) colours.add(image.getRGB(x,y));
                assertTrue(colours.size()>10,"Donor pixel detail must not become a flat bottle silhouette");
            }
        }
        try(var in=getClass().getResourceAsStream("/META-INF/plant-drinks-donor.txt")) {
            assertNotNull(in); assertTrue(new String(in.readAllBytes(),StandardCharsets.UTF_8).contains("Copyright (c) 2020 vectorwing"));
        }
    }
    private static JsonObject resource(String path) throws Exception {
        try(var in=PlantDrinkResourceTest.class.getResourceAsStream(path)) {
            assertNotNull(in,"Missing authored resource "+path);
            return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
