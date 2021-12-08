import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0"
    id("com.google.protobuf") version "0.8.12"
    id("com.google.cloud.tools.jib") version "3.1.4"
    idea
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

group = "nl.tudelft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.cloudstate:cloudstate-kotlin-support:0.5.2")
    implementation("com.google.api.grpc:proto-google-common-protos:2.6.0")
    implementation("com.google.protobuf:protobuf-java-util:3.18.0")
    implementation("net.logstash.logback:logstash-logback-encoder:6.6")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
    protobuf(files("../shared-proto"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.18.0"
    }
}

jib {
    from {
        image = "eclipse-temurin:11-jdk-alpine"
    }
    to {
        image = "benchmark-cloudstate-user-service"
    }
    container {
        mainClass = "ApplicationKt"
        ports = listOf("8080")
    }
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}