package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.api.ServingHydration;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.*;

/** Physical KC drink receipts only. No Diet, Spice, age clock, cooked catalogue or actor. */
public final class NativeDrinkFacts {
    public static final String KEY = "better_drinking_water:native_drink_facts";
    public static final String BASE_KEY = "better_drinking_water:native_drink_base";
    public static final String INPUTS_KEY = "better_drinking_water:native_drink_inputs";
    private NativeDrinkFacts() {}

    public record PaidInput(int slot, ItemStack original) {
        public PaidInput {
            if (slot < 0 || slot >= 9 || original == null || original.isEmpty() || original.getCount() != 1)
                throw new IllegalArgumentException("Invalid actual paid native input");
            validateStack(original); // validate raw NBT BEFORE ItemStack.copy() deep-copies it.
            original = original.copy();
        }
        @Override public ItemStack original() { return original.copy(); }
    }
    public record Receipt(String route, String output, int yield, String fluid, int millibuckets,
                          ItemStack base, List<PaidInput> inputs) {
        public Receipt {
            if (route == null || output == null || fluid == null || base == null || inputs == null)
                throw new IllegalArgumentException("Missing native physical fact");
            NativeCupYield.require(route, output, yield);
            if (millibuckets < 0 || millibuckets > 1000 || inputs.size() > 9)
                throw new IllegalArgumentException("Invalid native source volume/count");
            if (!base.isEmpty() && base.getCount() != 1) throw new IllegalArgumentException("Invalid paid base count");
            if (!fluid.isEmpty() && ResourceLocation.tryParse(fluid) == null)
                throw new IllegalArgumentException("Invalid actual native fluid ID");
            if (base.isEmpty() && millibuckets != 0 || !base.isEmpty() && millibuckets != 1000)
                throw new IllegalArgumentException("Native paid base/volume disagreement");
            if (!base.isEmpty()) validateStack(base); // BEFORE deep copy
            base = base.copy();
            inputs = List.copyOf(inputs);
            Set<Integer> slots = new HashSet<>();
            for (PaidInput input : inputs) {
                if (!slots.add(input.slot()) || route.equals("teapot") && input.slot() != 0)
                    throw new IllegalArgumentException("Duplicate/foreign paid native slot");
            }
            if (inputs.size() > (route.equals("teapot") ? 1 : yield))
                throw new IllegalArgumentException("More paid inputs than actual native cups");
        }
        @Override public ItemStack base() { return base.copy(); }
        @Override public List<PaidInput> inputs() { return inputs; }
        /** Only known full 1000mB bucket bases. A 1000mB DRAIN from a larger
         * jug is not a paid whole-jug Diet/Spice or hydration entitlement. */
        public boolean knownWholeBase() {
            if (millibuckets != 1000 || base.isEmpty()) return false;
            if (route.equals("stockpot"))
                return base.is(Items.MILK_BUCKET) && fluid.equals("minecraft:milk"); // native soup-base item branch
            if (!sourceFluidMatches()) return false; // placed/portable actual KC fluid-item callback
            if (output.equals("kaleidoscope_cookery:butter_tea")) return base.is(Items.MILK_BUCKET);
            if (output.equals("kaleidoscope_cookery:mystery_tea"))
                return base.is(Items.WATER_BUCKET) || base.is(Items.MILK_BUCKET);
            return base.is(Items.WATER_BUCKET) && fluid.equals("minecraft:water");
        }
        private boolean sourceFluidMatches() {
            return base.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
                if (handler.getTanks() < 1 || handler.getFluidInTank(0).getAmount() < 1000) return false;
                ResourceLocation actual = ForgeRegistries.FLUIDS.getKey(handler.getFluidInTank(0).getFluid());
                return actual != null && actual.toString().equals(fluid);
            }).orElse(false);
        }
        public boolean complete() {
            return knownWholeBase() && inputs.size() == (route.equals("teapot") ? 1 : yield);
        }
        public Optional<ServingHydration.Profile> hydration() {
            // Hydration is Water's independently observed paid base, even when
            // a native ingredient's Diet/Spice entitlement remains unresolved.
            if (!knownWholeBase()) return Optional.empty();
            return WaterSources.nativeHydration(base).map(profile -> new ServingHydration.Profile(
                    NativeCupYield.perCup(profile.hydration(), yield),
                    NativeCupYield.perCup(profile.quenching(), yield)));
        }
    }
    public static boolean isCup(ItemStack stack) { return !stack.isEmpty() && isCupId(WaterSources.id(stack)); }
    public static boolean isCupId(String id) { return NativeCupYield.isCupId(id); }
    public static Optional<Receipt> read(ItemStack stack) {
        if (!isCup(stack) || !stack.hasTag() || !stack.getTag().contains(KEY)) return Optional.empty();
        if (!stack.getTag().contains(KEY, Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Corrupt claimed native cup receipt type");
        CompoundTag data = bounded(stack.getTag().getCompound(KEY));
        if (data.getAllKeys().size() != 9 || data.getInt("v") != 1
                || !data.contains("v", Tag.TAG_INT) || !data.contains("route", Tag.TAG_STRING)
                || !data.contains("output", Tag.TAG_STRING) || !data.contains("yield", Tag.TAG_INT)
                || !data.contains("fluid", Tag.TAG_STRING) || !data.contains("millibuckets", Tag.TAG_INT)
                || !data.contains("base", Tag.TAG_COMPOUND) || !data.contains("inputs", Tag.TAG_LIST)
                || !data.contains("unresolved", Tag.TAG_BYTE))
            throw new IllegalArgumentException("Malformed claimed native cup receipt");
        String output = data.getString("output");
        if (!output.equals(WaterSources.id(stack))) throw new IllegalArgumentException("Native cup receipt/item mismatch");
        CompoundTag baseTag = data.getCompound("base");
        ItemStack base = baseTag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(baseTag);
        if (!baseTag.isEmpty() && base.isEmpty()) throw new IllegalArgumentException("Corrupt claimed paid base identity");
        ListTag rows = data.getList("inputs", Tag.TAG_COMPOUND);
        if (rows.size() != ((ListTag)data.get("inputs")).size() || rows.size() > 9)
            throw new IllegalArgumentException("Invalid native drink input list");
        List<PaidInput> paid = new ArrayList<>();
        for (Tag raw : rows) {
            CompoundTag row = (CompoundTag) raw;
            if (row.getAllKeys().size() != 2 || !row.contains("slot", Tag.TAG_INT) || !row.contains("stack", Tag.TAG_COMPOUND))
                throw new IllegalArgumentException("Malformed paid native input");
            paid.add(new PaidInput(row.getInt("slot"), ItemStack.of(row.getCompound("stack"))));
        }
        Receipt receipt = new Receipt(data.getString("route"), output, data.getInt("yield"),
                data.getString("fluid"), data.getInt("millibuckets"), base, paid);
        if (data.getBoolean("unresolved") != !receipt.complete())
            throw new IllegalArgumentException("Claimed native unresolved/paid basis disagreement");
        return Optional.of(receipt);
    }
    public static ItemStack decorate(ItemStack nativeResult, Receipt receipt) {
        if (!isCup(nativeResult) || nativeResult.getCount() != receipt.yield()
                || !WaterSources.id(nativeResult).equals(receipt.output()))
            throw new IllegalArgumentException("Native output disagrees with actual physical receipt");
        ItemStack output = nativeResult.copy();
        if (output.hasTag() && output.getTag().contains(KEY)) throw new IllegalArgumentException("Native cup already claimed");
        CompoundTag data = new CompoundTag();
        data.putInt("v", 1); data.putString("route", receipt.route()); data.putString("output", receipt.output());
        data.putInt("yield", receipt.yield()); data.putString("fluid", receipt.fluid());
        data.putInt("millibuckets", receipt.millibuckets());
        data.putBoolean("unresolved", !receipt.complete());
        data.put("base", receipt.base().isEmpty() ? new CompoundTag() : receipt.base().save(new CompoundTag()));
        ListTag inputs = new ListTag();
        for (PaidInput input : receipt.inputs()) {
            CompoundTag row = new CompoundTag(); row.putInt("slot", input.slot());
            row.put("stack", input.original().save(new CompoundTag())); inputs.add(row);
        }
        data.put("inputs", inputs);
        output.getOrCreateTag().put(KEY, bounded(data));
        return output;
    }
    /** Bound raw tag and serialized stack BEFORE ItemStack.copy deep-copies its NBT. */
    public static void validateStack(ItemStack original) {
        if (original.hasTag()) validate(original.getTag());
        validate(original.save(new CompoundTag()));
    }
    public static CompoundTag bounded(CompoundTag source) { validate(source); return source.copy(); }
    public static void validate(CompoundTag source) {
        if (source == null) throw new IllegalArgumentException("Null physical fact");
        record Node(Tag tag, int depth) {}
        ArrayDeque<Node> pending = new ArrayDeque<>(); pending.push(new Node(source, 0));
        int nodes = 0;
        while (!pending.isEmpty()) {
            Node node = pending.pop();
            if (++nodes > 4096 || node.depth() > 24) throw new IllegalArgumentException("Native drink NBT structural bound");
            if (node.tag() instanceof CompoundTag tag) {
                if (tag.size() > 4096 || (long)pending.size() + tag.size() > 4096)
                    throw new IllegalArgumentException("Native NBT compound fanout bound");
                for (String key : tag.getAllKeys()) {
                    if (key.length() > 512) throw new IllegalArgumentException("Native NBT key bound");
                    pending.push(new Node(tag.get(key), node.depth() + 1));
                }
            } else if (node.tag() instanceof CollectionTag<?> tag) {
                if (tag.size() > 4096 || (long)pending.size() + tag.size() > 4096)
                    throw new IllegalArgumentException("Native NBT list fanout bound");
                for (int i = 0; i < tag.size(); i++) pending.push(new Node(tag.get(i), node.depth() + 1));
            }
        }
        // NBT's binary wire length, NOT unbounded whole-SNBT string rendering.
        try (var bytes = new DataOutputStream(new OutputStream() {
            int count;
            private void add(int length) throws IOException {
                if (length < 0 || (long)count + length > 65536) throw new IOException("Native NBT byte bound");
                count += length;
            }
            @Override public void write(int value) throws IOException { add(1); }
            @Override public void write(byte[] value, int offset, int length) throws IOException { add(length); }
        })) {
            NbtIo.write(source, bytes);
        } catch (IOException oversized) { throw new IllegalArgumentException("Native drink NBT byte bound", oversized); }
    }
}
