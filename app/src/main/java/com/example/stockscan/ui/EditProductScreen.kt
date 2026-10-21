package com.example.stockscan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.stockscan.StockViewModel
import com.example.stockscan.data.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(vm: StockViewModel, id: Long, presetBarcode: String, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf(presetBarcode) }
    var qty by remember { mutableStateOf("0") }
    var min by remember { mutableStateOf("0") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var original by remember { mutableStateOf<Product?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L) vm.findById(id)?.let {
            original = it
            name = it.name; barcode = it.barcode; qty = it.quantity.toString()
            min = it.minQuantity.toString()
            price = if (it.price == 0.0) "" else it.price.toString()
            location = it.location
        }
    }

    val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (id == 0L) "Nouveau produit" else "Modifier le produit") },
            navigationIcon = {
                IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            },
            actions = {
                if (id != 0L) IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, "Supprimer")
                }
            }
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(name, { name = it }, label = { Text("Nom du produit *") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(barcode, { barcode = it.trim() }, label = { Text("Code-barres *") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(qty, { qty = it.filter(Char::isDigit) }, label = { Text("Quantité") },
                    singleLine = true, keyboardOptions = numeric, modifier = Modifier.weight(1f))
                OutlinedTextField(min, { min = it.filter(Char::isDigit) }, label = { Text("Seuil d'alerte") },
                    singleLine = true, keyboardOptions = numeric, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(price, { price = it }, label = { Text("Prix unitaire (€)") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(location, { location = it }, label = { Text("Emplacement (étagère, zone…)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (name.isBlank() || barcode.isBlank()) {
                        error = "Le nom et le code-barres sont obligatoires."
                        return@Button
                    }
                    val product = Product(
                        id = id,
                        barcode = barcode,
                        name = name.trim(),
                        quantity = qty.toIntOrNull() ?: 0,
                        minQuantity = min.toIntOrNull() ?: 0,
                        price = price.replace(',', '.').toDoubleOrNull() ?: 0.0,
                        location = location.trim()
                    )
                    vm.save(product) { err -> if (err == null) onDone() else error = err }
                }
            ) { Text("Enregistrer") }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer ce produit ?") },
            text = { Text("Le produit et son historique seront définitivement supprimés.") },
            confirmButton = {
                TextButton(onClick = {
                    original?.let { vm.delete(it) }
                    confirmDelete = false
                    onDone()
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } }
        )
    }
}
