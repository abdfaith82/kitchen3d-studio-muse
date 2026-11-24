package com.example.core.auth

import android.content.Context
import java.util.UUID

/**
 * Guest Mode v1.3 — enter the studio with no account at all.
 *
 * Why this is local-only, by design:
 *  - The app's Google sign-in is already optional in practice
 *    (DEFAULT_WEB_CLIENT_ID is "not-configured" in the shipped build),
 *    but the gate still blocked everyone without a Firebase user.
 *  - A Firebase *anonymous* sign-in would look like the easy fix, but it
 *    needs Anonymous auth enabled in the Firebase console (unverifiable
 *    from here) and the Firestore rules may still reject anonymous users,
 *    so it could fail exactly when the user needs it most — offline.
 *  - Local-only guest mode has no such dependency: no network, no
 *    console setting, no account. Every design, render and export
 *    feature works; only cloud sync is off, because there is no account
 *    to sync to. Projects live in the on-device Room database either
 *    way, so a guest's local projects stay on the device after they
 *    later sign in with Google.
 *
 * [GuestAuthPolicy] is the pure, unit-testable policy; [GuestSession] is
 * the thin SharedPreferences persistence around it (guest flag + a
 * stable guest id, kept across sign-out so re-entering as guest keeps
 * the same identity).
 */
data class GuestState(
    val isGuest: Boolean = false,
    val guestId: String? = null
) {
    /** Cloud sync is only for signed-in accounts — never for a guest. */
    val canUseCloud: Boolean get() = !isGuest
    val displayName: String get() = "مستخدم ضيف"
}

object GuestAuthPolicy {
    /** The studio opens for a guest OR a signed-in Firebase user — never for neither. */
    fun canEnterStudio(isGuest: Boolean, hasFirebaseUser: Boolean): Boolean =
        isGuest || hasFirebaseUser

    fun cloudEnabled(isGuest: Boolean): Boolean = !isGuest

    fun guestDisplayName(@Suppress("UNUSED_PARAMETER") guestId: String?): String = "ضيف"

    fun isValidGuestId(id: String?): Boolean = !id.isNullOrBlank()

    /** Shown on the gate under the guest button. */
    fun guestLimitsAr(): String =
        "كل ميزات التصميم والرندر تعمل. الحفظ محلي على جهازك فقط — " +
            "لا مزامنة سحابية ولا استرجاع من جهاز آخر حتى تسجّل الدخول."

    /** Shown as a chip inside the studio while in guest mode. */
    fun guestBannerAr(): String =
        "وضع الضيف — مشاريعك محفوظة على هذا الجهاز فقط، بدون حساب وبدون مزامنة سحابية. " +
            "سجّل الدخول لاحقاً لحفظها في السحابة."

    fun upgradeHintAr(): String =
        "تسجيل الدخول بحساب Google لحفظ المشاريع سحابياً"
}

object GuestSession {
    private const val PREFS_NAME = "kitchen_studio_guest"
    private const val KEY_IS_GUEST = "is_guest"
    private const val KEY_GUEST_ID = "guest_id"

    fun load(context: Context): GuestState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return GuestState(
            isGuest = prefs.getBoolean(KEY_IS_GUEST, false),
            guestId = prefs.getString(KEY_GUEST_ID, null)
        )
    }

    /** Enter as guest: creates a stable guest id on first use, remembers the choice. */
    fun enterGuest(context: Context): GuestState {
        val existing = load(context)
        val id = existing.guestId ?: UUID.randomUUID().toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_GUEST, true)
            .putString(KEY_GUEST_ID, id)
            .apply()
        return GuestState(isGuest = true, guestId = id)
    }

    /** Leave guest mode (back to the gate). The guest id is kept, so re-entering as guest keeps the same identity. */
    fun exitGuest(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_GUEST, false)
            .apply()
    }
}
