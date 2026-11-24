package com.example

import com.example.core.auth.GuestAuthPolicy
import com.example.core.auth.GuestState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guest Mode v1.3 — pure-policy guarantees, no Android needed:
 * a guest enters with no account, never gets cloud sync, and always
 * sees the limits before entering.
 */
class GuestModeTest {

    @Test
    fun testGuestEntersWithNoAccount() {
        assertTrue(GuestAuthPolicy.canEnterStudio(isGuest = true, hasFirebaseUser = false))
        assertTrue(GuestAuthPolicy.canEnterStudio(isGuest = true, hasFirebaseUser = true))
        assertTrue(GuestAuthPolicy.canEnterStudio(isGuest = false, hasFirebaseUser = true))
        assertFalse(GuestAuthPolicy.canEnterStudio(isGuest = false, hasFirebaseUser = false))
    }

    @Test
    fun testGuestNeverGetsCloudSync() {
        assertFalse(GuestAuthPolicy.cloudEnabled(isGuest = true))
        assertTrue(GuestAuthPolicy.cloudEnabled(isGuest = false))
        assertFalse(GuestState(isGuest = true, guestId = "guest-1").canUseCloud)
        assertTrue(GuestState(isGuest = false, guestId = null).canUseCloud)
    }

    @Test
    fun testGuestIdentity() {
        assertEquals("ضيف", GuestAuthPolicy.guestDisplayName("guest-123"))
        assertEquals("ضيف", GuestAuthPolicy.guestDisplayName(null))
        assertEquals("مستخدم ضيف", GuestState(isGuest = true, guestId = "guest-123").displayName)
        assertTrue(GuestAuthPolicy.isValidGuestId("guest-123"))
        assertFalse(GuestAuthPolicy.isValidGuestId(null))
        assertFalse(GuestAuthPolicy.isValidGuestId(""))
        assertFalse(GuestAuthPolicy.isValidGuestId("   "))
    }

    @Test
    fun testGuestLimitsAreStatedHonestly() {
        val limits = GuestAuthPolicy.guestLimitsAr()
        assertTrue(limits.isNotBlank())
        assertTrue("Limits must say storage is local-only", limits.contains("محلي"))
        assertTrue("Limits must say there is no cloud sync", limits.contains("لا مزامنة سحابية"))
        val banner = GuestAuthPolicy.guestBannerAr()
        assertTrue(banner.contains("وضع الضيف"))
        assertTrue(banner.contains("بدون حساب"))
        assertTrue(banner.contains("بدون مزامنة سحابية"))
        val upgrade = GuestAuthPolicy.upgradeHintAr()
        assertTrue(upgrade.contains("Google"))
        assertTrue(upgrade.contains("سحابياً"))
    }
}
