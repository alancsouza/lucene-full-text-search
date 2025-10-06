package com.lucene.search.config

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths

object LuceneConfig {
    private const val INDEX_PATH = "./data/lucene-index"

    val analyzer = StandardAnalyzer()
    private lateinit var directory: FSDirectory
    private lateinit var writer: IndexWriter
    private var reader: DirectoryReader? = null

    fun initialize() {
        val path = Paths.get(INDEX_PATH)
        directory = FSDirectory.open(path)

        val config = IndexWriterConfig(analyzer).apply {
            openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND
        }

        writer = IndexWriter(directory, config)
        writer.commit() // Ensure index is created
    }

    fun getWriter(): IndexWriter = writer

    fun getSearcher(): IndexSearcher {
        // Reopen reader if necessary to see latest changes
        reader = if (reader == null) {
            DirectoryReader.open(directory)
        } else {
            val newReader = DirectoryReader.openIfChanged(reader)
            newReader ?: reader
        }

        return IndexSearcher(reader)
    }

    fun commit() {
        writer.commit()
    }

    fun close() {
        reader?.close()
        writer.close()
        directory.close()
    }
}
