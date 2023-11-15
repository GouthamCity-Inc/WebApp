import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.1.4"
	id("io.spring.dependency-management") version "1.1.3"
	kotlin("jvm") version "1.8.22"
	kotlin("plugin.spring") version "1.8.22"
	kotlin("plugin.jpa") version "1.8.22"
	id("com.github.johnrengelman.shadow") version "7.1.1"
}

group = "edu.northeastern"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.mysql:mysql-connector-j")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")

	// my custom libraries
	implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")
	implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
	implementation("com.intuit.karate:karate-core:1.4.0")
	testImplementation("com.intuit.karate:karate-junit5:1.4.0")
	implementation("org.mariadb.jdbc:mariadb-java-client:3.2.0")
	implementation("com.timgroup:java-statsd-client:3.1.0")
	implementation("io.micrometer:micrometer-registry-statsd")
	implementation("com.timgroup:java-statsd-client:3.1.0")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.test {
	useJUnitPlatform()
	systemProperty("karate.options", System.getProperty("karate.options"))
	systemProperty("karate.env", System.getProperty("karate.env"))
	outputs.upToDateWhen { false }
	testLogging.showStandardStreams = true
}

sourceSets {
	val test by getting {
		resources {
			srcDir(file("src/test/java"))
			exclude("**/*.java")
		}
	}
}