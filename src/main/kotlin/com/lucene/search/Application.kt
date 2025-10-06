package com.lucene.search

import com.lucene.search.config.LuceneConfig
import com.lucene.search.config.MongoConfig
import com.lucene.search.models.*
import com.lucene.search.repository.DocumentRepository
import com.lucene.search.service.LuceneSearchService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(val status: String, val service: String, val documentsCount: Long)

fun main() {
    MongoConfig.initialize()
    LuceneConfig.initialize()

    Runtime.getRuntime().addShutdownHook(Thread {
        LuceneConfig.close()
        MongoConfig.close()
    })

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val luceneSearchService = LuceneSearchService()
    val documentRepository = DocumentRepository(luceneSearchService)

    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        anyHost()
    }

    routing {
        get("/health") {
            val count = documentRepository.count()
            call.respond(HealthResponse("ok", "lucene-search-api", count))
        }

        get("/") {
            call.respond(mapOf("message" to "Lucene Full-Text Search API"))
        }

        // Document CRUD endpoints
        route("/documents") {
            post {
                val request = call.receive<DocumentRequest>()
                val document = Document(
                    title = request.title,
                    content = request.content,
                    category = request.category,
                    tags = request.tags,
                    metadata = request.metadata
                )
                val created = documentRepository.create(document)
                call.respond(HttpStatusCode.Created, created.toResponse())
            }

            get {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val skip = call.request.queryParameters["skip"]?.toIntOrNull() ?: 0
                val category = call.request.queryParameters["category"]

                val documents = if (category != null) {
                    documentRepository.findByCategory(category, limit)
                } else {
                    documentRepository.findAll(limit, skip)
                }

                call.respond(documents.map { it.toResponse() })
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing id parameter")
                )

                val document = documentRepository.findById(id)
                if (document != null) {
                    call.respond(document.toResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Document not found"))
                }
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing id parameter")
                )

                val request = call.receive<DocumentRequest>()
                val updated = documentRepository.update(
                    id = id,
                    title = request.title,
                    content = request.content,
                    category = request.category,
                    tags = request.tags,
                    metadata = request.metadata
                )

                if (updated != null) {
                    call.respond(updated.toResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Document not found"))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing id parameter")
                )

                val deleted = documentRepository.delete(id)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Document not found"))
                }
            }
        }

        // Search endpoints
        route("/search") {
            get {
                val query = call.request.queryParameters["q"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing query parameter 'q'")
                )

                val category = call.request.queryParameters["category"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val highlight = call.request.queryParameters["highlight"]?.toBoolean() ?: false

                val results = luceneSearchService.search(
                    queryString = query,
                    category = category,
                    limit = limit,
                    enableHighlighting = highlight
                )

                val response = SearchResponse(
                    results = results.results.map { result ->
                        SearchResultResponse(
                            id = result.id,
                            title = result.title,
                            content = result.content,
                            category = result.category,
                            tags = result.tags,
                            score = result.score,
                            highlightedTitle = result.highlightedTitle,
                            highlightedContent = result.highlightedContent
                        )
                    },
                    totalHits = results.totalHits,
                    query = results.query
                )

                call.respond(response)
            }

            post {
                val request = call.receive<SearchRequest>()

                val results = luceneSearchService.search(
                    queryString = request.query,
                    category = request.category,
                    limit = request.limit,
                    enableHighlighting = request.highlight
                )

                val response = SearchResponse(
                    results = results.results.map { result ->
                        SearchResultResponse(
                            id = result.id,
                            title = result.title,
                            content = result.content,
                            category = result.category,
                            tags = result.tags,
                            score = result.score,
                            highlightedTitle = result.highlightedTitle,
                            highlightedContent = result.highlightedContent
                        )
                    },
                    totalHits = results.totalHits,
                    query = results.query
                )

                call.respond(response)
            }
        }
    }
}