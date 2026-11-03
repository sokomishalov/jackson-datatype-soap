# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Kotlin library that adds SOAP envelope (de)serialization on top of `jackson-dataformat-xml`. Published to Maven Central as `ru.sokomishalov.jackson:jackson-datatype-soap-{jaxb,jakarta}`. Targets Java 1.8 bytecode; CI builds on JDK 21.

## Commands

```bash
./mvnw clean test                                  # what CI runs (.github/workflows/ci.yml)
./mvnw -pl jackson-datatype-soap-jakarta -am test  # one module (+ common, which it depends on)
./mvnw -pl jackson-datatype-soap-jakarta -am test -Dtest=SoapMapperTest -Dsurefire.failIfNoSpecifiedTests=false
./mvnw license:format                              # apply header.txt license header to all *.kt
```

Version is the CI-friendly `${revision}` property in the root `pom.xml` (flattened by `flatten-maven-plugin`). `release.sh` bumps it, generates `CHANGELOG.md` (generated from GitHub, so don't edit it by hand), deploys with `-P ossrh`, tags and pushes to master — don't run it unless asked. Despite the profile name, `ossrh` publishes through the Sonatype Central Portal (`central-publishing-maven-plugin`, server id `central` in `~/.m2/settings.xml`); OSSRH itself is shut down. Every published module needs its own `<name>` (Central rejects it otherwise; `name` isn't inherited). To check the release build without signing or uploading: `./mvnw -P ossrh -DskipTests -Dgpg.skip clean dokka:javadocJar verify`.

## Architecture

Three Maven modules, all in package `ru.sokomishalov.jackson.datatype.soap`:

- **`jackson-datatype-soap-common`** — model types with no XML-binding dependency: `SoapEnvelope<H, B>`, `SoapAddressingHeaders` (WS-Addressing), `SoapMultipleHeaders`, `SoapFault` (a `RuntimeException`), `SoapConstants`.
- **`jackson-datatype-soap-jaxb`** and **`jackson-datatype-soap-jakarta`** — near-identical implementations; they differ only in imports (`javax.xml.bind` vs `jakarta.xml.bind`, and the corresponding Jackson JAXB/Jakarta annotation module). **Any change to one must be mirrored in the other**; `diff -r jackson-datatype-soap-jaxb/src/main jackson-datatype-soap-jakarta/src/main` should show only import lines.

How the pieces fit in each implementation module:

- `SoapMapper` is an `XmlMapper` that registers `SoapModule`. `SoapMapperExtensions.kt` adds reified Kotlin `readValue<H, B>` / `readValueBody<B>`.
- `SoapMapper(module)` takes a configured `SoapModule` (e.g. SOAP 1.2 envelope namespace) and overrides `copy()`; `AlwaysUseNamespacesXmlSerializerProvider` overrides `copy()` too, otherwise a copied mapper silently falls back to the plain provider.
- `SoapModule` extends the (Jakarta)XmlBind annotation module with `SoapJaxbAnnotationIntrospector`, registers the envelope serializers/deserializers, swaps in `AlwaysUseNamespacesXmlSerializerProvider`, and mutates the owning mapper's features (lenient: ignores unknown properties, case-insensitive, single value as array, etc.), sets the default property inclusion to `NON_NULL`, and Woodstox's auto namespace prefix.
- `deser/SoapEnvelopeDeserializers` walks the raw StAX reader (`FromXmlParser.staxReader`) over the direct children of `Envelope` only (`Header`, `Body`; `Fault` only as the first `Body` child in the envelope namespace), skipping everything else by depth. Header/body types come from the envelope's generic type parameters. Header, body and fault `detail` are read by a second parser on the same StAX reader (`child()`), through the caller's `DeserializationContext` with the outer parser's features copied — keep it that way, or `ObjectReader` settings stop applying. Where that second parser leaves the reader isn't predictable, so afterwards the code skips forward to the matching end tag (`skipTo`) instead of tracking depth. Any SOAP 1.1 or 1.2 fault is **thrown** as `SoapFault` rather than returned. Also handles `JAXBElement`.
- `ser/SoapEnvelopeSerializers` writes `Envelope`/`Header`/`Body` in the configured envelope namespace via `ToXmlGenerator.setNextName(QName)`, special-cases `SoapAddressingHeaders` and iterable headers, wraps `@XmlRootElement` classes and `JAXBElement`s in their element name (JAXB default naming for `##default`), and writes values through the caller's `SerializerProvider` (so `ObjectWriter` views apply). An absent header is omitted.
- `SoapJaxbAnnotationIntrospector` resolves element namespaces: explicit `@XmlElement(namespace)` → empty for attributes → declaring class's namespace for fields/methods/JDK types → otherwise the type's own package `@XmlSchema` namespace (cached in `util/NamespaceCache`). It also implements JAXB null handling: `xsi:nil` (`XsiNilSerializer`) only for `nillable` members, and an explicit member `@JsonInclude` wins. A null serializer is used even when inclusion suppresses nulls, so it must only be returned when nulls are actually meant to be written.

## Tests

Each implementation module has its own `SoapMapperTest` and `SoapStandardTest` (SOAP 1.1/1.2, WS-Addressing, faults, inclusion, `ObjectReader`/`ObjectWriter` settings; asserts on namespaced element names, never on generated prefixes) plus duplicated `src/test/resources`. `SoapStandardTest` must stay identical in both modules except for the `javax`/`jakarta` imports. Test POJOs are **generated at build time** by `cxf-codegen-plugin` (wsdl2java) from `src/test/resources/wsdl/*.wsdl` into `target/generated-test-sources/cxf` — they won't exist until `generate-test-sources` has run. To add a WSDL, add it to the `wsdlOptions` in that module's `pom.xml` (both modules). Tests use JUnit 5 via `kotlin-test-junit5` and `easy-random` for random POJOs.
