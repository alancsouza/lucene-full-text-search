package com.lucene.search.service

import com.lucene.search.config.LuceneConfig
import com.lucene.search.models.Document
import org.apache.lucene.document.Document as LuceneDocument
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.highlight.Highlighter
import org.apache.lucene.search.highlight.QueryScorer
import org.apache.lucene.search.highlight.SimpleHTMLFormatter
import org.apache.lucene.search.highlight.SimpleSpanFragmenter

class LuceneSearchService {

    fun indexDocument(document: Document) {
        val luceneDoc = LuceneDocument().apply {
            add(StringField("id", document.id.toHexString(), Field.Store.YES))
            add(TextField("title", document.title, Field.Store.YES))
            add(TextField("content", document.content, Field.Store.YES))
            document.category?.let { add(StringField("category", it, Field.Store.YES)) }
            document.tags.forEach { tag ->
                add(TextField("tags", tag, Field.Store.YES))
            }
        }

        val writer = LuceneConfig.getWriter()
        writer.addDocument(luceneDoc)
        LuceneConfig.commit()
    }

    fun updateDocument(documentId: String, document: Document) {
        val writer = LuceneConfig.getWriter()
        val term = Term("id", documentId)

        val luceneDoc = LuceneDocument().apply {
            add(StringField("id", document.id.toHexString(), Field.Store.YES))
            add(TextField("title", document.title, Field.Store.YES))
            add(TextField("content", document.content, Field.Store.YES))
            document.category?.let { add(StringField("category", it, Field.Store.YES)) }
            document.tags.forEach { tag ->
                add(TextField("tags", tag, Field.Store.YES))
            }
        }

        writer.updateDocument(term, luceneDoc)
        LuceneConfig.commit()
    }

    fun deleteDocument(documentId: String) {
        val writer = LuceneConfig.getWriter()
        val term = Term("id", documentId)
        writer.deleteDocuments(term)
        LuceneConfig.commit()
    }

    fun search(
        queryString: String,
        category: String? = null,
        limit: Int = 10,
        enableHighlighting: Boolean = false
    ): SearchResults {
        val searcher = LuceneConfig.getSearcher()

        // Build query
        val queries = mutableListOf<Query>()

        // Main text search across title and content
        val parser = MultiFieldQueryParser(
            arrayOf("title", "content", "tags"),
            LuceneConfig.analyzer,
            mapOf(
                "title" to 3.0f,    // Boost title matches
                "content" to 1.0f,
                "tags" to 2.0f       // Boost tag matches
            )
        )
        parser.defaultOperator = QueryParser.Operator.OR
        val textQuery = parser.parse(queryString)
        queries.add(textQuery)

        // Category filter
        category?.let {
            val categoryQuery = TermQuery(Term("category", it))
            queries.add(categoryQuery)
        }

        // Combine queries
        val finalQuery = if (queries.size == 1) {
            queries[0]
        } else {
            val booleanQuery = BooleanQuery.Builder()
            queries.forEachIndexed { index, query ->
                val occur = if (index == 0) BooleanClause.Occur.MUST else BooleanClause.Occur.FILTER
                booleanQuery.add(query, occur)
            }
            booleanQuery.build()
        }

        // Execute search
        val topDocs = searcher.search(finalQuery, limit)

        // Prepare highlighter if needed
        val highlighter = if (enableHighlighting) {
            val formatter = SimpleHTMLFormatter("<mark>", "</mark>")
            val scorer = QueryScorer(textQuery)
            Highlighter(formatter, scorer).apply {
                textFragmenter = SimpleSpanFragmenter(scorer, 150)
            }
        } else null

        // Build results
        val results = topDocs.scoreDocs.map { scoreDoc ->
            val doc = searcher.doc(scoreDoc.doc)
            val title = doc.get("title") ?: ""
            val content = doc.get("content") ?: ""

            SearchResult(
                id = doc.get("id"),
                title = title,
                content = content,
                category = doc.get("category"),
                tags = doc.getValues("tags")?.toList() ?: emptyList(),
                score = scoreDoc.score,
                highlightedTitle = highlighter?.let {
                    it.getBestFragment(LuceneConfig.analyzer, "title", title)
                },
                highlightedContent = highlighter?.let {
                    it.getBestFragment(LuceneConfig.analyzer, "content", content)
                }
            )
        }

        return SearchResults(
            results = results,
            totalHits = topDocs.totalHits.value,
            query = queryString
        )
    }
}

data class SearchResult(
    val id: String,
    val title: String,
    val content: String,
    val category: String?,
    val tags: List<String>,
    val score: Float,
    val highlightedTitle: String? = null,
    val highlightedContent: String? = null
)

data class SearchResults(
    val results: List<SearchResult>,
    val totalHits: Long,
    val query: String
)
