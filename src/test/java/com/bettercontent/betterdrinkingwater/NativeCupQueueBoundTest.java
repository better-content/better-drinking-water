package com.bettercontent.betterdrinkingwater;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeCupQueueBoundTest {
    @Test void prospectiveWholeFifoCannotOverflowAfterPayment() {
        var before = NativeCupQueue.unknown(3);
        assertEquals(4, before.appendUnknown().size());
        assertDoesNotThrow(() -> before.appendUnknown().save("kaleidoscope_cookery:barley_tea"));
        assertThrows(IllegalArgumentException.class, () -> NativeCupQueue.unknown(4).appendUnknown());
        assertEquals(3, before.size());
    }

    @Test void fullUnknownFifoRoundTripsWithoutFabricatingAnyPaidCup() {
        var before = NativeCupQueue.unknown(3);
        var candidate = before.appendUnknown();
        var saved = candidate.save("kaleidoscope_cookery:barley_tea");
        var restored = NativeCupQueue.load(saved, "kaleidoscope_cookery:barley_tea");
        assertEquals(4, restored.size());
        assertTrue(restored.first().isEmpty());
        assertEquals(3, restored.removeFirst().size());
        assertEquals(3, before.size()); // prepayment snapshot was not mutated
        assertThrows(IllegalArgumentException.class,
                () -> NativeCupQueue.load(saved, "kaleidoscope_cookery:mystery_tea"));
    }

    @Test void malformedClaimedFifoFailsRatherThanTurningIntoUnknownPaidFacts() {
        var saved = NativeCupQueue.unknown(1).save("kaleidoscope_cookery:barley_tea");
        saved.getList("queue", net.minecraft.nbt.Tag.TAG_COMPOUND)
                .getCompound(0).putBoolean("unknown", false);
        assertThrows(IllegalArgumentException.class,
                () -> NativeCupQueue.load(saved, "kaleidoscope_cookery:barley_tea"));
        assertThrows(IllegalArgumentException.class,
                () -> NativeCupQueue.unknown(4).appendUnknown());
    }
}
