package com.example.rememberworlds.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Model representing a Book available for download.
 * Mapped from Firestore document.
 */
data class BookModel(
    // Firestore fields
    @get:PropertyName("bookId") val bookId: String = "",
    @get:PropertyName("name") val name: String = "",
    @get:PropertyName("category") val category: String = "",
    @get:PropertyName("version") val version: Int = 1,
    @get:PropertyName("parts") val parts: List<String> = emptyList(),
    
    // Local state
    @get:PropertyName("isDownloaded") var isDownloaded: Boolean = false,
    
    // Alias for legacy code compatibility (mapped to bookId)
    val type: String = "" 
) {
    // Helper to sync legacy type with bookId if needed, 
    // or we just use bookId everywhere.
    // For Gson/Firestore no-arg constructor is often needed, 
    // but Kotlin data classes with defaults work well with modern libraries.
}
