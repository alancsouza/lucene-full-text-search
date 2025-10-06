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
    private var directory: FSDirectory? = null
    private var writer: IndexWriter? = null
    private var reader: DirectoryReader? = null

    fun initialize() {
        // Close existing resources if already initialized
        close()

        val indexPath = System.getProperty("lucene.index.path", INDEX_PATH)
        val path = Paths.get(indexPath)
        directory = FSDirectory.open(path)

        val config = IndexWriterConfig(analyzer).apply {
            openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND
        }

        writer = IndexWriter(directory, config)
        writer?.commit() // Ensure index is created
    }

    fun getWriter(): IndexWriter {
        if (writer == null || !writer!!.isOpen) {
            initialize()
        }
        return writer!!
    }

    fun getSearcher(): IndexSearcher {
        if (directory == null || writer == null || !writer!!.isOpen) {
            initialize()
        }

        // Reopen reader if necessary to see latest changes
        if (reader == null) {
            reader = DirectoryReader.open(writer)
        } else {
            val newReader = DirectoryReader.openIfChanged(reader, writer)
            if (newReader != null) {
                reader?.close()
                reader = newReader
            }
        }

        return IndexSearcher(reader)
    }

    fun commit() {
        writer?.commit()
    }

    fun close() {
        reader?.close()
        reader = null
        writer?.close()
        writer = null
        directory?.close()
        directory = null
    }
}
