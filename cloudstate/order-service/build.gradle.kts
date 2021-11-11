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
    implementation("io.grpc:grpc-netty-shaded:1.41.0")
    implementation("io.grpc:grpc-protobuf:1.41.0")
    implementation("io.grpc:grpc-stub:1.41.0")
    implementation("com.google.protobuf:protobuf-java-util:3.18.0")
    api("org.apache.tomcat:annotations-api:6.0.53")
    protobuf(files("../shared-proto"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.18.0"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.41.0:osx-x86_64"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
            }
//            it.builtins {
//                id("kotlin")
//            }
        }
    }
}

jib {
    from {
        image = "eclipse-temurin:11-jdk-alpine"
    }
    to {
        image = "benchmark-cloudstate-order-service"
    }
    container {
        mainClass = "ApplicationKt"
        ports = listOf("8080")
    }
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}