jackson-datatype-soap
========
~~Here should be some fancy logo~~

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://choosealicense.com/licenses/apache-2.0/)
[![](https://img.shields.io/maven-central/v/ru.sokomishalov.jackson/jackson-datatype-soap-jakarta)](https://mvnrepository.com/artifact/ru.sokomishalov.jackson/jackson-datatype-soap-jakarta)
[![](https://img.shields.io/jitpack/v/github/sokomishalov/jackson-datatype-soap)](https://jitpack.io/#sokomishalov/jackson-datatype-soap)

## Overview

SOAP 1.1 and SOAP 1.2 envelopes for Jackson, on top of [jackson-dataformat-xml](https://github.com/FasterXML/jackson-dataformat-xml).
It reads and writes JAXB classes (for example generated from a WSDL by `wsdl2java`), WS-Addressing headers and SOAP faults.

Requires Java 8+ and Jackson 2.x. A Jackson 3 (`tools.jackson`) build is planned.

## Distribution

Two artifacts, the same API: pick the one matching your JAXB annotations.

Maven:

```xml
<!-- Legacy (javax.xml.bind) artifact -->
<dependency>
    <groupId>ru.sokomishalov.jackson</groupId>
    <artifactId>jackson-datatype-soap-jaxb</artifactId>
    <version>x.y.z</version>
</dependency>
```
```xml
<!-- New (jakarta.xml.bind) artifact -->
<dependency>
    <groupId>ru.sokomishalov.jackson</groupId>
    <artifactId>jackson-datatype-soap-jakarta</artifactId>
    <version>x.y.z</version>
</dependency>
```

Gradle kotlin dsl:

```kotlin
// Legacy (javax.xml.bind) artifact
implementation("ru.sokomishalov.jackson:jackson-datatype-soap-jaxb:x.y.z")
// New (jakarta.xml.bind) artifact
implementation("ru.sokomishalov.jackson:jackson-datatype-soap-jakarta:x.y.z")
```

## Usage

The example below reads [this SOAP message](./jackson-datatype-soap-jakarta/src/test/resources/example/get_person_output_ws_addr.xml).

```kotlin
val mapper = SoapMapper()

// read
val envelope: SoapEnvelope<SoapAddressingHeaders, GetPersonOutput> = mapper.readValue(content)
val body: GetPersonOutput? = mapper.readValueBody(content)

// write
val xml = mapper.writeValueAsString(SoapEnvelope(header = SoapAddressingHeaders(action = "urn:getPerson"), body = request))
val withoutHeader = mapper.writeValueAsString(SoapEnvelope.fromBody(request))
```

From Java:

```java
SoapMapper mapper = new SoapMapper();
SoapEnvelope<SoapAddressingHeaders, GetPersonOutput> envelope = mapper.readValue(content, GetPersonOutput.class, SoapAddressingHeaders.class);
```

`SoapMapper` is an `XmlMapper`, so `readerFor(...)`, `writerWithView(...)` and other per-call settings also apply to the header and body.

### Headers

- `SoapAddressingHeaders`: WS-Addressing `Action`, `MessageID`, `From`, `To`, `ReplyTo`, `FaultTo`. Other headers next to them are ignored on read.
- Any JAXB class: written as its `@XmlRootElement`, read from the first header element.
- `SoapMultipleHeaders(first, second, ...)`: several header elements, write only.

### SOAP 1.2

SOAP 1.1 is written by default. Both versions are read.

```kotlin
val mapper = SoapMapper(SoapModule(soapEnvelopeNamespace = SoapConstants.SOAP_12_ENVELOPE_NAMESPACE))
```

### Faults

Reading an envelope whose body is a `Fault` throws `SoapFault`:

| Property | SOAP 1.1 | SOAP 1.2 |
|---|---|---|
| `message` | `faultstring` | first `Reason/Text` |
| `code` | `faultcode` | `Code/Value` |
| `subcodes` | | `Subcode/Value` chain |
| `node` | `faultactor` | `Node` |
| `role` | | `Role` |
| `detail` | `detail` as `JsonNode` | `Detail` as `JsonNode` |

```kotlin
try {
    mapper.readValueBody<GetPersonOutput>(content)
} catch (fault: SoapFault) {
    val errorCode = fault.detail?.at("/error/code")?.asText()
}
```

## Nulls and empty headers

Since 0.4.0, serialization follows a JAXB marshaller:

- `null` is omitted: `SoapMapper` sets the default property inclusion to `NON_NULL`. Earlier versions wrote `xsi:nil` for every `null`, which fails schema validation for non-nillable elements;
- `nillable` elements (`@XmlElement(nillable = true)` / `@XmlElementWrapper(nillable = true)`) and nil `JAXBElement`s are written as `xsi:nil="true"`;
- the usual Jackson inclusion settings still apply: `setDefaultPropertyInclusion(...)` on the mapper, `@JsonInclude` on a class, and `@JsonInclude` on a property, which also takes precedence over `nillable`;
- an absent (`null` or empty `SoapMultipleHeaders`) header is omitted instead of being written as an empty `<Header/>`.

## Building

```bash
./mvnw clean test
```

Releases are published to Maven Central through the Central Portal with `./release.sh`, which needs a `central`
server entry with a Portal user token in `~/.m2/settings.xml` and a GPG key for signing.
