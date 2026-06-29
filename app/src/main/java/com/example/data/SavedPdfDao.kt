package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPdfDao {
    @Query("SELECT * FROM saved_pdfs ORDER BY timestamp DESC")
    fun getAllPdfs(): Flow<List<SavedPdf>>

    @Query("SELECT * FROM saved_pdfs WHERE uriString = :uriString LIMIT 1")
    suspend fun getPdfByUri(uriString: String): SavedPdf?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: SavedPdf)

    @Delete
    suspend fun deletePdf(pdf: SavedPdf)

    @Query("DELETE FROM saved_pdfs WHERE id = :id")
    suspend fun deletePdfById(id: Int)
}
