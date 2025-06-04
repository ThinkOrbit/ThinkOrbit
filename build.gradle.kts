plugins {
	java
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "tw.yukina"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["springAiVersion"] = "1.0.0"

dependencies {
//	Jline
	implementation("org.jline:jline:3.30.0")
	implementation("org.jline:jline-remote-ssh:3.29.0")
	implementation("org.apache.sshd:sshd-core:2.12.0")
	implementation("org.apache.sshd:sshd-scp:2.12.0")
	implementation("org.apache.sshd:sshd-sftp:2.9.3")

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.ai:spring-ai-starter-model-openai")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
