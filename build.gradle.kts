plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
}

group = "com.lucene.search"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("io.ktor:ktor-server-cors:2.3.4")
    implementation("io.ktor:ktor-server-call-logging:2.3.4")
    
    // Lucene dependencies
    implementation("org.apache.lucene:lucene-core:9.7.0")
    implementation("org.apache.lucene:lucene-queryparser:9.7.0")
    implementation("org.apache.lucene:lucene-analysis-common:9.7.0")
    implementation("org.apache.lucene:lucene-highlighter:9.7.0")

    // MongoDB dependencies
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.11.0")
    implementation("org.mongodb:bson-kotlinx:4.11.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests:2.3.4")
}

application {
    mainClass.set("com.lucene.search.ApplicationKt")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}