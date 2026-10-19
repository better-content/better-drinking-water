package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.api.BeverageEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Immutable, validated entitlements; contains neither a usable item nor a transferable inventory. */
record BeverageServing(String source, int food, float saturation, CompoundTag nutrition,
                       List<DrinkCatalogue.Effect> effects, String action) {
    BeverageServing {
        nutrition = nutrition.copy();
        effects = List.copyOf(effects);
    }
    @Override public CompoundTag nutrition() { return nutrition.copy(); }

    static boolean allowed(ServerPlayer player) {
        return !MinecraftForge.EVENT_BUS.post(new BeverageEvents.Check(player));
    }

    static BeverageServing purchase(ServerPlayer player, ItemStack stack, DrinkCatalogue.Drink drink) {
        var properties = stack.getItem().getFoodProperties(stack, player);
        int food = properties == null ? 0 : properties.getNutrition();
        float saturation = properties == null ? 0 : food * properties.getSaturationModifier() * 2;
        CompoundTag nutrition = new CompoundTag();
        MinecraftForge.EVENT_BUS.post(new BeverageEvents.Profile(player, stack, nutrition));
        List<DrinkCatalogue.Effect> effects = new ArrayList<>();
        java.util.Map<Integer, Double> rolls = new java.util.HashMap<>();
        for (var effect : drink.effects()) {
            double roll = effect.group() == 0 ? player.getRandom().nextDouble()
                    : rolls.computeIfAbsent(Math.abs(effect.group()), ignored -> player.getRandom().nextDouble());
            if (effect.group() < 0) roll = 1 - roll;
            if (effect.chance() <= 0 || roll >= effect.chance()) continue;
            int amplifier = effect.amplifier();
            boolean tipsy = (drink.action().equals("booze") || drink.action().equals("dread_nog"))
                    && effect.id().equals("brewinandchewin:tipsy");
            boolean omen = drink.action().equals("dread_nog") && effect.id().equals("minecraft:bad_omen");
            if (tipsy || omen) {
                var active = player.getEffect(ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effect.id())));
                if (active != null) amplifier = Math.min(active.getAmplifier() + amplifier + 1, tipsy ? 9 : 4);
            }
            effects.add(new DrinkCatalogue.Effect(effect.id(), effect.duration(), amplifier, 1));
        }
        BeverageServing serving = new BeverageServing(drink.id(), food, saturation, nutrition, effects, drink.action());
        return read(serving.save()); // The persisted representation and live purchase share one validator.
    }

    CompoundTag save() {
        CompoundTag data = new CompoundTag();
        data.putString("source", source); data.putInt("food", food); data.putFloat("saturation", saturation);
        data.put("nutrition", nutrition.copy()); data.putString("action", action);
        ListTag list = new ListTag();
        for (var effect : effects) {
            CompoundTag e = new CompoundTag();
            e.putString("id", effect.id()); e.putInt("duration", effect.duration()); e.putInt("amplifier", effect.amplifier());
            list.add(e);
        }
        data.put("effects", list);
        return data;
    }

    static BeverageServing read(CompoundTag data) {
        if (!data.contains("source", Tag.TAG_STRING) || !data.contains("food", Tag.TAG_INT)
                || !data.contains("saturation", Tag.TAG_FLOAT) || !data.contains("nutrition", Tag.TAG_COMPOUND)
                || !data.contains("action", Tag.TAG_STRING) || !data.contains("effects", Tag.TAG_LIST)) return null;
        String source = data.getString("source"), action = data.getString("action");
        if (source.length() > 128 || ResourceLocation.tryParse(source) == null
                || !DrinkCatalogue.ACTIONS.contains(action)) return null;
        int food = data.getInt("food"); float saturation = data.getFloat("saturation");
        if (food < 0 || food > 64 || !Float.isFinite(saturation) || saturation < 0 || saturation > 128) return null;
        CompoundTag nutrition = data.getCompound("nutrition");
        if (!validNutrition(nutrition)) return null;
        ListTag list = data.getList("effects", Tag.TAG_COMPOUND);
        if (list.size() > 16 || list.size() != ((ListTag) data.get("effects")).size()) return null;
        List<DrinkCatalogue.Effect> effects = new ArrayList<>();
        for (Tag raw : list) {
            CompoundTag e = (CompoundTag) raw;
            if (!e.contains("id", Tag.TAG_STRING) || !e.contains("duration", Tag.TAG_INT) || !e.contains("amplifier", Tag.TAG_INT)) return null;
            String id = e.getString("id"); int duration = e.getInt("duration"), amplifier = e.getInt("amplifier");
            if (id.length() > 128 || ResourceLocation.tryParse(id) == null || duration < 1 || duration > 1200000 || amplifier < 0 || amplifier > 31) return null;
            // A removed provider must not turn a partially read serving into hydration without its costs.
            if (!ForgeRegistries.MOB_EFFECTS.containsKey(new ResourceLocation(id))
                    || ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(id)).isInstantenous()) return null;
            effects.add(new DrinkCatalogue.Effect(id, duration, amplifier, 1));
        }
        if (effects.stream().map(DrinkCatalogue.Effect::id).distinct().count() != effects.size()) return null;
        return new BeverageServing(source, food, saturation, nutrition, effects, action);
    }

    private static boolean validNutrition(CompoundTag nutrition) {
        if (nutrition.getAllKeys().size() > 5) return false;
        for (String key : nutrition.getAllKeys()) {
            if (key.equals("edible")) {
                if (!nutrition.contains(key, Tag.TAG_BYTE)) return false;
            } else if (key.equals("item")) {
                if (!nutrition.contains(key, Tag.TAG_STRING) || nutrition.getString(key).length() > 128
                        || ResourceLocation.tryParse(nutrition.getString(key)) == null) return false;
            } else if (key.equals("diet")) {
                if (!nutrition.contains(key, Tag.TAG_COMPOUND)) return false;
                CompoundTag diet = nutrition.getCompound(key);
                if (diet.getAllKeys().size() > 16) return false;
                for (String group : diet.getAllKeys()) if (group.length() > 64 || !bounded(diet, group)) return false;
            } else if (!(key.equals("sugar") || key.equals("alcohol")) || !bounded(nutrition, key)) return false;
        }
        return true;
    }

    private static boolean bounded(CompoundTag data, String key) {
        if (!data.contains(key, Tag.TAG_ANY_NUMERIC)) return false;
        double value = data.getDouble(key);
        return Double.isFinite(value) && value >= 0 && value <= 1;
    }

    void release(ServerPlayer player, double from, double to) {
        int hunger = FractionalCredit.delta(food, from, to);
        float saturationGain = (float) (saturation * (to - from));
        // FoodData.eat couples saturation to integer hunger; directly award its native bounded meter
        // so fractional hunger rounding does not erase or multiply saturation.
        var data = player.getFoodData();
        data.eat(hunger, 0);
        data.setSaturation(Math.min(data.getFoodLevel(), data.getSaturationLevel() + saturationGain));
        MinecraftForge.EVENT_BUS.post(new BeverageEvents.Sip(player, nutrition, from, to));
        BeverageEffects.release(player, effects, from, to);
        if (action.equals("heal2") || action.equals("heal4")) player.heal((float) ((action.equals("heal2") ? 2 : 4) * (to - from)));
        if (to >= 1) {
            if (action.equals("rest_reset")) player.resetStat(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.TIME_SINCE_REST));
            else if (action.equals("milk")) player.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
            else if (action.equals("poison")) player.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
            else if (action.startsWith("cure_")) cleanse(player);
        }
    }

    private void cleanse(ServerPlayer player) {
        var milk = new ItemStack(Items.MILK_BUCKET);
        var lowPriority = ForgeRegistries.MOB_EFFECTS.tags().getTag(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.MOB_EFFECT, new ResourceLocation("brewinandchewin",
                action.equals("cure_one") ? "low_priority/milk_bottle" : "low_priority/hot_cocoa")));
        boolean allLowPriority = player.getActiveEffects().stream().allMatch(e -> lowPriority.contains(e.getEffect()));
        var candidates = new ArrayList<>(player.getActiveEffects().stream()
                .filter(e -> action.equals("cure_harmful_two") || !lowPriority.contains(e.getEffect()) || allLowPriority)
                .filter(e -> e.isCurativeItem(milk))
                .filter(e -> action.equals("cure_one") || e.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                .toList());
        int removals = action.equals("cure_harmful_two") ? 2 : 1;
        for (int i = 0; i < removals && !candidates.isEmpty(); i++) {
            var selected = candidates.get(player.getRandom().nextInt(candidates.size()));
            // Strong cocoa's native second draw uses the original candidate list: it can pick
            // an already removed effect. Preserve its up-to-two behavior rather than buffing it.
            var active = player.getEffect(selected.getEffect());
            if (active != null && !MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.MobEffectEvent.Remove(player, active)))
                player.removeEffect(active.getEffect());
        }
    }
}
