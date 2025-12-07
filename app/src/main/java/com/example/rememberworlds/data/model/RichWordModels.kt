package com.example.rememberworlds.data.model

import com.google.gson.annotations.SerializedName

/**
 * Root object for the new JSON format.
 */
data class RichWordRoot(
    val wordRank: Int,
    val headWord: String,
    val bookId: String,
    val content: RichWordRootContent
)

data class RichWordRootContent(
    val word: RichWordInner
)

data class RichWordInner(
    val wordHead: String,
    val wordId: String,
    val content: WordDetailContent
)

/**
 * The core rich content containing definitions, sentences, etc.
 * This is what we will likely deserialize on demand.
 */
data class WordDetailContent(
    val sentence: SentenceContainer? = null,
    val usphone: String? = null,
    val ukphone: String? = null,
    val usspeech: String? = null,
    val ukspeech: String? = null,
    val syno: SynonymContainer? = null,
    val phrase: PhraseContainer? = null,
    val relWord: RelWordContainer? = null,
    @SerializedName("trans") val translations: List<Translation>? = null
)

// --- Helper Containers ---

data class SentenceContainer(
    val sentences: List<SentencePair>,
    val desc: String?
)

data class SentencePair(
    val sContent: String,
    val sCn: String
)

data class SynonymContainer(
    val synos: List<SynonymGroup>,
    val desc: String?
)

data class SynonymGroup(
    val pos: String,
    val tran: String,
    val hwds: List<SynonymWord>
)

data class SynonymWord(
    val w: String
)

data class PhraseContainer(
    val phrases: List<PhraseItem>,
    val desc: String?
)

data class PhraseItem(
    val pContent: String,
    val pCn: String
)

data class RelWordContainer(
    val rels: List<RelWordGroup>,
    val desc: String?
)

data class RelWordGroup(
    val pos: String,
    val words: List<RelWordItem>
)

data class RelWordItem(
    val hwd: String,
    val tran: String
)

data class Translation(
    val tranCn: String,
    val descOther: String? = null,
    val pos: String,
    val descCn: String? = null,
    val tranOther: String? = null
)
