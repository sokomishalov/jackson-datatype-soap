/*
 * Copyright (c) 2021-present Mikhael Sokolov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.sokomishalov.jackson.datatype.soap.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser
import jakarta.xml.bind.JAXBElement
import ru.sokomishalov.jackson.datatype.soap.SoapAddressingHeaders
import ru.sokomishalov.jackson.datatype.soap.SoapEnvelope
import ru.sokomishalov.jackson.datatype.soap.SoapFault
import javax.xml.namespace.QName
import javax.xml.stream.XMLStreamConstants.*
import javax.xml.stream.XMLStreamReader

/**
 * @author sokomishalov
 */
internal class SoapEnvelopeDeserializers : Deserializers.Base() {

    override fun findBeanDeserializer(
        type: JavaType,
        config: DeserializationConfig,
        beanDesc: BeanDescription
    ): JsonDeserializer<*>? = when {
        SoapEnvelope::class.java.isAssignableFrom(type.rawClass) -> SoapEnvelopeDeserializer(type)
        JAXBElement::class.java.isAssignableFrom(type.rawClass) -> JaxbElementDeserializer(type)
        else -> super.findBeanDeserializer(type, config, beanDesc)
    }

    internal class SoapEnvelopeDeserializer(type: JavaType) : StdDeserializer<SoapEnvelope<*, *>>(type) {

        override fun deserialize(parser: JsonParser, context: DeserializationContext): SoapEnvelope<*, *> {
            val (headerClass, bodyClass) = valueType
                .bindings
                .typeParameters
                .let { it.getOrNull(0)?.rawClass to it.getOrNull(1)?.rawClass }

            val sr = (parser as FromXmlParser).staxReader

            var header: Any? = null
            var body: Any? = null

            // the parser may already have moved past the Envelope start tag
            if (sr.eventType == START_ELEMENT && sr.localName == "Envelope") sr.next()

            // only direct children of Envelope are inspected, so payload elements can never be taken for Header/Body/Fault
            loop@ while (sr.hasNext()) {
                when (sr.eventType) {
                    START_ELEMENT -> when (sr.localName) {
                        "Header" -> {
                            val envelopeNs = sr.namespaceURI.orEmpty()
                            header = when (headerClass) {
                                null, Nothing::class.java, Void::class.java -> null
                                SoapAddressingHeaders::class.java -> context.readValue(parser.child(sr), headerClass)
                                else -> if (sr.nextTag() == START_ELEMENT) context.readValue(parser.child(sr), headerClass) else null
                            }
                            sr.skipTo(END_ELEMENT, "Header", envelopeNs)
                        }
                        "Body" -> {
                            val envelopeNs = sr.namespaceURI.orEmpty()
                            if (sr.nextTag() == START_ELEMENT) {
                                when {
                                    sr.localName == "Fault" && sr.namespaceURI.orEmpty() == envelopeNs -> throw sr.soapFault(parser, context)
                                    bodyClass != null && bodyClass != Nothing::class.java && bodyClass != Void::class.java -> {
                                        body = context.readValue(parser.child(sr), bodyClass)
                                    }
                                }
                            }
                            break@loop
                        }
                        else -> sr.skipElement()
                    }
                    END_ELEMENT -> break@loop
                }
                sr.next()
            }

            // consume the rest of the document like the parser itself would
            while (sr.hasNext()) sr.next()

            return SoapEnvelope(
                header = header,
                body = body
            )
        }

        /**
         * Reads a SOAP 1.1 or SOAP 1.2 `Fault`, the reader must be positioned at its start tag.
         * Only direct children of `Fault` are interpreted, so `detail` contents can't override code or message.
         */
        private fun XMLStreamReader.soapFault(parser: JsonParser, context: DeserializationContext): SoapFault {
            var message: String? = null
            var code: String? = null
            var subcodes: List<String> = emptyList()
            var node: String? = null
            var role: String? = null
            var detail: JsonNode? = null

            while (nextTag() == START_ELEMENT) {
                when (localName) {
                    // SOAP 1.1
                    "faultcode" -> code = elementText.trim()
                    "faultstring" -> message = elementText
                    "faultactor" -> node = elementText.trim()
                    // SOAP 1.2
                    "Code" -> faultCodeValues().let { code = it.firstOrNull(); subcodes = it.drop(1) }
                    "Reason" -> message = faultReason()
                    "Node" -> node = elementText.trim()
                    "Role" -> role = elementText.trim()
                    // both
                    "detail", "Detail" -> {
                        val detailName = localName
                        val detailNs = namespaceURI.orEmpty()
                        detail = context.readTree(parser.child(this))
                        skipTo(END_ELEMENT, detailName, detailNs)
                    }
                    else -> skipElement()
                }
            }

            return SoapFault(
                message = message,
                code = code,
                subcodes = subcodes,
                node = node,
                role = role,
                detail = detail
            )
        }

        /** `Code/Value` followed by every nested `Subcode/Value`. */
        private fun XMLStreamReader.faultCodeValues(): List<String> {
            val values = mutableListOf<String>()
            while (nextTag() == START_ELEMENT) {
                when (localName) {
                    "Value" -> values += elementText.trim()
                    "Subcode" -> values += faultCodeValues()
                    else -> skipElement()
                }
            }
            return values
        }

        /** The first `Reason/Text`. */
        private fun XMLStreamReader.faultReason(): String? {
            var text: String? = null
            while (nextTag() == START_ELEMENT) {
                if (localName == "Text" && text == null) text = elementText else skipElement()
            }
            return text
        }

        /**
         * A parser for the element the reader is positioned at, read with the caller's [DeserializationContext],
         * so per-call settings (`ObjectReader` features, views, injectables) apply to header, body and fault detail.
         * Parser features, including XML ones, are copied from this parser for the same reason.
         */
        private fun JsonParser.child(sr: XMLStreamReader): JsonParser {
            val factory = codec?.factory as? XmlFactory ?: XmlFactory()
            return factory.createParser(sr).also {
                it.codec = codec
                it.overrideStdFeatures(featureMask, ALL_FEATURES)
                it.overrideFormatFeatures(formatFeatures, ALL_FEATURES)
                it.nextToken()
            }
        }

        private companion object {
            const val ALL_FEATURES = -1
        }

        override fun isCachable(): Boolean = true
    }

    internal class JaxbElementDeserializer(type: JavaType) : StdDeserializer<JAXBElement<*>>(type) {

        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): JAXBElement<*> {
            val contentType = wrappedClass()
            val res = ctxt.readValue(parser, contentType)
            return JAXBElement(QName(parser.currentName()), contentType, res)
        }

        override fun getNullValue(ctxt: DeserializationContext): JAXBElement<*> {
            val contentType = wrappedClass()
            return JAXBElement(QName(ctxt.parser.currentName()), contentType, null)
        }

        override fun isCachable(): Boolean = true

        @Suppress("UNCHECKED_CAST")
        private fun wrappedClass(): Class<Any?> = valueType.bindings.typeParameters[0].rawClass as Class<Any?>
    }
}

/** Moves the reader from a start tag to its matching end tag. */
internal fun XMLStreamReader.skipElement() {
    var depth = 1
    while (depth > 0 && hasNext()) {
        when (next()) {
            START_ELEMENT -> depth++
            END_ELEMENT -> depth--
        }
    }
}

/** Moves the reader forward until it is at the given event of the given element. */
internal fun XMLStreamReader.skipTo(event: Int, localName: String, namespace: String) {
    while (!(eventType == event && this.localName == localName && namespaceURI.orEmpty() == namespace) && hasNext()) next()
}
