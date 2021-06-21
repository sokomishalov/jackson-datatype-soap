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
@file:Suppress(
    "CanBeParameter"
)

package ru.sokomishalov.jackson.dataformat.soap

import com.fasterxml.jackson.databind.DeserializationFeature.*
import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import org.codehaus.stax2.XMLOutputFactory2.P_AUTOMATIC_NS_PREFIX
import ru.sokomishalov.jackson.dataformat.soap.SoapConstants.SOAP_11_ENVELOPE_NAMESPACE
import ru.sokomishalov.jackson.dataformat.soap.deser.SoapEnvelopeDeserializers
import ru.sokomishalov.jackson.dataformat.soap.ser.SoapEnvelopeSerializers

/**
 * @author sokomishalov
 */
open class SoapModule @JvmOverloads constructor(
    private val namespacePrefix: String = "ns",
    private val soapEnvelopeNamespace: String = SOAP_11_ENVELOPE_NAMESPACE
) : JaxbAnnotationModule(SoapJaxbAnnotationIntrospector(TypeFactory.defaultInstance())) {

    override fun setupModule(context: SetupContext) {
        with(context) {
            addSerializers(SoapEnvelopeSerializers(soapEnvelopeNamespace))
            addDeserializers(SoapEnvelopeDeserializers())
            getOwner<ObjectMapper>().apply {
                enable(ACCEPT_SINGLE_VALUE_AS_ARRAY, ACCEPT_FLOAT_AS_INT)
                enable(ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                disable(FAIL_ON_EMPTY_BEANS, WRITE_DATES_AS_TIMESTAMPS)
                disable(FAIL_ON_READING_DUP_TREE_KEY, FAIL_ON_UNKNOWN_PROPERTIES)
                (factory as? XmlFactory)?.xmlOutputFactory?.setProperty(P_AUTOMATIC_NS_PREFIX, namespacePrefix)
            }
            super.setupModule(this)
        }
    }
}