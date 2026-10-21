package com.example.stockscan

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockscan.data.AppDatabase
import com.example.stockscan.data.MovementWithProduct
import com.example.stockscan.data.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()

    val products: StateFlow<List<Product>> = dao.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movements: StateFlow<List<MovementWithProduct>> = dao.observeMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun find(barcode: String): Product? = dao.findByBarcode(barcode)
    suspend fun findById(id: Long): Product? = dao.findById(id)

    fun adjust(productId: Long, delta: Int, note: String) {
        viewModelScope.launch { dao.applyMovement(productId, delta, note) }
    }

    /** Crée ou met à jour un produit. onResult(null) = succès, sinon message d'erreur. */
    fun save(p: Product, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                if (p.id == 0L) {
                    val id = dao.insertProduct(p.copy(quantity = 0))
                    if (p.quantity > 0) dao.applyMovement(id, p.quantity, "Stock initial")
                } else {
                    val old = dao.findById(p.id)
                    if (old != null) {
                        dao.updateProduct(p.copy(quantity = old.quantity, updatedAt = System.currentTimeMillis()))
                        val diff = p.quantity - old.quantity
                        if (diff != 0) dao.applyMovement(p.id, diff, "Correction manuelle")
                    }
                }
                onResult(null)
            } catch (e: SQLiteConstraintException) {
                onResult("Ce code-barres existe déjà dans le stock.")
            }
        }
    }

    fun delete(p: Product) {
        viewModelScope.launch { dao.deleteProduct(p) }
    }

    /** Exporte tout le stock en CSV (compatible Excel FR : séparateur ;). */
    suspend fun exportCsv(): File = withContext(Dispatchers.IO) {
        val list = dao.getAllProducts()
        val dir = File(getApplication<Application>().cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(dir, "stock_$stamp.csv")
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("\uFEFF")
            w.write("Code-barres;Nom;Quantité;Seuil;Prix;Emplacement\n")
            list.forEach {
                w.write(
                    listOf(esc(it.barcode), esc(it.name), it.quantity, it.minQuantity,
                        String.format(Locale.FRANCE, "%.2f", it.price), esc(it.location))
                        .joinToString(";") + "\n"
                )
            }
        }
        file
    }
}
