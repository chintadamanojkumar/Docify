package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_pdfs")
data class SavedPdf(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val uriString: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val category: String = "Uncategorized",
    val annotationsJson: String = "", // Stores JSON of signatures/annotations
    val password: String = "",
    val rotationDegrees: Int = 0,
    val isCompressed: Boolean = false,
    val isWatermarked: Boolean = false,
    val textContent: String = "" // Stores converted/extracted Word text
)
