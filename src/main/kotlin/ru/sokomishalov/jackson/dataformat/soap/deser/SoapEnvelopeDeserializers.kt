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
package ru.sokomishalov.jackson.dataformat.soap.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser
import ru.sokomishalov.jackson.dataformat.soap.SoapAddressingHeaders
import ru.sokomishalov.jackson.dataformat.soap.SoapEnvelope
import ru.sokomishalov.jackson.dataformat.soap.SoapFault
import javax.xml.bind.JAXBElement
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

            while (sr.hasNext()) {
                val type = sr.eventType
                if (type == START_ELEMENT) {
                    when (sr.localName) {
                        "Header" -> {
                            header = when (headerClass) {
                                null, Nothing::class.java -> null
                                SoapAddressingHeaders::class.java -> parser.clone(sr).readValueAs(headerClass)
                                else -> if (sr.next() != END_ELEMENT) parser.clone(sr).readValueAs(headerClass) else null
                            }
                        }
                        "Fault" -> {
                            sr.soapFault()
                        }
                        "Body" -> {
                            when {
                                sr.nextTag() == START_ELEMENT && sr.localName == "Fault" -> {
                                    sr.soapFault()
                                }
                                bodyClass != null && bodyClass != Nothing::class.java -> {
                                    sr.nextTag()
                                    body = parser.readValueAs(bodyClass)
                                }
                            }
                        }
                    }
                }
                try {
                    sr.next()
                } catch (e: NoSuchElementException) {
                    break
                }
            }

            return SoapEnvelope(
                header = header,
                body = body
            )
        }

        private fun XMLStreamReader.soapFault() {
            var message: String? = null
            var code: String? = null

            while (hasNext()) {
                if (next() == START_ELEMENT) {
                    when (localName) {
                        "faultstring" -> {
                            if (next() == CHARACTERS) message = text
                        }
                        "faultcode" -> {
                            if (next() == CHARACTERS) code = text
                        }
                        "Reason" -> {
                            while (hasNext()) {
                                if (next() == START_ELEMENT && localName == "Text") {
                                    if (next() == CHARACTERS) message = text
                                    break
                                }
                            }
                        }
                        "Code" -> {
                            while (hasNext()) {
                                if (next() == START_ELEMENT && localName == "Value") {
                                    if (next() == CHARACTERS) code = text
                                    break
                                }
                            }
                        }
                    }
                }
            }

            throw SoapFault(
                message = message,
                code = code
            )
        }

        private fun JsonParser.clone(sr: XMLStreamReader): JsonParser = when (codec) {
            is XmlMapper -> (codec as XmlMapper).factory.createParser(sr)
            else -> this
        }

        override fun isCachable(): Boolean = true
    }

    internal class JaxbElementDeserializer(type: JavaType) : StdDeserializer<JAXBElement<*>>(type) {

        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): JAXBElement<*> {
            val contentType = wrappedClass()
            val res = parser.readValueAs(contentType)
            return JAXBElement(QName(parser.currentName), contentType, res)
        }

        override fun getNullValue(ctxt: DeserializationContext): JAXBElement<*> {
            val contentType = wrappedClass()
            return JAXBElement(QName(ctxt.parser.currentName), contentType, null)
        }

        override fun isCachable(): Boolean = true

        @Suppress("UNCHECKED_CAST")
        private fun wrappedClass(): Class<Any?> = valueType.bindings.typeParameters[0].rawClass as Class<Any?>
    }
}