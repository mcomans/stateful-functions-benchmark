import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val logback_version = "1.2.3"
val ktor_version = "1.5.4"
val kotlin_version = "1.4.32"

plugins {
    application
    kotlin("jvm") version "1.4.32"
}

group = "nl.tudelft"
version = "0.0.1-SNAPSHOT"


repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("org.apache.kafka:kafka-clients:2.8.0")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}
