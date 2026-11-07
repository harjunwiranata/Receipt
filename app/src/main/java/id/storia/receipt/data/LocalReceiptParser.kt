package id.storia.receipt.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalReceiptParser(private val context: Context) {

    suspend fun parseReceipt(imageUri: Uri): AnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val bitmap = loadBitmap(imageUri) ?: return@withContext AnalysisResult(
            UnknownReceipt(rawText = "Failed to load image"), emptyList()
        )

        // Step 1: Run Local ML Kit OCR
        val ocrText = performOcr(bitmap)
        Log.d("LocalParser", "OCR Output:\n$ocrText")

        // Step 2: Classify Category
        val category = classifyCategory(ocrText)

        // Step 3: Extract Fields using Regex (Blank Canvas)
        val receipt = parseReceiptByCategory(ocrText, category)

        val duration = System.currentTimeMillis() - startTime
        AnalysisResult(receipt, emptyList(), duration)
    }

    private fun performOcr(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = Tasks.await(recognizer.process(image))
            result.text
        } catch (e: Exception) {
            Log.e("LocalParser", "OCR error", e)
            ""
        }
    }

    suspend fun performInteractiveOcr(imageUri: Uri): InteractiveOcrData? = withContext(Dispatchers.Default) {
        val bitmap = loadBitmap(imageUri) ?: return@withContext null
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = Tasks.await(recognizer.process(image))

            val lines = mutableListOf<OcrLine>()
            var lineIdx = 0

            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val lineBox = line.boundingBox ?: continue
                    val elements = line.elements.mapIndexedNotNull { elemIdx, elem ->
                        val elemBox = elem.boundingBox ?: return@mapIndexedNotNull null
                        OcrElement(
                            id = "elem_${lineIdx}_$elemIdx",
                            text = elem.text,
                            boundingBox = elemBox
                        )
                    }
                    lines.add(
                        OcrLine(
                            id = "line_$lineIdx",
                            text = line.text,
                            boundingBox = lineBox,
                            elements = elements
                        )
                    )
                    lineIdx++
                }
            }

            InteractiveOcrData(
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height,
                lines = lines,
                fullText = result.text,
                autoCategory = classifyCategory(result.text)
            )
        } catch (e: Exception) {
            Log.e("LocalParser", "Interactive OCR error", e)
            null
        }
    }

    private fun classifyCategory(ocrText: String): ReceiptCategory {
        val upper = ocrText.uppercase()
        return when {
            upper.contains("TOKEN") || upper.contains("METER") -> ReceiptCategory.PLN_PREPAID
            upper.contains("TRANSFER") || upper.contains("BERHASIL") -> ReceiptCategory.BANK_TRANSFER
            upper.contains("OVO") || upper.contains("DANA") || upper.contains("GOPAY") -> ReceiptCategory.EWALLET_TOPUP
            upper.contains("TAGIHAN") || upper.contains("PDAM") -> ReceiptCategory.BILL_PAYMENT
            upper.contains("PULSA") || upper.contains("TELKOMSEL") -> ReceiptCategory.PHONE_CREDIT
            else -> ReceiptCategory.SALE
        }
    }

    private fun parseReceiptByCategory(ocrText: String, category: ReceiptCategory): ParsedReceipt {
        // TODO: Add your custom Regex matching here
        return when (category) {
            ReceiptCategory.PLN_PREPAID -> PlnPrepaidReceipt(
                date = "", totalAmount = "", tokenNumber = "", customerId = ""
            )
            ReceiptCategory.BANK_TRANSFER -> BankTransferReceipt(
                date = "", totalAmount = "", targetBank = "BCA", accountNumber = ""
            )
            else -> UnknownReceipt(rawText = ocrText)
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val rawBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (rawBitmap == null) return null

            // Read EXIF orientation
            val exifStream = context.contentResolver.openInputStream(uri)
            val exif = exifStream?.use { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }
        } catch (e: Exception) {
            Log.e("LocalParser", "Error loading bitmap with EXIF", e)
            null
        }
    }
}
