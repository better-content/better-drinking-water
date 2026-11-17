package com.bettercontent.betterdrinkingwater;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import com.bettercontent.betterdrinkingwater.api.NativeDrinkProducedEvent;
import java.util.ArrayList;
import java.util.List;

/** Only attaches bounded physical facts to an actual KC assembled candidate. */
public final class NativeDrinkProduction {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private NativeDrinkProduction() {}

    public static void unsupported(String operation, RuntimeException reason) {
        LOG.warn("Native KC drink {} remains native; physical facts unavailable: {}", operation, reason.toString());
    }

    /** Never abort a native recipe AFTER it has spent its inputs or set status. */
    public static ItemStack safeDecorate(String route, ServerLevel level, ItemStack actualResult, CompoundTag base,
                                         List<ItemStack> nativeSlots, CompoundTag paidSlots) {
        try { return decorate(route, level, actualResult, base, nativeSlots, paidSlots); }
        catch (RuntimeException unsupported) { unsupported("result " + route, unsupported); return actualResult; }
    }

    public static CompoundTag base(String fluid, int millibuckets, ItemStack actualPaidSource, String kind) {
        CompoundTag data = new CompoundTag(); data.putInt("v", 1);
        data.putString("fluid", fluid == null ? "" : fluid);
        data.putString("kind", kind);
        data.putInt("millibuckets", millibuckets);
        if (actualPaidSource != null && !actualPaidSource.isEmpty())
            NativeDrinkFacts.validateStack(actualPaidSource); // before source.copyWithCount
        ItemStack paid = actualPaidSource == null || actualPaidSource.isEmpty()
                ? ItemStack.EMPTY : actualPaidSource.copyWithCount(1);
        data.put("source", paid.save(new CompoundTag()));
        return NativeDrinkFacts.bounded(data);
    }

    public static ItemStack decorate(String route, ServerLevel level, ItemStack actualResult, CompoundTag base,
                                     List<ItemStack> nativeSlots, CompoundTag paidSlots) {
        if (!NativeDrinkFacts.isCup(actualResult)) return actualResult;
        String output = WaterSources.id(actualResult);
        // Native cooked result controls item/count. A missing source fact is not an
        // exception after somebody already paid; it stays an explicit unresolved receipt.
        CompoundTag known = NativeDrinkFacts.bounded(base);
        String fluid = known.contains("fluid", Tag.TAG_STRING) ? known.getString("fluid") : "";
        int volume = known.contains("millibuckets", Tag.TAG_INT) ? known.getInt("millibuckets") : 0;
        ItemStack source = known.contains("source", Tag.TAG_COMPOUND)
                ? ItemStack.of(known.getCompound("source")) : ItemStack.EMPTY;
        if (source.isEmpty()) volume = 0;
        List<NativeDrinkFacts.PaidInput> paid = new ArrayList<>();
        for (int slot = 0; slot < nativeSlots.size(); slot++) {
            ItemStack current = nativeSlots.get(slot);
            if (current.isEmpty()) continue;
            if (current.getCount() != 1) continue; // Not an observed one-unit payment.
            String key = Integer.toString(slot);
            if (!paidSlots.contains(key, Tag.TAG_COMPOUND)) continue;
            ItemStack original = ItemStack.of(paidSlots.getCompound(key));
            if (!original.isEmpty() && original.getCount() == 1 && original.is(current.getItem()))
                paid.add(new NativeDrinkFacts.PaidInput(slot, original));
        }
        var receipt = new NativeDrinkFacts.Receipt(route, output, actualResult.getCount(), fluid, volume, source, paid);
        if (!receipt.complete()) LOG.warn("Native KC drink {} has incomplete actual paid sources; native cup remains usable without invented Diet/Spice profile", output);
        ItemStack credited = NativeDrinkFacts.decorate(actualResult, receipt);
        if (receipt.complete()) {
            List<ItemStack> actualPaid = new ArrayList<>();
            actualPaid.add(source.copyWithCount(1));
            for (var input : paid) actualPaid.add(input.original().copyWithCount(1));
            MinecraftForge.EVENT_BUS.post(new NativeDrinkProducedEvent(level, credited, actualPaid));
        }
        return credited;
    }
}
