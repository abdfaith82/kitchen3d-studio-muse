package com.example

import com.example.core.export.CsvExportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportHelperTest {

    @Test
    fun `simple rows join with commas and CRLF`() {
        val csv = CsvExportHelper.buildCsv(
            listOf(
                listOf("الخزانة", "القطعة", "الكمية"),
                listOf("خزانة 60", "جنب", "2")
            )
        )
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("الخزانة,القطعة,الكمية\r\n"))
        assertTrue(csv.contains("خزانة 60,جنب,2\r\n"))
    }

    @Test
    fun `fields with commas or quotes are quoted`() {
        val csv = CsvExportHelper.buildCsv(
            listOf(listOf("a,b", "say \"hi\"", "plain"))
        )
        assertTrue(csv.contains("\"a,b\",\"say \"\"hi\"\"\",plain\r\n"))
    }

    @Test
    fun `empty input is just the BOM`() {
        assertEquals("\uFEFF", CsvExportHelper.buildCsv(emptyList()))
    }
}
