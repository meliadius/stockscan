package com.example.stockscan.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.stockscan.StockViewModel
import com.example.stockscan.data.Product
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    vm: StockViewModel,
    onScan: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onHistory: () -> Unit
) {
    val products by vm.products.collectAsState()
    var query by remember { mutableStateOf("") }
    var lowOnly by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val filtered = remember(products, query, lowOnly) {
        products.filter {
            (query.isBlank() || it.name.contains(query, true) ||
                it.barcode.contains(query, true) || it.location.contains(query, true)) &&
                (!lowOnly || it.quantity <= it.minQuantity)
        }
    }
    val totalUnits = products.sumOf { it.quantity }
    val totalValue = products.sumOf { it.quantity * it.price }
    val lowCount = products.count { it.quantity <= it.minQuantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon stock") },
                actions = {
                    IconButton(onClick = onHistory) { Icon(Icons.Default.History, "Historique") }
                    IconButton(onClick = {
                        scope.launch {
                            val file = vm.exportCsv()
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(Intent.createChooser(intent, "Exporter le stock"))
                        }
                    }) { Icon(Icons.Default.Share, "Exporter en CSV") }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "Ajouter un produit") }
                ExtendedFloatingActionButton(
                    onClick = onScan,
                    icon = { Icon(Icons.Default.QrCodeScanner, null) },
                    text = { Text("Scanner") }
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Références", products.size.toString())
                    Stat("Unités", totalUnits.toString())
                    Stat("Valeur", String.format(Locale.FRANCE, "%.0f €", totalValue))
                    Stat("Stock bas", lowCount.toString())
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Nom, code-barres, emplacement") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                FilterChip(selected = lowOnly, onClick = { lowOnly = !lowOnly }, label = { Text("Stock bas uniquement") })
            }
            if (filtered.isEmpty()) {
                Text(
                    if (products.isEmpty()) "Aucun produit. Appuyez sur « Scanner » pour commencer."
                    else "Aucun résultat.",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn {
                items(filtered, key = { it.id }) { p ->
                    ProductRow(p, onClick = { onOpen(p.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ProductRow(p: Product, onClick: () -> Unit) {
    val low = p.quantity <= p.minQuantity
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(p.name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(buildString {
                append(p.barcode)
                if (p.location.isNotBlank()) append(" · ").append(p.location)
            })
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    p.quantity.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                if (low) Text("stock bas", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
