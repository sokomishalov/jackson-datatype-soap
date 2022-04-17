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
package ru.sokomishalov.jackson.dataformat.soap.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import ru.sokomishalov.jackson.dataformat.soap.SoapAddressingHeaders
import ru.sokomishalov.jackson.dataformat.soap.SoapConstants.SOAP_ADDRESSING_NAMESPACE
import ru.sokomishalov.jackson.dataformat.soap.SoapConstants.XML_SCHEMA_NAMESPACE
import ru.sokomishalov.jackson.dataformat.soap.SoapEnvelope
import ru.sokomishalov.jackson.dataformat.soap.util.NamespaceCache
import javax.xml.bind.JAXBElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.namespace.QName

/**
 * @author sokomishalov
 */
internal class SoapEnvelopeSerializers(private val ns: String) : Serializers.Base() {

    override fun findSerializer(
        config: SerializationConfig,
        type: JavaType,
        beanDesc: BeanDescription
    ): JsonSerializer<*>? = when {
        SoapEnvelope::class.java.isAssignableFrom(type.rawClass) -> SoapEnvelopeSerializer(ns)
        JAXBElement::class.java.isAssignableFrom(type.rawClass) -> JaxbElementSerializer
        else -> super.findSerializer(config, type, beanDesc)
    }

    internal class SoapEnvelopeSerializer(private val ns: String) : StdSerializer<SoapEnvelope<*, *>>(SoapEnvelope::class.java) {

        override fun serialize(envelope: SoapEnvelope<*, *>?, gen: JsonGenerator, provider: SerializerProvider) {
            if (envelope != null && gen is ToXmlGenerator) {
                with(gen) {
                    setNextName(QName(ns, "Envelope"))
                    writeStartObject()
                    staxWriter.writeNamespace("xsi", XML_SCHEMA_NAMESPACE)
                    serializeElement(localPart = "Header", element = envelope.header, isHeader = true)
                    serializeElement(localPart = "Body", element = envelope.body, isHeader = false)
                    writeEndObject()
                }
            }
        }

        private fun ToXmlGenerator.serializeElement(localPart: String, element: Any?, isHeader: Boolean) {
            with(this) {
                setNextName(QName(ns, localPart))
                writeFieldName(localPart)

                val annotation = element?.javaClass?.getAnnotation(XmlRootElement::class.java)
                when {
                    isHeader && element is SoapAddressingHeaders -> {
                        writeStartObject()
                        staxWriter.writeNamespace("wsa", SOAP_ADDRESSING_NAMESPACE)

                        writeSoapAddressingField("Action", element.action)
                        writeSoapAddressingField("MessageID", element.messageId)
                        writeSoapAddressingField("From", element.from)
                        writeSoapAddressingField("To", element.to)

                        writeSoapAddressingEndpoint("ReplyTo", element.replyTo)
                        writeSoapAddressingEndpoint("FaultTo", element.faultTo)

                        writeEndObject()
                    }
                    isHeader && element is Iterable<*> -> {
                        writeStartObject()
                        element.forEach {
                            if (it != null) {
                                setNextName(QName(NamespaceCache.getNamespace(it.javaClass), annotation?.name ?: it.javaClass.simpleName))
                                writeFieldName(annotation?.name ?: it.javaClass.simpleName)
                                writeObject(it)
                            }
                        }
                        writeEndObject()
                    }
                    annotation != null -> {
                        writeStartObject()
                        setNextName(QName(NamespaceCache.getNamespace(element.javaClass), annotation.name))
                        writeFieldName(annotation.name)
                        writeObject(element)
                        writeEndObject()
                    }
                    else -> writeObject(element)
                }
            }
        }

        private fun ToXmlGenerator.writeSoapAddressingEndpoint(
            name: String,
            endpoint: SoapAddressingHeaders.Endpoint?
        ) {
            if (endpoint != null) {
                setNextName(QName(SOAP_ADDRESSING_NAMESPACE, name))
                writeFieldName(name)
                writeStartObject()
                writeSoapAddressingField("Address", endpoint.address)
                writeSoapAddressingField("ServiceName", endpoint.serviceName)
                writeEndObject()
            }
        }

        private fun ToXmlGenerator.writeSoapAddressingField(name: String, value: String?) {
            if (value != null) {
                setNextName(QName(SOAP_ADDRESSING_NAMESPACE, name))
                writeFieldName(name)
                writeRawValue(value)
            }
        }
    }

    internal object JaxbElementSerializer : StdSerializer<JAXBElement<*>>(JAXBElement::class.java) {
        override fun serialize(value: JAXBElement<*>?, gen: JsonGenerator, provider: SerializerProvider) {
            when {
                value != null && !value.isNil -> gen.writeObject(value.value)
                else -> gen.writeNull()
            }
        }
    }

    internal object XsiNilSerializer : JsonSerializer<Any?>() {
        override fun serialize(value: Any?, gen: JsonGenerator?, provider: SerializerProvider?) {
            when (gen) {
                is ToXmlGenerator -> with(gen) {
                    writeStartObject()
                    staxWriter.writeNamespace("xsi", XML_SCHEMA_NAMESPACE)
                    staxWriter.writeAttribute("xsi:nil", "true")
                    writeEndObject()
                }
                else -> gen?.writeNull()
            }
        }
    }
}