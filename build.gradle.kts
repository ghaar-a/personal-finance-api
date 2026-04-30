plugins {
	java
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "br.com.tr.finances"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

// OBRIGATÓRIO PARA DOCKER v29: Força o motor a falar API moderna
configurations.all {
	resolutionStrategy {
		force("com.github.docker-java:docker-java-core:3.4.0")
		force("com.github.docker-java:docker-java-transport-httpclient5:3.4.0")
	}
}

dependencies {
	// ===== CORE =====
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// ===== FLYWAY =====
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	// ===== WEB + SECURITY =====
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")

	// ===== MAPPING =====
	implementation("org.mapstruct:mapstruct:1.6.3")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

	// ===== RATE LIMIT =====
	implementation("com.bucket4j:bucket4j-core:8.10.1")

	// ===== JWT =====
	implementation("io.jsonwebtoken:jjwt-api:0.12.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")


	// ===== DATABASE =====
	runtimeOnly("org.postgresql:postgresql")

	// ===== LOMBOK =====
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// ===== SECURITY (XSS Sanitizer) =====
	implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")

	// ===== TESTES =====
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
	testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
	testImplementation("org.testcontainers:testcontainers:2.0.5")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.5")

	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configuração MapStruct para Spring
tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.add("-Amapstruct.defaultComponentModel=spring")
}

tasks.withType<Test> {
	useJUnitPlatform()

	// para debugar o Docker
	testLogging {
		events("passed", "skipped", "failed")
		showStackTraces = true
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
}