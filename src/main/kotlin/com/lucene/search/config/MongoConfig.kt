package com.lucene.search.config

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase

object MongoConfig {
    private lateinit var client: MongoClient
    lateinit var database: MongoDatabase
        private set

    fun initialize(connectionString: String = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017/lucene_search") {
        client = MongoClient.create(connectionString)
        val dbName = connectionString.substringAfterLast("/").substringBefore("?")
        database = client.getDatabase(dbName)
    }

    fun close() {
        if (::client.isInitialized) {
            client.close()
        }
    }
}
