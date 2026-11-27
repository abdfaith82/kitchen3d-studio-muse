package com.example

import com.example.core.camera.CameraMode
import com.example.core.camera.CameraPose
import com.example.core.camera.CameraTweenEngine
import com.example.core.filament.CameraExposure
import com.example.core.math.Vector3
import com.example.domain.model.RoomStructure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.12 regression tests — the two v1.12 math fixes that used to be wrong
 * in ways only visible on the phone:
 *  - CameraExposure.displayScale (P0-3): the ray tracer multiplied by the
 *    Filament photometric exposureMultiplier (~0.0009) and rendered
 *    near-black; displayScale is 1.0 at the defaults and doubles per stop.
 *  - CameraTweenEngine.interpolatePose (P1-19): the mode used to flip at
 *    progress 0.5, teleporting the view mid-glide between orbit and walk.
 */
class CameraTweenAndExposureTest {

    // --- CameraExposure.displayScale (P0-3) ---

    @Test
    fun defaultExposureScaleIsOne() {
        assertEquals(1.0f, CameraExposure().displayScale, 0.001f)
    }

    @Test
    fun doublingIsoDoublesDisplayScale() {
        // ISO 200 is one stop faster than ISO 100 → image twice as bright.
        assertEquals(2.0f, CameraExposure(iso = 200f).displayScale, 0.001f)
    }

    @Test
    fun halvingIsoHalvesDisplayScale() {
        assertEquals(0.5f, CameraExposure(iso = 50f).displayScale, 0.001f)
    }

    @Test
    fun negativeEvCompensationBrightens() {
        // -1 EV compensation → one stop brighter.
        assertEquals(2.0f, CameraExposure(evCompensation = -1f).displayScale, 0.001f)
    }

    @Test
    fun photometricMultiplierIsStillTinyAtDefaults() {
        // Guard: the old (wrong) scale is still tiny — if anyone wires it back
        // into the ray tracer, renders go near-black again.
        assertTrue(CameraExposure().exposureMultiplier < 0.01f)
    }

    // --- CameraTweenEngine.interpolatePose (P1-19) ---

    @Test
    fun interpolateKeepsStartModeUntilTweenFinishes() {
        val start = CameraPose(mode = CameraMode.ORBIT)
        val target = CameraPose(mode = CameraMode.FIRST_PERSON_WALK)
        assertEquals(
            CameraMode.ORBIT,
            CameraTweenEngine.interpolatePose(start, target, 0.5f).mode
        )
        assertEquals(
            CameraMode.ORBIT,
            CameraTweenEngine.interpolatePose(start, target, 0.99f).mode
        )
        assertEquals(
            CameraMode.FIRST_PERSON_WALK,
            CameraTweenEngine.interpolatePose(start, target, 1.0f).mode
        )
    }

    @Test
    fun orbitEyeIsOutsideRoomAndAboveFloor() {
        val room = RoomStructure(widthMm = 4200f, depthMm = 3600f, heightMm = 2700f)
        val eye = CameraTweenEngine.orbitEyeFor(
            room = room,
            yawDegrees = 35f,
            pitchDegrees = 30f,
            zoomFactor = 1.0f,
            panOffset = Vector3.ZERO
        )
        // Eye height never drops below the 180 mm floor.
        assertTrue(eye.y >= 180f)
        // At zoom 1 the orbit distance is max(room) * 1.38 — the eye sits
        // outside the room, not inside it.
        val center = Vector3(room.widthMm * 0.5f, room.heightMm * 0.34f, room.depthMm * 0.5f)
        val distance = kotlin.math.sqrt(
            (eye.x - center.x) * (eye.x - center.x) +
                (eye.y - center.y) * (eye.y - center.y) +
                (eye.z - center.z) * (eye.z - center.z)
        )
        assertTrue(distance > 4200f)
    }
}
