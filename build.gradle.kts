plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.10"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.bridger"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Bridger."

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

	implementation("org.jetbrains.exposed:exposed-core:0.55.0")
	implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
	implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
	implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.55.0")
	implementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.55.0")

	// Database driver
	runtimeOnly("com.h2database:h2")

	implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
