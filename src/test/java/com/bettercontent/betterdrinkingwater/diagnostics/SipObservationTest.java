package com.bettercontent.betterdrinkingwater.diagnostics;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

final class SipObservationTest {
    public static void main(String[] args) { new SipObservationTest().creditAndNativeEffectAreNotConflatedAndOffIsEquivalent(); }
    @Test void creditAndNativeEffectAreNotConflatedAndOffIsEquivalent() {
        var records = new ArrayList<String>();
        boolean[] enabled = {false};
        var log = new PlaytestLog("better_drinking_water", new PlaytestLog.Sink() {
            public boolean enabled(String category, String level) { return enabled[0]; }
            public void write(String category, String level, String message) { records.add(message); }
        }, () -> 0L, new Properties());
        var context = PlaytestLog.Context.server("minecraft:overworld", 10);
        SipObservation.emit(log, context, 42, "sip.outcome", "released", "thirst_satisfied", 1, 2, 1);
        assertTrue(records.isEmpty());
        enabled[0] = true;
        SipObservation.emit(log, context, 42, "reserve.purchase", "admitted", "paid_portion_returned", 0, 0, 1);
        SipObservation.emit(log, context, 42, "sip.outcome", "rejected", "beverage_veto", 0, 0, 1);
        SipObservation.emit(log, context, 42, "sip.outcome", "released", "thirst_satisfied", 1, 2, 0);
        assertEquals(3, records.size());
        assertTrue(records.stream().allMatch(s -> s.contains("operation=42") && s.contains("dimension=minecraft:overworld tick=10")));
        assertTrue(records.get(0).contains("result=admitted reason=paid_portion_returned"));
        assertTrue(records.get(1).contains("result=rejected reason=beverage_veto"));
        assertTrue(records.get(2).contains("released_thirst=1 released_quenching=2 purchase_attempts=0"));
        assertTrue(records.stream().noneMatch(s -> s.contains("durable") || s.contains("subject=") || s.contains("source=") || s.contains("payload=")));
    }
}
