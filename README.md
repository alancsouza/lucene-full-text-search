# Lucene Full-Text Search API

A Kotlin-based REST API for full-text search using Apache Lucene and MongoDB.

> **Note:** This project was vibecoded with [Claude](https://claude.ai)

## Features

- Full-text search with Apache Lucene 9.7.0
- Document indexing with support for titles, content, categories, and tags
- MongoDB integration for document persistence
- Search highlighting
- Category filtering
- Relevance-based scoring
- RESTful API with Ktor

## Tech Stack

- **Language**: Kotlin 2.0.21
- **Framework**: Ktor 2.3.4
- **Search Engine**: Apache Lucene 9.7.0
- **Database**: MongoDB 7.0
- **Build Tool**: Gradle 8.10
- **JDK**: 25

## Prerequisites

- JDK 25
- Docker & Docker Compose (optional)

## Getting Started

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd lucene-full-text
   ```

2. **Run tests**
   ```bash
   ./gradlew test
   ```

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run the application**
   ```bash
   ./gradlew run
   ```

### Docker

1. **Build and run with Docker Compose**
   ```bash
   docker-compose up -d
   ```

2. **Run tests in Docker**
   ```bash
   docker build -t lucene-search .
   ```

   Or directly:
   ```bash
   docker run --rm -v $(pwd):/app -w /app gradle:8.10-jdk25 gradle test
   ```

3. **Stop services**
   ```bash
   docker-compose down
   ```

## API Endpoints

### Index a Document
```bash
POST /documents
Content-Type: application/json

{
  "title": "Document Title",
  "content": "Document content here",
  "category": "tech",
  "tags": ["kotlin", "lucene"],
  "metadata": {}
}
```

### Search Documents
```bash
POST /search
Content-Type: application/json

{
  "query": "search term",
  "category": "tech",
  "limit": 10,
  "highlight": true
}
```

### Update Document
```bash
PUT /documents/{id}
Content-Type: application/json

{
  "title": "Updated Title",
  "content": "Updated content",
  "category": "updated",
  "tags": ["updated"]
}
```

### Delete Document
```bash
DELETE /documents/{id}
```

## Project Structure

```
lucene-full-text/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/lucene/search/
│   │           ├── Application.kt
│   │           ├── config/
│   │           │   ├── LuceneConfig.kt
│   │           │   └── MongoConfig.kt
│   │           ├── models/
│   │           │   └── Document.kt
│   │           ├── repository/
│   │           │   └── DocumentRepository.kt
│   │           └── service/
│   │               └── LuceneSearchService.kt
│   └── test/
│       └── kotlin/
│           └── com/lucene/search/
│               └── service/
│                   └── LuceneSearchServiceTest.kt
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Testing

The project includes comprehensive functional tests for the Lucene search service.

### Run all tests
```bash
./gradlew test
```

### Run specific test class
```bash
./gradlew test --tests "LuceneSearchServiceTest"
```

### Run tests with detailed output
```bash
./gradlew test --info
```

### Continuous testing (watch mode)
```bash
./gradlew test --continuous
```

### Test Coverage

The test suite covers:
- Single and multiple document indexing
- Document updates and deletions
- Search with category filtering
- Search with highlighting
- Relevance scoring
- Tag-based searching
- Edge cases (empty tags, null categories, special characters, large content)
- Pagination and limits

## Configuration

### Environment Variables

- `ENVIRONMENT`: Application environment (development/production)
- `MONGODB_URI`: MongoDB connection string (default: `mongodb://localhost:27017/lucene_search`)

### Lucene Index

The Lucene index is stored in `./data/lucene-index` by default.

## Development

### For Python/Pytest Developers

If you're coming from Python and pytest:

- `pytest` → `./gradlew test`
- `@pytest.fixture` → `@BeforeTest` / `@AfterTest`
- `assert` → `assertEquals()`, `assertTrue()`, etc.
- `pytest -k "test_name"` → `./gradlew test --tests "*test_name*"`
- `pytest-watch` → `./gradlew test --continuous`

See the test files for examples.

## License

[Add your license here]

## Contributing

[Add contributing guidelines here]
