package com.bettercontent.betterdrinkingwater;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodHydrationLedgerTest {
    @Test void contractDefaultsConserveAcrossEightActualServings() {
        for (double[] pair : new double[][] {{4,5},{2,2.5},{1,1.25},{.5,.625},{.25,.3125},{0,0}}) {
            var state = FoodHydrationLedger.State.EMPTY;
            int hydration = 0, quenching = 0;
            for (int i = 1; i <= 8; i++) {
                var result = FoodHydrationLedger.complete(state, i, pair[0], pair[1], 10, 0);
                state = result.state(); hydration += result.hydration(); quenching += result.quenching();
            }
            assertEquals(pair[0]*8, hydration, 1e-9);
            assertEquals(Math.floor(pair[1]*8), quenching, 1e-9);
            assertEquals(pair[1]*8-quenching, state.quenchingRemainder(), 1e-9);
            assertEquals(0, state.hydrationRemainder(), 1e-9);
        }
    }
    @Test void reconnectRoundTripAndSameSequenceRetryCannotDuplicateFractionalCredit() {
        var first = FoodHydrationLedger.complete(FoodHydrationLedger.State.EMPTY, 1, .5, .625, 10, 0);
        var restored = new FoodHydrationLedger.State(first.state().sequence(),
                first.state().hydrationRemainder(), first.state().quenchingRemainder());
        var retry = FoodHydrationLedger.complete(restored, 1, .5, .625, 10, 0);
        assertFalse(retry.accepted()); assertEquals(restored, retry.state()); assertEquals(0, retry.hydration());
        var next = FoodHydrationLedger.complete(restored, 2, .5, .625, 10, 0);
        assertEquals(1, next.hydration()); assertEquals(1, next.quenching());
        assertEquals(.25, next.state().quenchingRemainder(), 1e-9);
        assertFalse(FoodHydrationLedger.complete(next.state(), 1, 64, 128, 0, 0).accepted());
    }
    @Test void fullMetersDiscardFoodInsteadOfMintingPaidReserveOrWholeFutureCredit() {
        var full = FoodHydrationLedger.complete(new FoodHydrationLedger.State(1,.75,.75), 2, 4, 5, 20, 20);
        assertTrue(full.accepted()); assertEquals(0,full.hydration()); assertEquals(0,full.quenching());
        assertEquals(new FoodHydrationLedger.State(2,0,0), full.state());
        var later = FoodHydrationLedger.complete(full.state(), 3, .25, .3125, 10, 0);
        assertEquals(0,later.hydration()); assertEquals(.25,later.state().hydrationRemainder());
        var almostFull = FoodHydrationLedger.complete(FoodHydrationLedger.State.EMPTY, 1, 4, 5, 19, 19);
        assertEquals(1,almostFull.hydration()); assertEquals(1,almostFull.quenching());
        assertEquals(0,almostFull.state().hydrationRemainder());
    }
    @Test void zeroThirstStillCombinesSubunitRemaindersAcrossCompletedServings() {
        var first = FoodHydrationLedger.complete(FoodHydrationLedger.State.EMPTY,1,.5,.625,0,0);
        assertEquals(.625,first.state().quenchingRemainder());
        var second = FoodHydrationLedger.complete(first.state(),2,.5,.625,0,0);
        assertEquals(1,second.hydration()); assertEquals(1,second.quenching());
        assertEquals(.25,second.state().quenchingRemainder());
    }
    @Test void lowThirstOneRetainsFractionThroughReconnectButTrueFullTwentyDoesNot() {
        var first = FoodHydrationLedger.complete(FoodHydrationLedger.State.EMPTY,1,.5,.625,1,1);
        var restored = new FoodHydrationLedger.State(first.state().sequence(),
                first.state().hydrationRemainder(),first.state().quenchingRemainder());
        assertFalse(FoodHydrationLedger.complete(restored,1,.5,.625,1,1).accepted());
        var second = FoodHydrationLedger.complete(restored,2,.5,.625,1,1);
        assertEquals(1,second.hydration()); assertEquals(1,second.quenching());
        assertEquals(.25,second.state().quenchingRemainder());
        var full = FoodHydrationLedger.complete(second.state(),3,.5,.625,20,20);
        assertEquals(0,full.hydration()); assertEquals(0,full.quenching());
        assertEquals(0,full.state().hydrationRemainder()); assertEquals(0,full.state().quenchingRemainder());
        var afterReconnect = new FoodHydrationLedger.State(full.state().sequence(),
                full.state().hydrationRemainder(),full.state().quenchingRemainder());
        var later = FoodHydrationLedger.complete(afterReconnect,4,.5,.625,1,0);
        assertEquals(0,later.hydration()); assertEquals(0,later.quenching());
    }
    @Test void quenchingRemainsBoundedByNativeThirstWithoutBankingWholeOverflow() {
        var result = FoodHydrationLedger.complete(FoodHydrationLedger.State.EMPTY,1,0,5,4,3);
        assertEquals(1,result.quenching()); assertEquals(0,result.state().quenchingRemainder());
    }
    @Test void invalidStatesAndEntitlementsFailClosed() {
        for (double invalid : new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,1})
            assertThrows(IllegalArgumentException.class, () -> new FoodHydrationLedger.State(0, invalid, 0));
        assertThrows(IllegalArgumentException.class, () -> FoodHydrationLedger.complete(FoodHydrationLedger.State.EMPTY,0,1,1,10,0));
        assertThrows(IllegalArgumentException.class, () -> FoodHydrationLedger.complete(FoodHydrationLedger.State.EMPTY,1,Double.NaN,1,10,0));
    }
    @Test void quarterAndLitreBeverageQuantitiesConserveAllFiveClassesAcrossPartialSips() {
        for (var basis : DrinkHydration.values()) {
            assertEquals(basis.hydration(250)*4,basis.hydration(1000));
            assertEquals(basis.quenching(250)*4,basis.quenching(1000));
            for (int volume : new int[]{250,1000}) {
                int h = basis.hydration(volume), q = basis.quenching(volume), totalH = 0, totalQ = 0;
                double from = 0;
                while (from < 1) {
                    var sip = WaterBottleConsumption.calculate(1,h,q,from);
                    totalH += sip.thirstRestored(); totalQ += sip.quenchedRestored();
                    assertTrue(sip.bottleCompleted() || sip.remainingFraction() > from);
                    from = sip.bottleCompleted() ? 1 : sip.remainingFraction();
                }
                assertEquals(h,totalH); assertEquals(q,totalQ);
            }
        }
    }
}
