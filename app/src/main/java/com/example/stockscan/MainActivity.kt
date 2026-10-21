package com.example.stockscan

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.stockscan.ui.EditProductScreen
import com.example.stockscan.ui.HistoryScreen
import com.example.stockscan.ui.ScannerScreen
import com.example.stockscan.ui.StockListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { StockApp() } }
    }
}

@Composable
fun StockApp(vm: StockViewModel = viewModel()) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            StockListScreen(
                vm = vm,
                onScan = { nav.navigate("scan") },
                onAdd = { nav.navigate("edit") },
                onOpen = { nav.navigate("edit?id=$it") },
                onHistory = { nav.navigate("history") }
            )
        }
        composable("scan") {
            ScannerScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onCreate = { nav.navigate("edit?barcode=${Uri.encode(it)}") },
                onEdit = { nav.navigate("edit?id=$it") }
            )
        }
        composable(
            route = "edit?id={id}&barcode={barcode}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                navArgument("barcode") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { entry ->
            EditProductScreen(
                vm = vm,
                id = entry.arguments?.getLong("id") ?: 0L,
                presetBarcode = entry.arguments?.getString("barcode") ?: "",
                onDone = { nav.popBackStack() }
            )
        }
        composable("history") { HistoryScreen(vm = vm, onBack = { nav.popBackStack() }) }
    }
}
