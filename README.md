jackson-dataformat-soap
========

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://choosealicense.com/licenses/apache-2.0/)
[![](https://img.shields.io/maven-central/v/ru.sokomishalov.jackson/jackson-dataformat-soap)](https://mvnrepository.com/artifact/ru.sokomishalov.jackson/jackson-dataformat-soap)
[![](https://img.shields.io/jitpack/v/github/sokomishalov/jackson-dataformat-soap)](https://jitpack.io/#sokomishalov/jackson-dataformat-soap)

## Overview

Jackson SOAP implementation over jackson-dataformat-xml

## Distribution

Maven:

```xml

<dependency>
    <groupId>ru.sokomishalov.jackson</groupId>
    <artifactId>jackson-dataformat-soap</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle kotlin dsl:

```kotlin
implementation("ru.sokomishalov.jackson:jackson-dataformat-soap:0.1.0")
```

## Usage

You need to know that jackson requires to have setters for correct deserialization. So, if you are using code generation
from xsd/wsdl you also have to provide setters for collections like [here](https://gist.github.com/meiwin/2779731).

```kotlin
fun main() {
    val mapper = SoapMapper()
    val content = GetPersonOutput::class.java.getResource("/example/get_person_output_ws_addr.xml")?.readText().orEmpty()
    val deserialized: SoapEnvelope<SoapAddressingHeaders, GetPersonOutput> = mapper.readValue(content)
    val serialized = mapper.writeValueAsString(deserialized)
}
```