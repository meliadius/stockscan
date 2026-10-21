package com.example.stockscan.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "products", indices = [Index(value = ["barcode"], unique = true)])
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val name: String,
    val quantity: Int = 0,
    val minQuantity: Int = 0,
    val price: Double = 0.0,
    val location: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "movements",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("productId")]
)
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val delta: Int,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)

data class MovementWithProduct(
    val id: Long,
    val productName: String,
    val barcode: String,
    val delta: Int,
    val note: String,
    val date: Long
)
