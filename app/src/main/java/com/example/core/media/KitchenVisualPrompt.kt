package com.example.core.media

import com.example.core.ai.KitchenAiStylePreference
import com.example.domain.model.RoomLayoutShape
import com.example.domain.model.RoomStructure

/**
 * v1.9 — the text half of the kitchen media chain (text → image → video).
 *
 * The AI designer already speaks English to the models via
 * [KitchenAiStylePreference.promptGuidance]; this builder turns the current
 * room + style into one photorealistic English image prompt that any image
 * model understands — Pollinations (in-app, keyless), Raphael, Leonardo,
 * Bing Image Creator or Meta AI (WebView tabs). Pure function, unit-tested.
 */
object KitchenVisualPrompt {

    private fun layoutEnglish(shape: RoomLayoutShape): String = when (shape) {
        RoomLayoutShape.RECTANGULAR -> "rectangular"
        RoomLayoutShape.L_SHAPE -> "L-shaped"
        RoomLayoutShape.U_SHAPE -> "U-shaped"
    }

    /**
     * One English photorealistic prompt for the kitchen described by [room]
     * and [style]. Dimensions are in meters (mm / 1000, one decimal).
     */
    fun buildKitchenImagePrompt(
        room: RoomStructure,
        style: KitchenAiStylePreference,
        includeIsland: Boolean,
        customNotes: String = ""
    ): String {
        val w = "%.1f".format(room.widthMm / 1000f)
        val d = "%.1f".format(room.depthMm / 1000f)
        val h = "%.1f".format(room.heightMm / 1000f)
        val islandText = if (includeIsland) {
            "Large central kitchen island with breakfast bar and pendant lights above it."
        } else {
            "No island, open floor space in the middle of the kitchen."
        }
        val notesText = customNotes.trim().takeIf { it.isNotEmpty() }?.let { " $it" } ?: ""
        return "Photorealistic luxury kitchen interior, wide-angle architectural " +
            "photography, ${w}m wide x ${d}m deep, ${h}m ceiling, " +
            "${layoutEnglish(room.layoutShape)} layout. ${style.promptGuidance} " +
            "$islandText Warm natural daylight from a window, soft shadows, " +
            "ultra detailed, 8k, interior design magazine quality.$notesText"
    }
}
