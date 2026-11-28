package com.example.core.geometry

import com.example.domain.model.KitchenElement
import com.example.domain.model.KitchenElementType

/**
 * v1.17 — Chinese features, batch 3 (growth animation 漫游视频 / 生长动画).
 *
 * The "growth" tour shows the kitchen being built piece by piece, in the
 * order a real installer would work: tall units first (they anchor the
 * layout), then base cabinets, then wall cabinets, then appliances and
 * accessories. Pure and unit-testable; the viewport only filters by the
 * stage count the dialog drives.
 */
object GrowthTourEngine {

    /** Build stage of an element type: 0 tall, 1 base, 2 wall, 3 appliances/other. */
    fun stageOf(type: KitchenElementType): Int = when {
        type.name.startsWith("TALL") -> 0
        type.name.startsWith("BASE") -> 1
        type.name.startsWith("WALL") -> 2
        else -> 3
    }

    /** Elements in build order: stage first, then left-to-right, front-to-back. */
    fun buildOrder(elements: List<KitchenElement>): List<KitchenElement> =
        elements.sortedWith(
            compareBy(
                { stageOf(it.type) },
                { it.position.x },
                { it.position.z }
            )
        )

    /** Arabic label for a build stage, shown in the tour dialog. */
    fun stageLabelAr(stage: Int): String = when (stage) {
        0 -> "خزائن عالية وثلاجة"
        1 -> "خزائن سفلية"
        2 -> "خزائن علوية"
        else -> "أجهزة وإكسسوارات"
    }
}
