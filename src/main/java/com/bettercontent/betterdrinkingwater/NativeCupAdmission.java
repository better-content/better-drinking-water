package com.bettercontent.betterdrinkingwater;

/** A managed, claimed cup obeys prepayment Check before native effects or debit. */
public final class NativeCupAdmission {
    private NativeCupAdmission() {}
    public static boolean nativeUseAllowed(boolean claimedManaged, boolean checkAllowed) {
        return !claimedManaged || checkAllowed;
    }
}
