package com.bettercontent.betterdrinkingwater;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeDrinkBoundsTest {
    @Test void boundsRawBinarySizeAndCollectionBeforeCopyAndSeparatesCopy() {
        CompoundTag huge = new CompoundTag();
        huge.putByteArray("paid", new byte[70000]);
        assertThrows(IllegalArgumentException.class, () -> NativeDrinkFacts.bounded(huge));
        ListTag wide = new ListTag();
        for (int i = 0; i < 4097; i++) wide.add(IntTag.valueOf(i));
        CompoundTag crowded = new CompoundTag(); crowded.put("paid", wide);
        assertThrows(IllegalArgumentException.class, () -> NativeDrinkFacts.bounded(crowded));
        CompoundTag original = new CompoundTag(); original.putString("source", "exact");
        CompoundTag detached = NativeDrinkFacts.bounded(original);
        original.putString("source", "changed");
        assertEquals("exact", detached.getString("source"));
    }
}
