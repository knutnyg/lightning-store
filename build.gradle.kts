val ktorVersion = "1.6.7"
val jacksonVersion = "2.13.1"
val junitJupiterVersion = "5.8.2"

group = "xyz.nygaard"
version = "1.1"

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.siouan.frontend-jdk11") version "6.0.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

buildscript {
    dependencies {
        classpath("org.siouan:frontend-gradle-plugin-jdk11:6.0.0")
    }
}

frontend {
    nodeVersion.set("16.13.1")
    assembleScript.set("run build")
    cleanScript.set("run clean")
    packageJsonDirectory.set(File("src/main/frontend"))
}

apply(plugin = "org.siouan.frontend-jdk11")

dependencies {
    implementation("org.lightningj:lightningj:0.13.0-Beta")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.0")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("com.github.nitram509:jmacaroons:0.4.1")

    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("net.logstash.logback:logstash-logback-encoder:7.0.1")

    //Database
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("org.flywaydb:flyway-core:8.1.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:1.12.1")
    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty") // conflicts with WireMock
        exclude(group = "junit")
    }
    testImplementation("org.testcontainers:postgresql:1.16.2")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    named<Jar>("jar") {
        archiveBaseName.set("lightning-store")

        manifest {
            attributes["Main-Class"] = "xyz.nygaard.BootstrapKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<Wrapper> {
        gradleVersion = "7.3.2"
    }
}
