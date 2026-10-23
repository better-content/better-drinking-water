package com.bettercontent.betterdrinkingwater.fixture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.ghen.thirst.foundation.common.loot.AddLootTableModifier;
import dev.ghen.thirst.foundation.common.loot.ModLootModifiers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixins;

import java.nio.file.Files;
import java.util.HashSet;

/** Nonshipping assertion of the actual production compatibility path; registers nothing. */
@GameTestHolder("better_drinking_water")
@PrefixGameTestTemplate(false)
public final class ThirstCompatibilityGameTests {
    @GameTest(templateNamespace = "better_drinking_water", template = "empty")
    public static void realThirstCodecEntriesAndExistingRecursionFix(GameTestHelper helper) throws Exception {
        helper.assertTrue(ModList.get().isLoaded("better_compat_fixes"), "Actual production Compat provider required");
        helper.assertTrue(ModList.get().isLoaded("better_gameplay_notices"), "Actual mandatory Notices provider required");
        var codec = ForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.get()
                .getValue(new ResourceLocation("thirst", "add_loot_table"));
        helper.assertTrue(ModLootModifiers.ADD_LOOT_TABLE.isPresent(), "Native DeferredRegister entry must resolve");
        helper.assertTrue(codec == ModLootModifiers.ADD_LOOT_TABLE.get() && codec == AddLootTableModifier.CODEC.get(),
                "Existing native ID must bind the exact native codec, not a stand-in");
        helper.assertTrue(Mixins.getMixinsForClass(AddLootTableModifier.class.getName()).stream().anyMatch(info ->
                        info.getClassName().equals("com.bettercontent.bettercompatfixes.mixin.thirst.AddLootTableModifierMixin")),
                "Public Mixin applied-target metadata must report the existing production recursion fix");

        var modFile = ModList.get().getModFileById("thirst").getFile();
        JsonObject index;
        try (var reader = Files.newBufferedReader(modFile.findResource("data", "forge", "loot_modifiers", "global_loot_modifiers.json"))) {
            index = JsonParser.parseReader(reader).getAsJsonObject();
        }
        var seen = new HashSet<String>();
        IGlobalLootModifier mineshaft = null;
        JsonObject mineshaftJson = null;
        for (var entry : index.getAsJsonArray("entries")) {
            var id = new ResourceLocation(entry.getAsString());
            helper.assertTrue(id.getNamespace().equals("thirst") && id.getPath().matches(
                    "add_loot_(shipwreck_supply|abandoned_mineshaft|simple_dungeon|nether_bridge|bastion_other)(_fr|_bc)?"),
                    "Preserve the actual native fifteen-entry contract");
            helper.assertTrue(seen.add(id.toString()), "Native GLM index must not duplicate an entry");
            JsonObject json;
            try (var reader = Files.newBufferedReader(modFile.findResource("data", "thirst", "loot_modifiers", id.getPath() + ".json"))) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }
            var modifier = IGlobalLootModifier.DIRECT_CODEC.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(false, message -> { throw new AssertionError("Real native GLM decode failed: " + id + ": " + message); });
            helper.assertTrue(modifier instanceof AddLootTableModifier && modifier.codec() == codec,
                    "Every original entry must decode through the actual registered native codec");
            var source = new ResourceLocation(json.get("lootTable").getAsString());
            helper.assertTrue(source.getNamespace().equals("thirst") && source.getPath().startsWith("chests/"),
                    "Decoded entry must retain its authored native loot source");
            helper.assertTrue(helper.getLevel().getServer().getLootData().getLootTable(source) != LootTable.EMPTY,
                    "Each authored nested loot source must actually load");
            if (id.getPath().equals("add_loot_abandoned_mineshaft")) {
                mineshaft = modifier;
                mineshaftJson = json;
            }
        }
        helper.assertTrue(seen.size() == 15 && mineshaft != null, "All fifteen actual native entries must decode");
        var original = new ResourceLocation("minecraft", "chests/abandoned_mineshaft");
        var params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        var context = new LootContext.Builder(params).withOptionalRandomSeed(72831L)
                .withQueriedLootTableId(original).create(null);
        // Decode the unchanged authored conditions with Forge's actual public codec. apply() gates
        // doApply() through their conjunction: an unchanged queried ID alone could pass on a skip.
        var conditions = IGlobalLootModifier.LOOT_CONDITIONS_CODEC.parse(JsonOps.INSTANCE, mineshaftJson.get("conditions"))
                .getOrThrow(false, message -> { throw new AssertionError("Native conditions decode failed: " + message); });
        helper.assertTrue(conditions.length == 1 && conditions[0].test(context),
                "Actual mineshaft loot_table_id condition must match before entering native nested generation");
        var nestedSource = new ResourceLocation(mineshaftJson.get("lootTable").getAsString());
        var rawContext = new LootContext.Builder(params).withOptionalRandomSeed(72831L)
                .withQueriedLootTableId(original).create(null);
        var expected = new ObjectArrayList<ItemStack>();
        context.getResolver().getLootTable(nestedSource).getRandomItemsRaw(rawContext,
                LootTable.createStackSplitter(helper.getLevel(), expected::add));
        helper.assertTrue(!expected.isEmpty(),
                "Fixed native control must produce nested loot; an empty control cannot prove entry");
        var actual = mineshaft.apply(new ObjectArrayList<>(), context);
        helper.assertTrue(actual.size() == expected.size(),
                "Actual modifier must append the nonempty native raw source result, not conditionally skip or no-op");
        for (int i = 0; i < expected.size(); i++) {
            helper.assertTrue(ItemStack.matches(actual.get(i), expected.get(i)),
                    "Actual nested item/count/tag must equal the same-seed native raw source result");
        }
        helper.assertTrue(context.getQueriedLootTableId().equals(original),
                "Actual raw nested generation must not re-enter global modifiers and replace the caller's queried table");
        var actualChest = helper.getLevel().getServer().getLootData().getLootTable(original);
        helper.assertTrue(actualChest != LootTable.EMPTY && !actualChest.getRandomItems(params, 72831L).isEmpty(),
                "Actual native chest generation with installed global modifiers must complete");
        helper.succeed();
    }
}
