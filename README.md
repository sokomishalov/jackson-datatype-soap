jackson-datatype-soap
========
~~Here should be some fancy logo~~

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://choosealicense.com/licenses/apache-2.0/)
[![](https://img.shields.io/maven-central/v/ru.sokomishalov.jackson/jackson-datatype-soap-jakarta)](https://mvnrepository.com/artifact/ru.sokomishalov.jackson/jackson-datatype-soap-jakarta)
[![](https://img.shields.io/jitpack/v/github/sokomishalov/jackson-datatype-soap)](https://jitpack.io/#sokomishalov/jackson-datatype-soap)

## Overview

Jackson SOAP implementation over jackson-dataformat-xml

## Distribution

Maven:

```xml
<!-- Legacy (JAXB) artifact -->
<dependency>
    <groupId>ru.sokomishalov.jackson</groupId>
    <artifactId>jackson-datatype-soap-jaxb</artifactId>
    <version>x.y.z</version>
</dependency>
```
```xml
<!-- New (Jakarta) artifact -->
<dependency>
    <groupId>ru.sokomishalov.jackson</groupId>
    <artifactId>jackson-datatype-soap-jakarta</artifactId>
    <version>x.y.z</version>
</dependency>
```

Gradle kotlin dsl:

```kotlin
// Legacy (JAXB) artifact
implementation("ru.sokomishalov.jackson:jackson-datatype-soap-jaxb:x.y.z")
// New (Jakarta) artifact
implementation("ru.sokomishalov.jackson:jackson-datatype-soap-jakarta:x.y.z")
```

## Usage

Used [this SOAP message](./src/test/resources/example/get_person_output_ws_addr.xml) for deserialization in example below.
```kotlin
fun main() {
    val mapper = SoapMapper()
    val content = this.javaClass.getResource("/example/get_person_output_ws_addr.xml").readText()
    val deserialized: SoapEnvelope<SoapAddressingHeaders, GetPersonOutput> = mapper.readValue(content)
    val serialized = mapper.writeValueAsString(deserialized)
}
```