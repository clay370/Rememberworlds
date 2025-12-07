package com.example.rememberworlds.utils

import com.example.rememberworlds.data.db.WordDao
import com.example.rememberworlds.data.db.WordEntity
import com.example.rememberworlds.data.model.RichWordRoot
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader

object JsonImporter {

    suspend fun importJsonData(inputStream: InputStream, dao: WordDao, overrideBookId: String? = null) {
        withContext(Dispatchers.IO) {
            val reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))
            val gson = Gson()
            val batchSize = 100
            val buffer = mutableListOf<WordEntity>()

            // Handle if the file starts with an array or is just a single object (JSON Lines or just one object)
            // But typically strictly JSON should be an array or object.
            // Our logic handles a standard JSON Array of objects.
            
            try {
                // Check the first token
                if (reader.peek() == com.google.gson.stream.JsonToken.BEGIN_ARRAY) {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val wordRoot: RichWordRoot = gson.fromJson(reader, RichWordRoot::class.java)
                        buffer.add(wordRoot.toEntity(overrideBookId))

                        if (buffer.size >= batchSize) {
                            dao.insertAll(buffer)
                            buffer.clear()
                        }
                    }
                    reader.endArray()
                } else {
                    // Single object mode (for test files)
                    val wordRoot: RichWordRoot = gson.fromJson(reader, RichWordRoot::class.java)
                    buffer.add(wordRoot.toEntity(overrideBookId))
                }

                // Insert remaining
                if (buffer.isNotEmpty()) {
                    dao.insertAll(buffer)
                    buffer.clear()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Log error
            } finally {
                reader.close()
            }
        }
    }

    private fun RichWordRoot.toEntity(overrideBookId: String? = null): WordEntity {
        // Map rich data to flat entity + json blob
        val inner = this.content.word
        val details = inner.content
        
        // Simple heuristic for main translation
        val mainTrans = details.translations?.firstOrNull()?.tranCn ?: ""
        
        return WordEntity(
            id = this.wordRank,
            word = this.headWord,
            cn = mainTrans,
            audio = details.usspeech ?: "",
            bookType = overrideBookId ?: this.bookId,
            wordId = inner.wordId,
            detailedContent = details,
            
            // Preservation of existing defaults
            isLearned = false,
            isWrong = false,
            wrongCount = 0
        )
    }
}
