# Egyptian National ID Checker

[![Maven Central](https://img.shields.io/maven-central/v/io.github.hmh-3080/egyptian-national-id-checker)](https://central.sonatype.com/artifact/io.github.hmh-3080/egyptian-national-id-checker)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-blueviolet)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

A **lightweight**, **type-safe** Kotlin Multiplatform (KMP) library to validate and parse Egyptian National ID numbers (`الرقم القومي`). Runs on **JVM**, **JS** (Browser & Node.js), and **iOS**.

---

## Features

- ✅ **100% Multiplatform** — Pure `commonMain` code, no `expect`/`actual`, no platform-specific hooks.
- ✅ **Robust Validation** — Checks format, date-of-birth (including leap years), governorate code, and the official civil registry weighted **mod-11 checksum**.
- ✅ **Smart Parsing** — Extracts birth date, governorate, gender, century, serial number, and check digit.
- ✅ **Arabic-Indic Numerals** — Automatically handles `٠-٩` / `۰-۹` (Unicode `Nd` category).
- ✅ **Garbage-Tolerant Input** — Strips spaces, hyphens, and other non-digit characters automatically.
- ✅ **Zero Exceptions in Default Path** — `parse()` never throws; ideal for high-load backends (Ktor, Spring) and mobile apps.

---

## Installation

Add to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.github.hmh-3080:egyptian-national-id-checker:1.0.0")
            }
        }
    }
}
```

---

## Egyptian National ID Format

The 14-digit ID is structured as follows:

```
C YY MM DD GG SSS G CHK
0 12 34 56 78 91011 12 13
```

| Position | Length | Field | Example |
|---|---|---|---|
| **0** | 1 | Century (`2` = 1900s, `3` = 2000s) | `2` |
| **1–2** | 2 | Year of birth (last 2 digits) | `90` |
| **3–4** | 2 | Month of birth (`01`–`12`) | `01` |
| **5–6** | 2 | Day of birth (`01`–`31`) | `01` |
| **7–8** | 2 | Governorate code | `01` (Cairo) |
| **9–11** | 3 | Serial number (unique per birth date) | `000` |
| **12** | 1 | Gender (odd = Male, even = Female) | `1` |
| **13** | 1 | Check digit (weighted mod-11) | `7` |

**Example:** `29001010100017` → Male born 1990-01-01 in Cairo.

---

## Usage

### 1. Quick Validation

```kotlin
import io.github.hmh3080.egyptianid.*

fun main() {
    val id = "29001010100017"

    println(id.isValidEgyptianId())   // true
    println(id.isValidEgyptianIdLength()) // true
}
```

### 2. Parsing

```kotlin
import io.github.hmh3080.egyptianid.*
import io.github.hmh3080.egyptianid.models.Gender
import io.github.hmh3080.egyptianid.models.Governorate

fun main() {
    val id = "30512250212327"

    // Automatically returns a NationalIdInfo — never throws
    val info = id.toNationalIdInfo()

    println(info.isValid)                          // true
    println(info.dateOfBirth)                      // 2005-12-25
    println(info.gender)                           // FEMALE
    println(info.governorate)                      // ALEXANDRIA
    println(info.century)                          // 2000
    println(info.birthYear)                        // 2005
    println(info.serialNumber)                     // 123
    println(info.checkDigit)                       // 7
    println(info.centuryDigit)                     // 3
}
```

### 3. Parse with Null Safety

```kotlin
val info = "invalid".toNationalIdInfoOrNull()
println(info) // null
```

### 4. Parse Strict (Throws on Invalid)

```kotlin
import io.github.hmh3080.egyptianid.parser.NationalIdParser

try {
    val info = NationalIdParser.parseValid("bad input")
} catch (e: IllegalArgumentException) {
    println("Invalid ID: ${e.message}")
}
```

### 5. Manual Validation API

```kotlin
import io.github.hmh3080.egyptianid.validator.NationalIdValidator

val id = "29001010100017"

NationalIdValidator.isValid(id)             // true (full check)
NationalIdValidator.isValidFormat(id)       // true (14 digits + valid century)
NationalIdValidator.isValidDate(id)         // true (valid calendar date)
NationalIdValidator.isValidGovernorate(id)  // true (code 01 = Cairo)
NationalIdValidator.isValidCheckDigit(id)   // true (mod-11 checksum)
```

### 6. Input Sanitization

```kotlin
import io.github.hmh3080.egyptianid.*

// Handles spaces, hyphens, and Arabic-Indic digits
println("305 12 25 02 123 2 7".sanitizeEgyptianId())    // 30512250212327
println("305-12-25-02123-27".sanitizeEgyptianId())       // 30512250212327
println("٣٠٥١٢٢٥٠٢١٢٣٢٧".extractEgyptianIdDigits())      // 30512250212327
```

### 7. Low-Level API

```kotlin
import io.github.hmh3080.egyptianid.models.Gender
import io.github.hmh3080.egyptianid.models.Governorate

// Gender from a digit
println(Gender.fromDigit('1'))   // MALE
println(Gender.fromDigit('2'))   // FEMALE

// Governorate from a code
println(Governorate.fromCode(1))   // CAIRO
println(Governorate.fromCode(14))  // GIZA
```

---

## API Overview

| Extension function | Returns | Description |
|---|---|---|
| `String.isValidEgyptianId()` | `Boolean` | Full validation |
| `String.toNationalIdInfo()` | `NationalIdInfo` | Parse (never throws) |
| `String.toNationalIdInfoOrNull()` | `NationalIdInfo?` | Parse or `null` |
| `String.sanitizeEgyptianId()` | `String` | Strip non-digits, truncate to 14 |
| `String.extractEgyptianIdDigits()` | `String` | Keep only digits |
| `String.isValidEgyptianIdLength()` | `Boolean` | Exactly 14 digits? |

### `NationalIdInfo` Properties

| Property | Type | Description |
|---|---|---|
| `rawId` | `String` | Original input |
| `dateOfBirth` | `LocalDate` | Parsed birth date |
| `century` | `Int` | Century base (1900, 2000) |
| `governorate` | `Governorate` | Matched governorate |
| `gender` | `Gender` | MALE or FEMALE |
| `serialNumber` | `Int` | 3-digit serial |
| `checkDigit` | `Int` | 14th digit (checksum) |
| `isValid` | `Boolean` | Passed all checks? |

---

## Development

```bash
# Run JVM tests
./gradlew jvmTest

# Build all targets
./gradlew build

# Publish to Maven Local (for testing)
./gradlew publishToMavenLocal
```

---

## License

```
Copyright 2026 Hasan Mohamed

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
