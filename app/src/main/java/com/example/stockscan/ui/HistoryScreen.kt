package com.example.stockscan.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stockscan.StockViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: StockViewModel, onBack: () -> Unit) {
    val movements by vm.movements.collectAsState()
    val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Historique des mouvements") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            }
        )
    }) { padding ->
        if (movements.isEmpty()) {
            Text("Aucun mouvement pour le moment.", Modifier.padding(padding).padding(24.dp))
        }
        LazyColumn(Modifier.padding(padding)) {
            items(movements, key = { it.id }) { m ->
                ListItem(
                    headlineContent = { Text(m.productName) },
                    supportingContent = {
                        Text("${fmt.format(Date(m.date))} · ${m.note.ifBlank { m.barcode }}")
                    },
                    trailingContent = {
                        Text(
                            (if (m.delta > 0) "+" else "") + m.delta,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (m.delta > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
