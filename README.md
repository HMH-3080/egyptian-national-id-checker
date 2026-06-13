# Egyptian National ID Checker (KMP)

A lightweight, zero-dependency, type-safe Kotlin Multiplatform (KMP) library to validate and parse Egyptian National IDs. It runs purely in `commonMain` and supports JVM, JS (Browser & Node.js), and iOS.

## Features
- **100% Multiplatform:** Pure Kotlin implementation with no platform-specific hooks.
- **Robust Validation:** Uses the official civil registry checksum algorithm (mod 11) with exact weights.
- **Smart Parsing:** Extracts Date of Birth, Governorate, and Gender effortlessly.
- **Highly Sanitized:** Automatically handles Arabic/Eastern Arabic numerals (`٠-٩`), spaces, and hyphens.
- **Performance-First:** Designed with zero exceptions thrown during parsing, making it extremely friendly for high-load backend configurations (like Ktor with Virtual Threads) and mobile applications.

## Installation

Add the dependency to your `build.gradle.kts` in the `commonMain` source set:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.github.hmh-3080:egyptian-national-id-checker:1.0.0")
            }
        }
    }
}