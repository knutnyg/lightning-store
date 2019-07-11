import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "1.2.0"
val logbackVersion = "1.2.3"
val logstashEncoderVersion = "5.1"
val kotlinVersion = "1.3.40"
val jacksonVersion = "2.9.7"

group = "xyz.nygaard"
version = "1.0"

plugins {
    java
    kotlin("jvm") version "1.3.40"
    id("com.diffplug.gradle.spotless") version "3.14.0"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
    jcenter()
    maven ( url = "https://dl.bintray.com/kotlin/ktor" )
}

dependencies {
    compile ("org.lightningj:lightningj:0.7.0-Beta")
    compile ("javax.xml.bind:jaxb-api:2.3.1")
    runtime ("javax.json:javax.json-api:1.1.2")

    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation ("io.ktor:ktor-server-netty:$ktorVersion")

    implementation ("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation ("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    
    implementation ("ch.qos.logback:logback-classic:$logbackVersion")
    implementation ("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

}

tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "xyz.nygaard.BootstrapKt"
    }

    create("printVersion") {

        doLast {
            println(project.version)
        }
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging {
            showStandardStreams = true
        }
    }
}
