package com.example.rememberworlds

import com.example.rememberworlds.data.db.Converters
import com.example.rememberworlds.data.db.WordDao
import com.example.rememberworlds.data.db.WordEntity
import com.example.rememberworlds.data.models.RichWordRoot
import com.example.rememberworlds.utils.JsonImporter
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class JsonImporterTest {

    // Mock Dao
    class FakeWordDao : WordDao {
        val inserted = mutableListOf<WordEntity>()
        override suspend fun insertAll(words: List<WordEntity>) { inserted.addAll(words) }
        override suspend fun getAllWordsByBook(type: String): List<WordEntity> = emptyList()
        override suspend fun getUnlearnedWordsList(type: String): List<WordEntity> = emptyList()
        override fun getLearnedWords(type: String): Flow<List<WordEntity>> = throw NotImplementedError()
        override suspend fun getQuizWords(type: String): List<WordEntity> = emptyList()
        override suspend fun updateWord(word: WordEntity) {}
        override suspend fun markAsUnlearned(wordId: Int) {}
        override suspend fun markWordsAsLearned(bookType: String, ids: List<Int>) {}
        override suspend fun clearBook(type: String) {}
        override suspend fun deleteAll() {}
        override fun getLearnedCount(): Flow<Int> = throw NotImplementedError()
        override suspend fun resetAllProgress() {}
        override fun getTotalCount(): Flow<Int> = throw NotImplementedError()
    }

    @Test
    fun testJsonImportAndConversion() = runBlocking {
        val json = """
{
  "wordRank": 18,
  "headWord": "talk",
  "content": {
    "word": {
      "wordHead": "talk",
      "wordId": "PEPChuZhong7_2_18",
      "content": {
        "sentence": {
          "sentences": [
            {
              "sContent": "I could hear Sarah and Andy talking in the next room.",
              "sCn": "我听到萨拉和安迪在隔壁讲话。"
            }
          ],
          "desc": "例句"
        },
        "usphone": "tɔk",
        "syno": {
          "synos": [
            {
              "pos": "vt",
              "tran": "说；谈话；讨论",
              "hwds": [
                {
                  "w": "quo"
                }
              ]
            }
          ],
          "desc": "同近"
        },
        "ukphone": "tɔːk",
        "trans": [
            {
                "tranCn": "说话；谈话",
                "pos": "v"
            }
        ]
      }
    }
  },
  "bookId": "PEPChuZhong7_2"
}
        """.trimIndent()

        val inputStream = ByteArrayInputStream(json.toByteArray())
        val dao = FakeWordDao()

        JsonImporter.importJsonData(inputStream, dao)

        assertEquals(1, dao.inserted.size)
        val entity = dao.inserted[0]
        
        assertEquals("talk", entity.word)
        assertEquals("PEPChuZhong7_2_18", entity.wordId)
        assertEquals(18, entity.id)
        assertEquals("说话；谈话", entity.cn)
        
        // Convert back
        val converters = Converters()
        val jsonString = converters.fromWordDetailContent(entity.detailedContent)
        assertNotNull(jsonString)
        
        val parsedContent = converters.toWordDetailContent(jsonString)
        assertNotNull(parsedContent)
        assertEquals("tɔk", parsedContent?.usphone)
        assertEquals(1, parsedContent?.sentence?.sentences?.size)
        assertEquals("I could hear Sarah and Andy talking in the next room.", parsedContent?.sentence?.sentences?.get(0)?.sContent)
    }
}
