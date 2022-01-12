import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
}

group = "nl.tudelft"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared-types"))
    implementation("org.apache.flink:statefun-sdk-java:3.0.0")
    implementation("org.jboss.xnio:xnio-nio:3.6.5.Final")
    implementation("io.undertow:undertow-core:2.1.0.Final")
    implementation("net.logstash.logback:logstash-logback-encoder:6.6")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Jar>() {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "Expose"
    }

    from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}