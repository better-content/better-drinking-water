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
}
