package id.storia.receipt

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.storia.receipt.navigation.ReceiptNavHost
import id.storia.receipt.ui.theme.ReceiptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedUri = extractSharedImageUri(intent)

        setContent {
            ReceiptTheme {
                ReceiptNavHost(initialSharedUri = sharedUri)
            }
        }
    }

    private fun extractSharedImageUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }
        } else {
            null
        }
    }
}
