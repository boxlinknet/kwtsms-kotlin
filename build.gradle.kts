import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
    `java-library`
}

group = "com.github.boxlinknet"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.boxlinknet"
            artifactId = "kwtsms-kotlin"
            version = project.version.toString()
            from(components["java"])

            pom {
                name.set("kwtsms")
                description.set("Official Kotlin client for the kwtSMS SMS gateway API")
                url.set("https://github.com/boxlinknet/kwtsms-kotlin")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("boxlinknet")
                        name.set("Boxlink")
                        url.set("https://www.kwtsms.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/boxlinknet/kwtsms-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/boxlinknet/kwtsms-kotlin.git")
                    url.set("https://github.com/boxlinknet/kwtsms-kotlin")
                }
            }
        }
    }
}
