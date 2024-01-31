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

package ru.sokomishalov.jackson.datatype.soap

import com.ctc.wstx.stax.WstxOutputFactory
import com.fasterxml.jackson.databind.DeserializationFeature.*
import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS
import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule
import ru.sokomishalov.jackson.datatype.soap.SoapConstants.SOAP_11_ENVELOPE_NAMESPACE
import ru.sokomishalov.jackson.datatype.soap.deser.SoapEnvelopeDeserializers
import ru.sokomishalov.jackson.datatype.soap.ser.SoapEnvelopeSerializers
import ru.sokomishalov.jackson.datatype.soap.util.AlwaysUseNamespacesXmlSerializerProvider

/**
 * @author sokomishalov
 */
open class SoapModule @JvmOverloads constructor(
    private val namespacePrefix: String = "ns",
    private val soapEnvelopeNamespace: String = SOAP_11_ENVELOPE_NAMESPACE
) : JakartaXmlBindAnnotationModule(SoapJaxbAnnotationIntrospector()) {

    override fun setupModule(context: SetupContext) {
        with(context) {
            addSerializers(SoapEnvelopeSerializers(soapEnvelopeNamespace))
            addDeserializers(SoapEnvelopeDeserializers())
            getOwner<ObjectMapper>().apply {
                setSerializerProvider(AlwaysUseNamespacesXmlSerializerProvider())
                enable(ACCEPT_SINGLE_VALUE_AS_ARRAY, ACCEPT_FLOAT_AS_INT)
                enable(ACCEPT_CASE_INSENSITIVE_PROPERTIES, ACCEPT_CASE_INSENSITIVE_ENUMS)
                disable(FAIL_ON_EMPTY_BEANS, WRITE_DATES_AS_TIMESTAMPS)
                disable(FAIL_ON_UNKNOWN_PROPERTIES, FAIL_ON_READING_DUP_TREE_KEY)
                ((factory as? XmlFactory)?.xmlOutputFactory as? WstxOutputFactory)?.apply { config.automaticNsPrefix = namespacePrefix }
            }
            super.setupModule(this)
        }
    }
}