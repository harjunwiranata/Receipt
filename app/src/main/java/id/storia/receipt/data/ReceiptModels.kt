package id.storia.receipt.data

import kotlinx.serialization.Serializable

@Serializable
enum class ReceiptCategory {
    PLN_PREPAID,
    BANK_TRANSFER,
    SALE,
    EWALLET_TOPUP,
    BILL_PAYMENT,
    PHONE_CREDIT,
    UNKNOWN
}

@Serializable
sealed class ParsedReceipt {
    abstract val date: String
    abstract val totalAmount: String
    abstract val baseNominal: String
    abstract val category: ReceiptCategory
    abstract val trxId: String?
}

@Serializable
data class AnalysisResult(
    val receipt: ParsedReceipt,
    val candidates: List<String>,
    val processingTimeMs: Long? = null
)

@Serializable
data class PlnPrepaidReceipt(
    override val date: String,
    override val totalAmount: String,
    override val baseNominal: String = "",
    val tokenNumber: String,
    val customerId: String,
    val meterNo: String? = null,
    val customerName: String? = null,
    val tariff: String? = null,
    val power: String? = null,
    val kwh: String? = null,
    val adminFee: String? = null,
    val denom: String? = null,
    override val category: ReceiptCategory = ReceiptCategory.PLN_PREPAID,
    override val trxId: String? = null
) : ParsedReceipt()

@Serializable
data class BankTransferReceipt(
    override val date: String,
    override val totalAmount: String,
    override val baseNominal: String = "",
    val targetBank: String,
    val accountNumber: String,
    val recipientName: String? = null,
    val referenceNumber: String? = null,
    val adminFee: String? = null,
    override val category: ReceiptCategory = ReceiptCategory.BANK_TRANSFER,
    override val trxId: String? = null
) : ParsedReceipt()

@Serializable
data class SaleReceipt(
    override val date: String,
    override val totalAmount: String,
    override val baseNominal: String = "",
    val storeName: String,
    val items: List<SaleItem>,
    override val category: ReceiptCategory = ReceiptCategory.SALE,
    override val trxId: String? = null
) : ParsedReceipt()

@Serializable
data class SaleItem(
    val name: String,
    val quantity: String,
    val price: String
)

@Serializable
data class EWalletTopupReceipt(
    override val date: String,
    override val totalAmount: String,
    override val baseNominal: String = "",
    val walletName: String,
    val targetNumber: String,
    val recipientName: String? = null,
    val adminFee: String? = null,
    override val category: ReceiptCategory = ReceiptCategory.EWALLET_TOPUP,
    override val trxId: String? = null
) : ParsedReceipt()

@Serializable
data class BillPaymentReceipt(
    override val date: String,
    override val totalAmount: String,
    override val baseNominal: String = "",
    val billType: String,
    val customerId: String,
    val customerName: String? = null,
    val period: String? = null,
    val adminFee: String? = null,
    override val category: ReceiptCategory = ReceiptCategory.BILL_PAYMENT,
    override val trxId: String? = null
) : ParsedReceipt()

@Serializable
data class PhoneCreditReceipt(
    override val date: String,
    override val totalAmount: String,
    override val baseNominal: String = "",
    val provider: String,
    val phoneNumber: String,
    val denom: String? = null,
    val adminFee: String? = null,
    override val category: ReceiptCategory = ReceiptCategory.PHONE_CREDIT,
    override val trxId: String? = null
) : ParsedReceipt()

@Serializable
data class UnknownReceipt(
    override val date: String = "",
    override val totalAmount: String = "",
    override val baseNominal: String = "",
    val rawText: String? = null,
    override val category: ReceiptCategory = ReceiptCategory.UNKNOWN,
    override val trxId: String? = null
) : ParsedReceipt()
