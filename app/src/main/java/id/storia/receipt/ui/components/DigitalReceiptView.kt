package id.storia.receipt.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.storia.receipt.data.*

@Composable
fun DigitalReceiptView(
    receipt: ParsedReceipt,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(300.dp)
            .wrapContentHeight(),
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RECEIPT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            when (receipt) {
                is PlnPrepaidReceipt -> {
                    Text("Category: PLN Prepaid")
                    Text("Token: ${receipt.tokenNumber}")
                    Text("Amount: ${receipt.totalAmount}")
                }
                is BankTransferReceipt -> {
                    Text("Category: Bank Transfer")
                    Text("Bank: ${receipt.targetBank}")
                    Text("Account: ${receipt.accountNumber}")
                    Text("Amount: ${receipt.totalAmount}")
                }
                is UnknownReceipt -> {
                    Text("Raw OCR Text:")
                    Text(receipt.rawText ?: "")
                }
                else -> {
                    Text("Amount: ${receipt.totalAmount}")
                }
            }
        }
    }
}
