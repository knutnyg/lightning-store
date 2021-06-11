
val ktorVersion = "1.5.1"
val logbackVersion = "1.2.3"
val logstashEncoderVersion = "5.1"
val kotlinVersion = "1.3.40"
val jacksonVersion = "2.9.7"
val postgresVersion = "42.2.5"
val flywayVersion = "5.2.4"
val hikariVersion = "3.3.0"
val junitJupiterVersion = "5.6.2"

group = "xyz.nygaard"
version = "1.1"

plugins {
    kotlin("jvm") version "1.4.0"
}

buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    jcenter()
}

dependencies {
    compile("org.lightningj:lightningj:0.12.0-Beta")
    compile("javax.xml.bind:jaxb-api:2.3.1")
    runtime("javax.json:javax.json-api:1.1.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    //Database
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")

    //Test
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:1.10.5")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty") // conflicts with WireMock
        exclude(group = "junit")
    }
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.4")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "14"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "14"
    }

    named<Jar>("jar") {
        archiveBaseName.set("app")

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
        gradleVersion = "6.8.1"
    }
}
