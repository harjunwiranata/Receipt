package id.storia.receipt.ui.screens

import android.graphics.Rect
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.storia.receipt.data.*
import kotlin.math.hypot

data class FieldSpec(
    val key: String,
    val label: String,
    val isRequired: Boolean = false
)

enum class ViewMode {
    IMAGE_CANVAS,
    TEXT_PALETTE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InteractiveOcrScreen(
    imageUri: Uri,
    initialCategory: ReceiptCategory = ReceiptCategory.PLN_PREPAID,
    onNavigateBack: () -> Unit,
    onConfirmReceipt: (ParsedReceipt) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var ocrData by remember { mutableStateOf<InteractiveOcrData?>(null) }

    val selectedCategory = initialCategory

    val fields = remember(selectedCategory) { getFieldsForCategory(selectedCategory) }
    var activeFieldIndex by remember { mutableIntStateOf(0) }

    val activeFieldKey = remember(activeFieldIndex, fields) {
        fields.getOrNull(activeFieldIndex)?.key ?: ""
    }

    val fieldMap = remember { mutableStateMapOf<String, String>() }

    var highlightedBox by remember { mutableStateOf<Rect?>(null) }
    var currentViewMode by remember { mutableStateOf(ViewMode.IMAGE_CANVAS) }

    LaunchedEffect(imageUri) {
        isLoading = true
        val parser = LocalReceiptParser(context)
        val data = parser.performInteractiveOcr(imageUri)
        if (data != null) {
            ocrData = data
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Interactive OCR (${selectedCategory.name})", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap text to append to target field",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Toggle between Image Overlay View & Text Chips Palette
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = currentViewMode == ViewMode.IMAGE_CANVAS,
                            onClick = { currentViewMode = ViewMode.IMAGE_CANVAS },
                            label = { Text("Image", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )

                        FilterChip(
                            selected = currentViewMode == ViewMode.TEXT_PALETTE,
                            onClick = { currentViewMode = ViewMode.TEXT_PALETTE },
                            label = { Text("Chips Palette", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Reading receipt coordinates...")
                }
            }
        } else {
            val data = ocrData
            if (data == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Failed to process image OCR.")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // MAIN CONTENT AREA: Either Image Canvas or Tappable Text Chips Palette
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.05f))
                    ) {
                        if (currentViewMode == ViewMode.IMAGE_CANVAS) {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val containerWidth = constraints.maxWidth.toFloat()
                                val containerHeight = constraints.maxHeight.toFloat()

                                val scale = remember(containerWidth, containerHeight, data.bitmapWidth, data.bitmapHeight) {
                                    val sX = containerWidth / data.bitmapWidth.toFloat()
                                    val sY = containerHeight / data.bitmapHeight.toFloat()
                                    minOf(sX, sY)
                                }

                                val drawnWidth = data.bitmapWidth * scale
                                val drawnHeight = data.bitmapHeight * scale
                                val offsetX = (containerWidth - drawnWidth) / 2f
                                val offsetY = (containerHeight - drawnHeight) / 2f

                                // Image
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Receipt Image",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Touch & Canvas Overlay
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(data, activeFieldKey) {
                                            detectTapGestures { tapOffset ->
                                                val tapBmpX = (tapOffset.x - offsetX) / scale
                                                val tapBmpY = (tapOffset.y - offsetY) / scale

                                                val hitResult = findGenerousHitOcrText(
                                                    data = data,
                                                    bmpX = tapBmpX,
                                                    bmpY = tapBmpY
                                                )

                                                if (hitResult != null) {
                                                    highlightedBox = hitResult.boundingBox

                                                    if (activeFieldKey.isNotEmpty()) {
                                                        val currentVal = fieldMap[activeFieldKey] ?: ""
                                                        val updatedVal = if (currentVal.isBlank()) {
                                                            hitResult.text
                                                        } else {
                                                            "$currentVal ${hitResult.text}"
                                                        }
                                                        fieldMap[activeFieldKey] = updatedVal
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    val boxOverlayColor = Color(0x332196F3)
                                    val boxBorderColor = Color(0x882196F3)

                                    // Draw all line bounding boxes
                                    data.lines.forEach { line ->
                                        val box = line.boundingBox
                                        val left = offsetX + box.left * scale
                                        val top = offsetY + box.top * scale
                                        val width = box.width() * scale
                                        val height = box.height() * scale

                                        drawRect(
                                            color = boxOverlayColor,
                                            topLeft = Offset(left, top),
                                            size = Size(width, height)
                                        )
                                        drawRect(
                                            color = boxBorderColor,
                                            topLeft = Offset(left, top),
                                            size = Size(width, height),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }

                                    // Highlighted Box
                                    highlightedBox?.let { box ->
                                        val left = offsetX + box.left * scale
                                        val top = offsetY + box.top * scale
                                        val width = box.width() * scale
                                        val height = box.height() * scale

                                        drawRect(
                                            color = Color(0x77FF9800),
                                            topLeft = Offset(left, top),
                                            size = Size(width, height)
                                        )
                                        drawRect(
                                            color = Color(0xFFFF9800),
                                            topLeft = Offset(left, top),
                                            size = Size(width, height),
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                    }
                                }
                            }
                        } else {
                            // TAPPABLE TEXT CHIPS PALETTE VIEW (Mimics Visual Layout with Horizontal Alignment)
                            val spatialRows = remember(data) { data.getSpatialRows() }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Spatial Layout Palette (Tap chip to append):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                spatialRows.forEach { row ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            if (row.lines.size == 1) {
                                                val line = row.lines[0]
                                                ElevatedButton(
                                                    onClick = {
                                                        if (activeFieldKey.isNotEmpty()) {
                                                            val currentVal = fieldMap[activeFieldKey] ?: ""
                                                            val updatedVal = if (currentVal.isBlank()) line.text else "$currentVal ${line.text}"
                                                            fieldMap[activeFieldKey] = updatedVal
                                                            highlightedBox = line.boundingBox
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(text = line.text, fontWeight = FontWeight.Bold)
                                                }

                                                if (line.elements.size > 1) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        line.elements.forEach { elem ->
                                                            SuggestionChip(
                                                                onClick = {
                                                                    if (activeFieldKey.isNotEmpty()) {
                                                                        val currentVal = fieldMap[activeFieldKey] ?: ""
                                                                        val updatedVal = if (currentVal.isBlank()) elem.text else "$currentVal ${elem.text}"
                                                                        fieldMap[activeFieldKey] = updatedVal
                                                                        highlightedBox = elem.boundingBox
                                                                    }
                                                                },
                                                                label = { Text(elem.text, fontSize = 11.sp) }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Side-by-side items on the physical receipt
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    row.lines.forEachIndexed { idx, line ->
                                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                                            ElevatedButton(
                                                                onClick = {
                                                                    if (activeFieldKey.isNotEmpty()) {
                                                                        val currentVal = fieldMap[activeFieldKey] ?: ""
                                                                        val updatedVal = if (currentVal.isBlank()) line.text else "$currentVal ${line.text}"
                                                                        fieldMap[activeFieldKey] = updatedVal
                                                                        highlightedBox = line.boundingBox
                                                                    }
                                                                }
                                                            ) {
                                                                Text(text = line.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            }

                                                            if (line.elements.size > 1) {
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                FlowRow(
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    line.elements.forEach { elem ->
                                                                        SuggestionChip(
                                                                            onClick = {
                                                                                if (activeFieldKey.isNotEmpty()) {
                                                                                    val currentVal = fieldMap[activeFieldKey] ?: ""
                                                                                    val updatedVal = if (currentVal.isBlank()) elem.text else "$currentVal ${elem.text}"
                                                                                    fieldMap[activeFieldKey] = updatedVal
                                                                                    highlightedBox = elem.boundingBox
                                                                                }
                                                                            },
                                                                            label = { Text(elem.text, fontSize = 11.sp) }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if (idx < row.lines.size - 1) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Floating Active Field Target Badge
                        val currentSpec = fields.getOrNull(activeFieldIndex)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(10.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Target (${activeFieldIndex + 1}/${fields.size}): ${currentSpec?.label ?: ""}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Bottom Panel: Active Field Control & Field Overview List
                    Surface(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxWidth(),
                        tonalElevation = 3.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            val activeSpec = fields.getOrNull(activeFieldIndex)
                            val activeVal = fieldMap[activeSpec?.key ?: ""] ?: ""

                            // Active Field Display & Toolbar
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = activeSpec?.label ?: "Field",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Reset Field Button
                                            IconButton(
                                                onClick = { activeSpec?.key?.let { fieldMap.remove(it) } },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Reset Field",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Previous Field
                                            IconButton(
                                                onClick = { if (activeFieldIndex > 0) activeFieldIndex-- },
                                                enabled = activeFieldIndex > 0,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                                    contentDescription = "Previous Field",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            // Next Field
                                            IconButton(
                                                onClick = { if (activeFieldIndex < fields.size - 1) activeFieldIndex++ },
                                                enabled = activeFieldIndex < fields.size - 1,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                                    contentDescription = "Next Field",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = activeVal,
                                        onValueChange = { activeSpec?.key?.let { k -> fieldMap[k] = it } },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Tap text above to append here...", fontSize = 13.sp) },
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        singleLine = true,
                                        trailingIcon = {
                                            if (activeVal.isNotEmpty()) {
                                                IconButton(onClick = { activeSpec?.key?.let { fieldMap.remove(it) } }) {
                                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Field Overview List
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                fields.forEachIndexed { idx, spec ->
                                    val isSelected = idx == activeFieldIndex
                                    val valText = fieldMap[spec.key] ?: ""

                                    FieldCompactRow(
                                        indexStr = "${idx + 1}",
                                        spec = spec,
                                        value = valText,
                                        isActive = isSelected,
                                        onSelect = { activeFieldIndex = idx }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val receipt = buildParsedReceipt(selectedCategory, fieldMap, data.fullText)
                                    onConfirmReceipt(receipt)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Confirm & Generate Digital Receipt",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldCompactRow(
    indexStr: String,
    spec: FieldSpec,
    value: String,
    isActive: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(150), label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$indexStr.",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spec.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )

                Text(
                    text = value.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private data class HitResult(val text: String, val boundingBox: Rect)

private fun findGenerousHitOcrText(
    data: InteractiveOcrData,
    bmpX: Float,
    bmpY: Float
): HitResult? {
    val paddingPx = 50

    val hitLine = data.lines.firstOrNull { line ->
        val box = line.boundingBox
        bmpX >= (box.left - paddingPx) && bmpX <= (box.right + paddingPx) &&
                bmpY >= (box.top - paddingPx) && bmpY <= (box.bottom + paddingPx)
    }

    if (hitLine != null) {
        val hitElem = hitLine.elements.firstOrNull { elem ->
            val box = elem.boundingBox
            bmpX >= (box.left - paddingPx) && bmpX <= (box.right + paddingPx) &&
                    bmpY >= (box.top - paddingPx) && bmpY <= (box.bottom + paddingPx)
        }

        if (hitElem != null) {
            return HitResult(hitElem.text, hitElem.boundingBox)
        }
        return HitResult(hitLine.text, hitLine.boundingBox)
    }

    var minDistance = Double.MAX_VALUE
    var closest: HitResult? = null

    for (line in data.lines) {
        val cx = line.boundingBox.centerX().toFloat()
        val cy = line.boundingBox.centerY().toFloat()
        val dist = hypot((cx - bmpX).toDouble(), (cy - bmpY).toDouble())
        if (dist < minDistance && dist < 140.0) {
            minDistance = dist
            closest = HitResult(line.text, line.boundingBox)
        }

        for (elem in line.elements) {
            val ecx = elem.boundingBox.centerX().toFloat()
            val ecy = elem.boundingBox.centerY().toFloat()
            val edist = hypot((ecx - bmpX).toDouble(), (ecy - bmpY).toDouble())
            if (edist < minDistance && edist < 140.0) {
                minDistance = edist
                closest = HitResult(elem.text, elem.boundingBox)
            }
        }
    }

    return closest
}

private fun getFieldsForCategory(category: ReceiptCategory): List<FieldSpec> {
    return when (category) {
        ReceiptCategory.PLN_PREPAID -> listOf(
            FieldSpec("tokenNumber", "Token Number", isRequired = true),
            FieldSpec("customerId", "Customer ID / IDPEL", isRequired = true),
            FieldSpec("date", "Transaction Date"),
            FieldSpec("totalAmount", "Total Amount"),
            FieldSpec("customerName", "Customer Name"),
            FieldSpec("meterNo", "Meter No"),
            FieldSpec("tariff", "Tariff / Power"),
            FieldSpec("kwh", "kWh"),
            FieldSpec("adminFee", "Admin Fee")
        )
        ReceiptCategory.BANK_TRANSFER -> listOf(
            FieldSpec("targetBank", "Target Bank Name"),
            FieldSpec("accountNumber", "Account Number"),
            FieldSpec("recipientName", "Recipient Name"),
            FieldSpec("date", "Transaction Date"),
            FieldSpec("totalAmount", "Total Amount"),
            FieldSpec("referenceNumber", "Reference Number"),
            FieldSpec("adminFee", "Admin Fee")
        )
        ReceiptCategory.SALE -> listOf(
            FieldSpec("storeName", "Store Name"),
            FieldSpec("date", "Transaction Date"),
            FieldSpec("totalAmount", "Total Amount")
        )
        ReceiptCategory.EWALLET_TOPUP -> listOf(
            FieldSpec("walletName", "Wallet Name (OVO/DANA/GoPay)"),
            FieldSpec("targetNumber", "Target Phone Number"),
            FieldSpec("recipientName", "Recipient Name"),
            FieldSpec("date", "Transaction Date"),
            FieldSpec("totalAmount", "Total Amount"),
            FieldSpec("adminFee", "Admin Fee")
        )
        ReceiptCategory.BILL_PAYMENT -> listOf(
            FieldSpec("billType", "Bill Type (PDAM/Telkom)"),
            FieldSpec("customerId", "Customer ID"),
            FieldSpec("customerName", "Customer Name"),
            FieldSpec("date", "Transaction Date"),
            FieldSpec("totalAmount", "Total Amount"),
            FieldSpec("period", "Period"),
            FieldSpec("adminFee", "Admin Fee")
        )
        ReceiptCategory.PHONE_CREDIT -> listOf(
            FieldSpec("provider", "Provider Name"),
            FieldSpec("phoneNumber", "Phone Number"),
            FieldSpec("date", "Transaction Date"),
            FieldSpec("totalAmount", "Total Amount"),
            FieldSpec("denom", "Denom"),
            FieldSpec("adminFee", "Admin Fee")
        )
        ReceiptCategory.UNKNOWN -> listOf(
            FieldSpec("date", "Transaction Date"),
            FieldSpec("totalAmount", "Total Amount")
        )
    }
}

private fun buildParsedReceipt(
    category: ReceiptCategory,
    map: Map<String, String>,
    rawText: String
): ParsedReceipt {
    val date = map["date"] ?: ""
    val totalAmount = map["totalAmount"] ?: ""

    return when (category) {
        ReceiptCategory.PLN_PREPAID -> PlnPrepaidReceipt(
            date = date,
            totalAmount = totalAmount,
            tokenNumber = map["tokenNumber"] ?: "",
            customerId = map["customerId"] ?: "",
            meterNo = map["meterNo"],
            customerName = map["customerName"],
            tariff = map["tariff"],
            kwh = map["kwh"],
            adminFee = map["adminFee"]
        )
        ReceiptCategory.BANK_TRANSFER -> BankTransferReceipt(
            date = date,
            totalAmount = totalAmount,
            targetBank = map["targetBank"] ?: "Bank",
            accountNumber = map["accountNumber"] ?: "",
            recipientName = map["recipientName"],
            referenceNumber = map["referenceNumber"],
            adminFee = map["adminFee"]
        )
        ReceiptCategory.SALE -> SaleReceipt(
            date = date,
            totalAmount = totalAmount,
            storeName = map["storeName"] ?: "Store",
            items = emptyList()
        )
        ReceiptCategory.EWALLET_TOPUP -> EWalletTopupReceipt(
            date = date,
            totalAmount = totalAmount,
            walletName = map["walletName"] ?: "E-Wallet",
            targetNumber = map["targetNumber"] ?: "",
            recipientName = map["recipientName"],
            adminFee = map["adminFee"]
        )
        ReceiptCategory.BILL_PAYMENT -> BillPaymentReceipt(
            date = date,
            totalAmount = totalAmount,
            billType = map["billType"] ?: "Bill",
            customerId = map["customerId"] ?: "",
            customerName = map["customerName"],
            period = map["period"],
            adminFee = map["adminFee"]
        )
        ReceiptCategory.PHONE_CREDIT -> PhoneCreditReceipt(
            date = date,
            totalAmount = totalAmount,
            provider = map["provider"] ?: "Provider",
            phoneNumber = map["phoneNumber"] ?: "",
            denom = map["denom"],
            adminFee = map["adminFee"]
        )
        ReceiptCategory.UNKNOWN -> UnknownReceipt(
            date = date,
            totalAmount = totalAmount,
            rawText = rawText
        )
    }
}
