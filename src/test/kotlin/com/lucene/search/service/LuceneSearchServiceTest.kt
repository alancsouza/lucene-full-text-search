package com.lucene.search.service

import com.lucene.search.config.LuceneConfig
import com.lucene.search.models.Document
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.bson.types.ObjectId
import kotlin.test.*
import java.nio.file.Files
import java.nio.file.Paths

class LuceneSearchServiceTest {

    private lateinit var service: LuceneSearchService
    private lateinit var tempIndexDir: java.nio.file.Path

    @BeforeTest
    fun setup() {
        service = LuceneSearchService()

        // Create temporary index directory for testing
        tempIndexDir = Files.createTempDirectory("lucene-test-")

        // Initialize LuceneConfig with test directory
        System.setProperty("lucene.index.path", tempIndexDir.toString())
        LuceneConfig.initialize()
    }

    @AfterTest
    fun cleanup() {
        LuceneConfig.close()

        // Clean up temp directory
        tempIndexDir.toFile().deleteRecursively()
    }

    @Test
    fun `test index single document`() {
        // Given
        val doc = Document(
            id = ObjectId(),
            title = "Test Document",
            content = "This is test content for indexing",
            category = "test",
            tags = listOf("sample", "test")
        )

        // When
        service.indexDocument(doc)

        // Then
        val results = service.search("test", limit = 10)
        assertEquals(1, results.totalHits)
        assertEquals("Test Document", results.results[0].title)
        assertEquals("test", results.results[0].category)
        assertEquals(2, results.results[0].tags.size)
    }

    @Test
    fun `test index multiple documents`() {
        // Given
        val docs = listOf(
            Document(
                id = ObjectId(),
                title = "Document One",
                content = "Content about Kotlin programming",
                category = "programming",
                tags = listOf("kotlin", "jvm")
            ),
            Document(
                id = ObjectId(),
                title = "Document Two",
                content = "Content about Java development",
                category = "programming",
                tags = listOf("java", "jvm")
            ),
            Document(
                id = ObjectId(),
                title = "Document Three",
                content = "Content about Python scripting",
                category = "programming",
                tags = listOf("python", "scripting")
            )
        )

        // When
        docs.forEach { service.indexDocument(it) }

        // Then
        val results = service.search("programming", limit = 10)
        assertEquals(3, results.totalHits)
    }

    @Test
    fun `test index document with empty tags`() {
        // Given
        val doc = Document(
            id = ObjectId(),
            title = "No Tags Document",
            content = "This document has no tags",
            category = "test",
            tags = emptyList()
        )

        // When
        service.indexDocument(doc)

        // Then
        val results = service.search("tags", limit = 10)
        assertEquals(1, results.totalHits)
        assertTrue(results.results[0].tags.isEmpty())
    }

    @Test
    fun `test index document without category`() {
        // Given
        val doc = Document(
            id = ObjectId(),
            title = "No Category Document",
            content = "This document has no category",
            category = null,
            tags = listOf("uncategorized")
        )

        // When
        service.indexDocument(doc)

        // Then
        val results = service.search("category", limit = 10)
        assertEquals(1, results.totalHits)
        assertNull(results.results[0].category)
    }

    @Test
    fun `test update existing document`() {
        // Given
        val originalId = ObjectId()
        val original = Document(
            id = originalId,
            title = "Original Title",
            content = "Original content",
            category = "test",
            tags = listOf("original")
        )
        service.indexDocument(original)

        // When
        val updated = Document(
            id = originalId,
            title = "Updated Title",
            content = "Updated content",
            category = "updated",
            tags = listOf("updated")
        )
        service.updateDocument(originalId.toHexString(), updated)

        // Then
        val results = service.search("Updated", limit = 10)
        assertEquals(1, results.totalHits)
        assertEquals("Updated Title", results.results[0].title)
        assertEquals("updated", results.results[0].category)

        val oldResults = service.search("Original", limit = 10)
        assertEquals(0, oldResults.totalHits)
    }

    @Test
    fun `test delete document`() {
        // Given
        val docId = ObjectId()
        val doc = Document(
            id = docId,
            title = "Document to Delete",
            content = "This will be deleted",
            category = "test"
        )
        service.indexDocument(doc)

        // Verify it exists
        var results = service.search("Delete", limit = 10)
        assertEquals(1, results.totalHits)

        // When
        service.deleteDocument(docId.toHexString())

        // Then
        results = service.search("Delete", limit = 10)
        assertEquals(0, results.totalHits)
    }

