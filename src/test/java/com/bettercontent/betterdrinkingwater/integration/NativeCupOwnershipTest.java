package com.bettercontent.betterdrinkingwater.integration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeCupOwnershipTest {
    @Test void onlyCompleteClaimedPaidCupSuppressesNativeWholeFinish() {
        assertTrue(NativeCupOwnership.typedWholeCompletion(true, false, false)); // existing plants
        assertTrue(NativeCupOwnership.typedWholeCompletion(false, true, true));
        assertFalse(NativeCupOwnership.typedWholeCompletion(false, true, false)); // unsupported native cup
        assertFalse(NativeCupOwnership.typedWholeCompletion(false, false, false)); // untagged native cup
    }
}
