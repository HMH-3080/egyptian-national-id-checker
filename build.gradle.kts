import org.jreleaser.model.Active

        plugins {
            kotlin("multiplatform") version "2.3.10"
            id("org.jreleaser") version "1.20.0"
            id("org.jetbrains.dokka") version "2.0.0"

            `maven-publish`
            signing
        }

group = "io.github.hmh-3080"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    js(IR) {
        browser()
        nodejs()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(11)

    withSourcesJar()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Egyptian National ID Checker")
            description.set("A multiplatform Kotlin library to validate and parse Egyptian National IDs.")
            url.set("https://github.com/HMH-3080/egyptian-national-id-checker")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("HMH-3080")
                    name.set("Hasan Mohamed")
                    email.set("hasanmo3080vr@gmail.com")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/HMH-3080/egyptian-national-id-checker.git")
                developerConnection.set("scm:git:ssh://github.com/HMH-3080/egyptian-national-id-checker.git")
                url.set("https://github.com/HMH-3080/egyptian-national-id-checker")
            }
        }
    }
}

signing {
    sign(publishing.publications)
}

jreleaser {
    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
    }

    release {
        github {
            enabled.set(false)
        }
    }

    deploy {
        maven {
            mavenCentral {
                create("central") {
                    active.set(Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}
