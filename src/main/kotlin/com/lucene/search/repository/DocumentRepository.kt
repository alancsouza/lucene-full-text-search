package com.lucene.search.repository

import com.lucene.search.config.MongoConfig
import com.lucene.search.models.Document
import com.lucene.search.service.LuceneSearchService
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import java.time.Instant

class DocumentRepository(
    private val luceneSearchService: LuceneSearchService
) {
    private val collection = MongoConfig.database.getCollection<org.bson.Document>("documents")

    suspend fun create(document: Document): Document {
        collection.insertOne(document.toMongoDocument())
        // Index in Lucene
        luceneSearchService.indexDocument(document)
        return document
    }

    suspend fun findById(id: String): Document? {
        val objectId = try {
            ObjectId(id)
        } catch (e: IllegalArgumentException) {
            return null
        }

        val doc = collection.find(Filters.eq("_id", objectId)).firstOrNull()
        return doc?.let { Document.fromMongoDocument(it) }
    }

    suspend fun findAll(limit: Int = 100, skip: Int = 0): List<Document> {
        return collection.find()
            .skip(skip)
            .limit(limit)
            .toList()
            .map { Document.fromMongoDocument(it) }
    }

    suspend fun update(id: String, title: String?, content: String?, category: String?, tags: List<String>?, metadata: Map<String, String>?): Document? {
        val objectId = try {
            ObjectId(id)
        } catch (e: IllegalArgumentException) {
            return null
        }

        val updates = mutableListOf<org.bson.conversions.Bson>()
        title?.let { updates.add(Updates.set("title", it)) }
        content?.let { updates.add(Updates.set("content", it)) }
        category?.let { updates.add(Updates.set("category", it)) }
        tags?.let { updates.add(Updates.set("tags", it)) }
        metadata?.let { updates.add(Updates.set("metadata", it)) }
        updates.add(Updates.set("updatedAt", Instant.now()))

        if (updates.isEmpty()) return findById(id)

        collection.updateOne(
            Filters.eq("_id", objectId),
            Updates.combine(updates)
        )

        val updated = findById(id)
        // Update Lucene index
        updated?.let { luceneSearchService.updateDocument(id, it) }
        return updated
    }

    suspend fun delete(id: String): Boolean {
        val objectId = try {
            ObjectId(id)
        } catch (e: IllegalArgumentException) {
            return false
        }

        val result = collection.deleteOne(Filters.eq("_id", objectId))
        val deleted = result.deletedCount > 0

        // Delete from Lucene index
        if (deleted) {
            luceneSearchService.deleteDocument(id)
        }

        return deleted
    }

    suspend fun count(): Long {
        return collection.countDocuments()
    }

    suspend fun findByCategory(category: String, limit: Int = 100): List<Document> {
        return collection.find(Filters.eq("category", category))
            .limit(limit)
            .toList()
            .map { Document.fromMongoDocument(it) }
    }

    suspend fun findByTags(tags: List<String>, limit: Int = 100): List<Document> {
        return collection.find(Filters.all("tags", tags))
            .limit(limit)
            .toList()
            .map { Document.fromMongoDocument(it) }
    }
}
