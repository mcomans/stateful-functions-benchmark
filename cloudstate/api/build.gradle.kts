import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.5.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.spring") version "1.5.31"
    id("com.google.protobuf") version "0.8.12"
    id("com.google.cloud.tools.jib") version "3.1.4"
    idea
}

group = "nl.tudelft"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("net.logstash.logback:logstash-logback-encoder:6.6")

    implementation(kotlin("stdlib-jdk8"))
    implementation("io.cloudstate:cloudstate-kotlin-support:0.5.2")
    implementation("net.devh:grpc-client-spring-boot-starter:2.12.0.RELEASE")
    implementation("com.google.api.grpc:proto-google-common-protos:2.6.0")
    implementation("io.grpc:grpc-netty-shaded:1.41.0")
    implementation("io.grpc:grpc-protobuf:1.41.0")
    implementation("io.grpc:grpc-stub:1.41.0")
    implementation("com.google.protobuf:protobuf-java-util:3.18.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    api("org.apache.tomcat:annotations-api:6.0.53")
    protobuf(files("../shared-proto"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.18.0"
    }

    plugins {
        if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC)) {
            id("grpc") {
                artifact = "io.grpc:protoc-gen-grpc-java:1.41.0:osx-x86_64"
            }
        }
        else {
            id("grpc") {
                artifact = "io.grpc:protoc-gen-grpc-java:1.41.0"
            }
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

jib {
    from {
        image = "eclipse-temurin:11-jdk-alpine"
    }
    to {
        image = "benchmark-cloudstate-api"
    }
    container {
        ports = listOf("8080")
    }
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}
