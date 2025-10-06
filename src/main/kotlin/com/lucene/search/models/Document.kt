package com.lucene.search.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.time.Instant

@Serializable
data class Document(
    @Contextual
    val id: ObjectId = ObjectId(),
    val title: String,
    val content: String,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    @Contextual
    val createdAt: Instant = Instant.now(),
    @Contextual
    val updatedAt: Instant = Instant.now()
) {
    fun toMongoDocument(): org.bson.Document {
        return org.bson.Document().apply {
            put("_id", id)
            put("title", title)
            put("content", content)
            category?.let { put("category", it) }
            put("tags", tags)
            put("metadata", metadata)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
        }
    }

    companion object {
        fun fromMongoDocument(doc: org.bson.Document): Document {
            return Document(
                id = doc.getObjectId("_id"),
                title = doc.getString("title"),
                content = doc.getString("content"),
                category = doc.getString("category"),
                tags = doc.getList("tags", String::class.java) ?: emptyList(),
                metadata = doc.get("metadata", Map::class.java) as? Map<String, String> ?: emptyMap(),
                createdAt = doc.get("createdAt", Instant::class.java) ?: Instant.now(),
                updatedAt = doc.get("updatedAt", Instant::class.java) ?: Instant.now()
            )
        }
    }
}

@Serializable
data class DocumentRequest(
    val title: String,
    val content: String,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class DocumentResponse(
    val id: String,
    val title: String,
    val content: String,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: String,
    val updatedAt: String
)

fun Document.toResponse(): DocumentResponse {
    return DocumentResponse(
        id = id.toHexString(),
        title = title,
        content = content,
        category = category,
        tags = tags,
        metadata = metadata,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString()
    )
}

@Serializable
data class SearchRequest(
    val query: String,
    val category: String? = null,
    val limit: Int = 10,
    val highlight: Boolean = false
)

@Serializable
data class SearchResultResponse(
    val id: String,
    val title: String,
    val content: String,
    val category: String?,
    val tags: List<String>,
    val score: Float,
    val highlightedTitle: String? = null,
    val highlightedContent: String? = null
)

@Serializable
data class SearchResponse(
    val results: List<SearchResultResponse>,
    val totalHits: Long,
    val query: String
)
