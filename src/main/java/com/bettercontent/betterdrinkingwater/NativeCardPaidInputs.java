package com.bettercontent.betterdrinkingwater;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Supplier;

/** Exact extracted KC RecipeItem inventory stacks, NEVER its card template rows. */
public final class NativeCardPaidInputs {
    private NativeCardPaidInputs() {}
    private static final ThreadLocal<Operation> CURRENT = new ThreadLocal<>();
    private static final class Operation {
        final List<ItemStack> extracted = new ArrayList<>();
        boolean invalid;
        boolean inserting;
    }
    public static <T> T scoped(Supplier<T> nativeCall) {
        Operation parent = CURRENT.get();
        Operation active = new Operation(); CURRENT.set(active);
        try { return nativeCall.get(); }
        finally { if (parent == null) CURRENT.remove(); else CURRENT.set(parent); }
    }
    /** The native IItemHandler has ALREADY returned exactly what it charged. */
    public static void extracted(ItemStack paid) {
        Operation op = CURRENT.get();
        if (op == null || paid == null || paid.isEmpty()) return;
        try {
            NativeDrinkFacts.validateStack(paid);
            if (paid.getCount() > 9 || op.extracted.size() + paid.getCount() > 9) { op.invalid = true; return; }
            for (int i = 0; i < paid.getCount(); i++) op.extracted.add(paid.copyWithCount(1));
        } catch (RuntimeException unresolved) { op.invalid = true; NativeDrinkProduction.unsupported("card actual extracted source", unresolved); }
    }
    public static void nativeInsertion(Runnable actualNativeInsertion) {
        Operation op = CURRENT.get();
        if (op == null) { actualNativeInsertion.run(); return; }
        op.inserting = true;
        try { actualNativeInsertion.run(); }
        finally { op.inserting = false; }
    }
    /** Correlate real extracted items to KC's template-filled native slots BY item identity.
     * Same-ID distinct-NBT stacks queue in payment order; no paid original is doubled. */
    public static Map<Integer, ItemStack> paidFor(List<ItemStack> nativeSlots) {
        Operation op = CURRENT.get();
        if (op == null || !op.inserting || op.invalid) return Map.of();
        Map<Item, ArrayDeque<ItemStack>> byItem = new IdentityHashMap<>();
        for (ItemStack actual : op.extracted)
            byItem.computeIfAbsent(actual.getItem(), ignored -> new ArrayDeque<>()).addLast(actual);
        Map<Integer, ItemStack> result = new HashMap<>();
        for (int slot = 0; slot < nativeSlots.size(); slot++) {
            ItemStack template = nativeSlots.get(slot);
            if (template.isEmpty()) continue;
            ArrayDeque<ItemStack> originals = byItem.get(template.getItem());
            if (template.getCount() != 1 || originals == null || originals.isEmpty()) return Map.of();
            result.put(slot, originals.removeFirst());
        }
        if (byItem.values().stream().anyMatch(q -> !q.isEmpty())) return Map.of();
        return Map.copyOf(result);
    }
}
