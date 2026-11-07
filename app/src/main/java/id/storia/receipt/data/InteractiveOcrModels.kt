package id.storia.receipt.data

import android.graphics.Rect
import kotlin.math.abs

data class OcrElement(
    val id: String,
    val text: String,
    val boundingBox: Rect
)

data class OcrLine(
    val id: String,
    val text: String,
    val boundingBox: Rect,
    val elements: List<OcrElement>
)

data class OcrRow(
    val rowId: String,
    val topY: Int,
    val lines: List<OcrLine>
)

data class InteractiveOcrData(
    val bitmapWidth: Int,
    val bitmapHeight: Int,
    val lines: List<OcrLine>,
    val fullText: String,
    val autoCategory: ReceiptCategory
) {
    /**
     * Groups recognized lines into spatial horizontal rows that mimic the receipt's visual layout.
     * Items on the same horizontal band (e.g. Label on left, Value on right) are grouped into the same [OcrRow].
     */
    fun getSpatialRows(): List<OcrRow> {
        if (lines.isEmpty()) return emptyList()

        val sorted = lines.sortedBy { it.boundingBox.centerY() }
        val rows = mutableListOf<MutableList<OcrLine>>()

        for (line in sorted) {
            val lineCy = line.boundingBox.centerY()
            val lineH = line.boundingBox.height()
            val threshold = maxOf(lineH * 0.7f, 24f)

            val matchingRow = rows.firstOrNull { row ->
                val rowAvgY = row.map { it.boundingBox.centerY() }.average()
                abs(rowAvgY - lineCy) <= threshold
            }

            if (matchingRow != null) {
                matchingRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        return rows.mapIndexed { idx, rowLines ->
            rowLines.sortBy { it.boundingBox.left }
            OcrRow(
                rowId = "row_$idx",
                topY = rowLines.minOf { it.boundingBox.top },
                lines = rowLines
            )
        }.sortedBy { it.topY }
    }
}
