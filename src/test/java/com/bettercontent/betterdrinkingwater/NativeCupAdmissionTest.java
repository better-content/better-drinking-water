package com.bettercontent.betterdrinkingwater;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeCupAdmissionTest {
    @Test void prepaymentVetoStopsManagedCupButNotUnclaimedNativeCup() {
        assertFalse(NativeCupAdmission.nativeUseAllowed(true, false));
        assertTrue(NativeCupAdmission.nativeUseAllowed(true, true));
        assertTrue(NativeCupAdmission.nativeUseAllowed(false, false));
    }
}
