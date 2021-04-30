package ru.sokomishalov.jackson.dataformat.soap

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.dataformat.xml.XmlMapper


/**
 * @author sokomishalov
 */
class SoapMapper : XmlMapper() {
    init {
        registerModule(SoapModule())
    }

    fun <H, B> readValue(envelope: String, bodyClass: Class<B>, headerClass: Class<H>?): SoapEnvelope<H, B> {
        return readValue(envelope, createGenericSoapEnvelope(headerClass, bodyClass))
    }

    private fun <H, B> createGenericSoapEnvelope(headerClass: Class<H>?, bodyClass: Class<B>): JavaType {
        return typeFactory.constructParametricType(SoapEnvelope::class.java, headerClass ?: Nothing::class.java, bodyClass)
    }
}