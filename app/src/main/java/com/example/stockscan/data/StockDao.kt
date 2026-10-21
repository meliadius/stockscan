package com.example.stockscan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StockDao {

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    abstract fun observeProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    abstract suspend fun getAllProducts(): List<Product>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    abstract suspend fun findByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    abstract suspend fun findById(id: Long): Product?

    @Insert
    abstract suspend fun insertProduct(product: Product): Long

    @Update
    abstract suspend fun updateProduct(product: Product)

    @Delete
    abstract suspend fun deleteProduct(product: Product)

    @Insert
    abstract suspend fun insertMovement(movement: Movement)

    @Query(
        """SELECT m.id AS id, p.name AS productName, p.barcode AS barcode,
                  m.delta AS delta, m.note AS note, m.date AS date
           FROM movements m JOIN products p ON p.id = m.productId
           ORDER BY m.date DESC LIMIT 500"""
    )
    abstract fun observeMovements(): Flow<List<MovementWithProduct>>

    /** Applique une variation de stock (jamais sous 0) et enregistre le mouvement. */
    @Transaction
    open suspend fun applyMovement(productId: Long, delta: Int, note: String) {
        val p = findById(productId) ?: return
        val newQty = (p.quantity + delta).coerceAtLeast(0)
        val real = newQty - p.quantity
        if (real == 0) return
        val now = System.currentTimeMillis()
        updateProduct(p.copy(quantity = newQty, updatedAt = now))
        insertMovement(Movement(productId = productId, delta = real, note = note, date = now))
    }
}
