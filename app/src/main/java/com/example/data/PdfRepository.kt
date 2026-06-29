package com.example.data

import kotlinx.coroutines.flow.Flow

class PdfRepository(private val savedPdfDao: SavedPdfDao) {
    val allPdfs: Flow<List<SavedPdf>> = savedPdfDao.getAllPdfs()

    suspend fun getPdfByUri(uriString: String): SavedPdf? {
        return savedPdfDao.getPdfByUri(uriString)
    }

    suspend fun insertPdf(pdf: SavedPdf) {
        savedPdfDao.insertPdf(pdf)
    }

    suspend fun deletePdf(pdf: SavedPdf) {
        savedPdfDao.deletePdf(pdf)
    }

    suspend fun deletePdfById(id: Int) {
        savedPdfDao.deletePdfById(id)
    }
}
