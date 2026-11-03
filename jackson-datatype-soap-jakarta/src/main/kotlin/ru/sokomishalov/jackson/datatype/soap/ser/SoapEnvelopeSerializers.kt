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
package ru.sokomishalov.jackson.datatype.soap.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.annotation.XmlRootElement
import ru.sokomishalov.jackson.datatype.soap.SoapAddressingHeaders
import ru.sokomishalov.jackson.datatype.soap.SoapConstants.SOAP_ADDRESSING_NAMESPACE
import ru.sokomishalov.jackson.datatype.soap.SoapConstants.XML_SCHEMA_NAMESPACE
import ru.sokomishalov.jackson.datatype.soap.SoapEnvelope
import ru.sokomishalov.jackson.datatype.soap.util.NamespaceCache
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
                    // Header is optional, Body is mandatory
                    if (envelope.header.isPresent) serializeElement(localPart = "Header", element = envelope.header, isHeader = true, provider = provider)
                    serializeElement(localPart = "Body", element = envelope.body, isHeader = false, provider = provider)
                    writeEndObject()
                }
            }
        }

        // values are written through the provider, so ObjectWriter settings (views, features) apply to them
        private fun ToXmlGenerator.serializeElement(localPart: String, element: Any?, isHeader: Boolean, provider: SerializerProvider) {
            with(this) {
                setNextName(QName(ns, localPart))
                writeFieldName(localPart)

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
                        element.forEach { if (it != null) writeRootElement(it, provider) }
                        writeEndObject()
                    }
                    element is JAXBElement<*> || element?.javaClass?.isAnnotationPresent(XmlRootElement::class.java) == true -> {
                        writeStartObject()
                        writeRootElement(element, provider)
                        writeEndObject()
                    }
                    else -> provider.defaultSerializeValue(element, this)
                }
            }
        }

        /**
         * Writes [element] wrapped in its global element: the `JAXBElement` name or the `@XmlRootElement` one,
         * falling back to the class simple name in the class namespace when there is no annotation.
         */
        private fun ToXmlGenerator.writeRootElement(element: Any, provider: SerializerProvider) {
            val name = when (element) {
                is JAXBElement<*> -> element.name
                else -> {
                    val annotation = element.javaClass.getAnnotation(XmlRootElement::class.java)
                    QName(
                        annotation?.namespace?.takeUnless { it == DEFAULT } ?: NamespaceCache.getNamespace(element.javaClass),
                        annotation?.name?.takeUnless { it == DEFAULT } ?: if (annotation != null) element.javaClass.simpleName.toXmlName() else element.javaClass.simpleName
                    )
                }
            }
            setNextName(name)
            writeFieldName(name.localPart)
            when {
                element !is JAXBElement<*> -> provider.defaultSerializeValue(element, this)
                element.isNil -> XsiNilSerializer.serialize(null, this, provider)
                else -> provider.defaultSerializeValue(element.value, this)
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
                writeString(value)
            }
        }

        private val Any?.isPresent: Boolean get() = this != null && !(this is Iterable<*> && none())

        /** Default `@XmlRootElement` name, as JAXB derives it: `Person` -> `person`, `URLRequest` -> `urlRequest`, `URL` -> `url`. */
        private fun String.toXmlName(): String {
            val leadingUpper = takeWhile { it.isUpperCase() }.length
            val lowered = when {
                leadingUpper <= 1 || leadingUpper == length -> leadingUpper
                else -> leadingUpper - 1
            }
            return take(lowered).lowercase() + drop(lowered)
        }

        private companion object {
            const val DEFAULT = "##default"
        }
    }

    internal object JaxbElementSerializer : StdSerializer<JAXBElement<*>>(JAXBElement::class.java) {
        override fun serialize(value: JAXBElement<*>?, gen: JsonGenerator, provider: SerializerProvider) {
            when {
                value != null && !value.isNil -> provider.defaultSerializeValue(value.value, gen)
                else -> XsiNilSerializer.serialize(null, gen, provider)
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
