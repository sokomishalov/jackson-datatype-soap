package ru.sokomishalov.jackson.dataformat.soap

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS
import com.fasterxml.jackson.databind.DeserializationFeature.*
import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UPPER_CAMEL_CASE
import com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.codehaus.stax2.XMLOutputFactory2.P_AUTOMATIC_NS_PREFIX
import ru.sokomishalov.jackson.dataformat.soap.SoapConstants.SOAP_11_ENVELOPE_NAMESPACE
import ru.sokomishalov.jackson.dataformat.soap.deser.SoapEnvelopeDeserializers
import ru.sokomishalov.jackson.dataformat.soap.ser.SoapEnvelopeSerializers

/**
 * @author sokomishalov
 */
open class SoapModule @JvmOverloads constructor(
    private val namespacePrefix: String = "ns",
    private val soapEnvelopeNamespace: String = SOAP_11_ENVELOPE_NAMESPACE,
    private val serializationInclusion: Include = ALWAYS,
    private val annotationIntrospector: JaxbAnnotationIntrospector = SoapJaxbAnnotationIntrospector()
) : JaxbAnnotationModule(annotationIntrospector) {

    override fun setupModule(context: SetupContext) {
        with(context) {
            insertAnnotationIntrospector(annotationIntrospector)
            addSerializers(SoapEnvelopeSerializers(soapEnvelopeNamespace))
            addDeserializers(SoapEnvelopeDeserializers())
            setNamingStrategy(UPPER_CAMEL_CASE)
            getOwner<ObjectMapper>().apply {
                registerKotlinModule()
                setSerializerProvider(AlwaysUseNamespacesXmlSerializerProvider())
                enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
                enable(ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                enable(ACCEPT_FLOAT_AS_INT)
                disable(FAIL_ON_EMPTY_BEANS, WRITE_DATES_AS_TIMESTAMPS)
                disable(FAIL_ON_READING_DUP_TREE_KEY)
                disable(FAIL_ON_UNKNOWN_PROPERTIES)
                setSerializationInclusion(serializationInclusion)
                (factory as? XmlFactory)?.xmlOutputFactory?.setProperty(P_AUTOMATIC_NS_PREFIX, namespacePrefix)
            }
            super.setupModule(this)
        }
    }
}