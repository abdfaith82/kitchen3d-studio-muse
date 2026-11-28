package com.example

import com.example.core.geometry.GrowthTourEngine
import com.example.core.math.Vector3
import com.example.domain.model.KitchenElement
import com.example.domain.model.KitchenElementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthTourEngineTest {

    private fun elem(id: String, type: KitchenElementType, x: Float, z: Float) =
        KitchenElement(id = id, name = id, type = type, position = Vector3(x, 0f, z))

    @Test
    fun `stages follow tall base wall appliances`() {
        assertEquals(0, GrowthTourEngine.stageOf(KitchenElementType.TALL_FRIDGE_HOUSING))
        assertEquals(0, GrowthTourEngine.stageOf(KitchenElementType.TALL_PANTRY))
        assertEquals(1, GrowthTourEngine.stageOf(KitchenElementType.BASE_SINK_UNIT))
        assertEquals(1, GrowthTourEngine.stageOf(KitchenElementType.BASE_CORNER_L))
        assertEquals(2, GrowthTourEngine.stageOf(KitchenElementType.WALL_SINGLE_DOOR))
        assertEquals(2, GrowthTourEngine.stageOf(KitchenElementType.WALL_CORNER_L))
        assertEquals(3, GrowthTourEngine.stageOf(KitchenElementType.COOKTOP_INDUCTION))
        assertEquals(3, GrowthTourEngine.stageOf(KitchenElementType.RANGE_HOOD_BOX))
        assertEquals(3, GrowthTourEngine.stageOf(KitchenElementType.DISHWASHER_INTEGRATED))
    }

    @Test
    fun `build order groups by stage then left to right`() {
        val elements = listOf(
            elem("wall-a", KitchenElementType.WALL_SINGLE_DOOR, 300f, 0f),
            elem("base-b", KitchenElementType.BASE_SINGLE_DOOR, 1200f, 0f),
            elem("tall-a", KitchenElementType.TALL_PANTRY, 0f, 0f),
            elem("cooktop", KitchenElementType.COOKTOP_INDUCTION, 900f, 0f),
            elem("base-a", KitchenElementType.BASE_SINK_UNIT, 300f, 0f)
        )
        val ordered = GrowthTourEngine.buildOrder(elements).map { it.id }
        assertEquals(
            listOf("tall-a", "base-a", "base-b", "wall-a", "cooktop"),
            ordered
        )
    }

    @Test
    fun `build order keeps every element exactly once`() {
        val elements = listOf(
            elem("a", KitchenElementType.BASE_DOUBLE_DOOR, 0f, 0f),
            elem("b", KitchenElementType.WALL_SINGLE_DOOR, 0f, 0f),
            elem("c", KitchenElementType.TALL_FRIDGE_HOUSING, 0f, 0f)
        )
        val ordered = GrowthTourEngine.buildOrder(elements)
        assertEquals(3, ordered.size)
        assertEquals(elements.map { it.id }.toSet(), ordered.map { it.id }.toSet())
        assertTrue(GrowthTourEngine.buildOrder(emptyList()).isEmpty())
    }

    @Test
    fun `stage labels are non-empty Arabic`() {
        for (stage in 0..3) {
            val label = GrowthTourEngine.stageLabelAr(stage)
            assertTrue(label.isNotBlank())
        }
    }
}
