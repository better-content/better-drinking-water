package com.bettercontent.betterdrinkingwater.diagnostics;

/** Scalar sip result; released credit is deliberately not called durable storage or native hydration. */
public final class SipObservation {
    private SipObservation() {}
    public static void emit(PlaytestLog log, PlaytestLog.Context context, long operation, String event, String result,
                            String reason, int restored, int quenched, int purchaseAttempts) {
        if (!log.enabled("transaction")) return;
        log.debug("transaction", event, context, result, reason, () -> PlaytestLog.fields(
            "operation", operation, "released_thirst", restored, "released_quenching", quenched, "purchase_attempts", purchaseAttempts));
    }
}
