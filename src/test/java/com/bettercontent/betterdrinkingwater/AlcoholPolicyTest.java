package com.bettercontent.betterdrinkingwater;

import com.bettercontent.betterdrinkingwater.integration.AlcoholPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlcoholPolicyTest {
    @Test void retainedCarbInteractionsAndCaps() {
        assertEquals(0, AlcoholPolicy.reduction(0, 0));
        assertEquals(.3, AlcoholPolicy.reduction(1, 1), 1e-12);
        assertEquals(.15, AlcoholPolicy.reduction(.5, 0), 1e-12);
        assertEquals(0, AlcoholPolicy.impairment(.3, 1), 1e-12);
        assertEquals(.5, AlcoholPolicy.impairment(.65, 0), 1e-12);
        assertEquals(1, AlcoholPolicy.impairment(1, 1), 1e-12);
        assertEquals(.8 - 8.0 / 24000, AlcoholPolicy.clearance(.8, 1), 1e-12);
        assertEquals(.8 - 1.0 / 24000, AlcoholPolicy.clearance(.8, 0), 1e-12);
        assertTrue(AlcoholPolicy.blackout(1));
        assertFalse(AlcoholPolicy.blackout(.999));
        assertTrue(AlcoholPolicy.sober(.85));
        assertFalse(AlcoholPolicy.sober(.851));
    }
}
