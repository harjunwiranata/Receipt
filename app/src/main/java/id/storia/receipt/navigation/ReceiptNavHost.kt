package id.storia.receipt.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import id.storia.receipt.ui.screens.ImportScreen
import id.storia.receipt.ui.screens.InteractiveOcrScreen
import id.storia.receipt.ui.screens.PreviewScreen
import id.storia.receipt.ui.screens.ResultScreen
import kotlinx.serialization.Serializable

import id.storia.receipt.data.ReceiptCategory

@Serializable
object ImportDestination

@Serializable
data class PreviewDestination(val imageUri: String)

@Serializable
data class ResultDestination(val imageUri: String)

@Serializable
data class InteractiveOcrDestination(val imageUri: String, val initialCategoryName: String = "PLN_PREPAID")

@Composable
fun ReceiptNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    initialSharedUri: Uri? = null
) {
    val startDestination: Any = if (initialSharedUri != null) {
        PreviewDestination(Uri.encode(initialSharedUri.toString()))
    } else {
        ImportDestination
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<ImportDestination> {
            ImportScreen(
                onImageSelected = { uri ->
                    val encodedUri = Uri.encode(uri.toString())
                    navController.navigate(PreviewDestination(encodedUri))
                }
            )
        }

        composable<PreviewDestination> { backStackEntry ->
            val previewDest: PreviewDestination = backStackEntry.toRoute()

            val uri = Uri.parse(Uri.decode(previewDest.imageUri))

            PreviewScreen(
                imageUri = uri,
                onNavigateBack = { navController.popBackStack() },
                onScanOcrClicked = {
                    navController.navigate(ResultDestination(previewDest.imageUri))
                },
                onInteractiveOcrClicked = { selectedCategory ->
                    navController.navigate(
                        InteractiveOcrDestination(
                            imageUri = previewDest.imageUri,
                            initialCategoryName = selectedCategory.name
                        )
                    )
                }
            )
        }

        composable<InteractiveOcrDestination> { backStackEntry ->
            val dest: InteractiveOcrDestination = backStackEntry.toRoute()
            val uri = Uri.parse(Uri.decode(dest.imageUri))
            val initialCategory = try {
                ReceiptCategory.valueOf(dest.initialCategoryName)
            } catch (e: Exception) {
                ReceiptCategory.PLN_PREPAID
            }

            InteractiveOcrScreen(
                imageUri = uri,
                initialCategory = initialCategory,
                onNavigateBack = { navController.popBackStack() },
                onConfirmReceipt = {
                    navController.navigate(ResultDestination(dest.imageUri))
                }
            )
        }

        composable<ResultDestination> { backStackEntry ->
            val resultDest: ResultDestination = backStackEntry.toRoute()
            val uri = Uri.parse(Uri.decode(resultDest.imageUri))

            ResultScreen(
                imageUri = uri,
                onNavigateBack = { navController.popBackStack() },
                onOpenInteractiveOcr = {
                    navController.navigate(InteractiveOcrDestination(resultDest.imageUri))
                }
            )
        }
    }
}
