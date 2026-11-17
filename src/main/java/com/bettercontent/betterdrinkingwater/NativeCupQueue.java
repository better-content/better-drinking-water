package com.bettercontent.betterdrinkingwater;

import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bounded actual filled-cup FIFO. Unknown means native-only, never a fabricated paid original. */
public final class NativeCupQueue {
    private final List<Optional<CompoundTag>> saved;
    private NativeCupQueue(List<Optional<CompoundTag>> saved) {
        if (saved.size() > 4) throw new IllegalArgumentException("Native cups exceed four filled slots");
        this.saved = saved.stream().map(row -> row.map(NativeDrinkFacts::bounded)).toList();
    }
    public static NativeCupQueue unknown(int count) {
        if (count < 0 || count > 4) throw new IllegalArgumentException("Invalid native filled count");
        return new NativeCupQueue(java.util.Collections.nCopies(count, Optional.empty()));
    }
    public int size() { return saved.size(); }
    public Optional<ItemStack> first() {
        if (saved.isEmpty()) throw new IllegalStateException("No filled native cup");
        return saved.get(0).map(ItemStack::of);
    }
    public NativeCupQueue append(ItemStack actual) {
        if (actual.isEmpty() || actual.getCount() != 1 || !NativeDrinkFacts.isCup(actual))
            throw new IllegalArgumentException("Not one actual native cup");
        NativeDrinkFacts.validateStack(actual);
        var next = new ArrayList<>(saved);
        next.add(Optional.of(NativeDrinkFacts.bounded(actual.save(new CompoundTag()))));
        return new NativeCupQueue(next);
    }
    public NativeCupQueue appendUnknown() {
        var next = new ArrayList<>(saved);
        next.add(Optional.empty());
        return new NativeCupQueue(next);
    }
    public NativeCupQueue removeFirst() {
        first(); return new NativeCupQueue(saved.subList(1, saved.size()));
    }
    public CompoundTag save(String block) {
        if (!NativeDrinkFacts.isCupId(block) || saved.isEmpty()) throw new IllegalArgumentException("Invalid filled-cup block");
        CompoundTag body = new CompoundTag(); body.putInt("v", 1); body.putString("block", block);
        ListTag queue = new ListTag();
        for (var original : saved) {
            CompoundTag row = new CompoundTag();
            original.ifPresentOrElse(tag -> row.put("stack", tag.copy()), () -> row.putBoolean("unknown", true));
            queue.add(row);
        }
        body.put("queue", queue); return NativeDrinkFacts.bounded(body);
    }
    public static NativeCupQueue load(CompoundTag source, String block) {
        CompoundTag body = NativeDrinkFacts.bounded(source);
        if (body.getAllKeys().size() != 3 || !body.contains("v", Tag.TAG_INT) || body.getInt("v") != 1
                || !body.contains("block", Tag.TAG_STRING) || !block.equals(body.getString("block"))
                || !body.contains("queue", Tag.TAG_LIST)) throw new IllegalArgumentException("Corrupt native cup queue schema");
        ListTag raw = (ListTag)body.get("queue");
        ListTag rows = body.getList("queue", Tag.TAG_COMPOUND);
        if (rows.isEmpty() || rows.size() != raw.size() || rows.size() > 4)
            throw new IllegalArgumentException("Corrupt native cup queue length/type");
        List<Optional<CompoundTag>> restored = new ArrayList<>();
        for (Tag value : rows) {
            CompoundTag row = (CompoundTag)value;
            if (row.getAllKeys().size() != 1) throw new IllegalArgumentException("Corrupt native cup row");
            if (row.contains("unknown", Tag.TAG_BYTE) && row.getByte("unknown") == 1) restored.add(Optional.empty());
            else if (row.contains("stack", Tag.TAG_COMPOUND)) {
                CompoundTag stack = row.getCompound("stack");
                ItemStack item = ItemStack.of(stack);
                if (item.isEmpty() || item.getCount() != 1 || !WaterSources.id(item).equals(block))
                    throw new IllegalArgumentException("Foreign native cup original");
                restored.add(Optional.of(stack.copy()));
            } else throw new IllegalArgumentException("Corrupt native cup row type");
        }
        return new NativeCupQueue(restored);
    }
    public List<ItemStack> drops(int filled, ItemStack nativeCandidate) {
        if (filled < 0 || filled > size()) throw new IllegalArgumentException("Native loot exceeds actual filled cups");
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < filled; i++) {
            ItemStack original = saved.get(i).map(ItemStack::of).orElseGet(() -> nativeCandidate.copyWithCount(1));
            result.add(original);
        }
        return List.copyOf(result);
    }
}