    @Test
    fun `test search with category filter`() {
        // Given
        val docs = listOf(
            Document(
                id = ObjectId(),
                title = "Tech Article",
                content = "About technology",
                category = "tech"
            ),
            Document(
                id = ObjectId(),
                title = "Science Article",
                content = "About science",
                category = "science"
            ),
            Document(
                id = ObjectId(),
                title = "Tech News",
                content = "Latest technology news",
                category = "tech"
            )
        )
        docs.forEach { service.indexDocument(it) }

        // When
        val results = service.search("Article", category = "tech", limit = 10)

        // Then
        assertEquals(1, results.totalHits)
        assertEquals("Tech Article", results.results[0].title)
    }

    @Test
    fun `test search with highlighting`() {
        // Given
        val doc = Document(
            id = ObjectId(),
            title = "Lucene Search Engine",
            content = "Apache Lucene is a powerful full-text search library",
            category = "search"
        )
        service.indexDocument(doc)

        // When
        val results = service.search("Lucene", limit = 10, enableHighlighting = true)

        // Then
        assertEquals(1, results.totalHits)
        assertNotNull(results.results[0].highlightedTitle)
        assertTrue(results.results[0].highlightedTitle!!.contains("<mark>"))
    }

    @Test
    fun `test search returns results by relevance score`() {
        // Given
        val docs = listOf(
            Document(
                id = ObjectId(),
                title = "Kotlin Kotlin Kotlin",
                content = "This document mentions kotlin many times",
                category = "programming"
            ),
            Document(
                id = ObjectId(),
                title = "Java Article",
                content = "Brief mention of kotlin",
                category = "programming"
            )
        )
        docs.forEach { service.indexDocument(it) }

        // When
        val results = service.search("kotlin", limit = 10)

        // Then
        assertEquals(2, results.totalHits)
        assertTrue(results.results[0].score > results.results[1].score)
        assertTrue(results.results[0].title.contains("Kotlin"))
    }

    @Test
    fun `test search with limit parameter`() {
        // Given
        val docs = (1..5).map { i ->
            Document(
                id = ObjectId(),
                title = "Document $i",
                content = "Test content number $i",
                category = "test"
            )
        }
        docs.forEach { service.indexDocument(it) }

        // When
        val results = service.search("test", limit = 3)

        // Then
        assertEquals(5, results.totalHits)
        assertEquals(3, results.results.size)
    }

    @Test
    fun `test search with tags`() {
        // Given
        val docs = listOf(
            Document(
                id = ObjectId(),
                title = "Article 1",
                content = "Content",
                tags = listOf("kotlin", "programming", "jvm")
            ),
            Document(
                id = ObjectId(),
                title = "Article 2",
                content = "Content",
                tags = listOf("python", "programming")
            )
        )
        docs.forEach { service.indexDocument(it) }

        // When
        val results = service.search("kotlin", limit = 10)

        // Then
        assertEquals(1, results.totalHits)
        assertTrue(results.results[0].tags.contains("kotlin"))
    }

    @Test
    fun `test index document with special characters`() {
        // Given
        val doc = Document(
            id = ObjectId(),
            title = "Special & Characters: Test!",
            content = "Content with @special #characters $100 and more...",
            category = "test"
        )

        // When
        service.indexDocument(doc)

        // Then
        val results = service.search("special", limit = 10)
        assertEquals(1, results.totalHits)
        assertTrue(results.results[0].title.contains("Special"))
    }

    @Test
    fun `test search with no results`() {
        // Given
        val doc = Document(
            id = ObjectId(),
            title = "Sample Document",
            content = "Some content here",
            category = "test"
        )
        service.indexDocument(doc)

        // When
        val results = service.search("nonexistent", limit = 10)

        // Then
        assertEquals(0, results.totalHits)
        assertTrue(results.results.isEmpty())
    }

    @Test
    fun `test index large content document`() {
        // Given
        val largeContent = "This is a test. ".repeat(1000)
        val doc = Document(
            id = ObjectId(),
            title = "Large Document",
            content = largeContent,
            category = "test"
        )

        // When
        service.indexDocument(doc)

        // Then
        val results = service.search("test", limit = 10)
        assertEquals(1, results.totalHits)
        assertTrue(results.results[0].content.length > 10000)
    }

    @Test
    fun `test concurrent indexing scenario`() {
        // Given
        val docs = (1..10).map { i ->
            Document(
                id = ObjectId(),
                title = "Concurrent Doc $i",
                content = "Content for document $i",
                category = "concurrent"
            )
        }

        // When - index all documents
        docs.forEach { service.indexDocument(it) }

        // Then
        val results = service.search("concurrent", limit = 20)
        assertEquals(10, results.totalHits)
    }
}
