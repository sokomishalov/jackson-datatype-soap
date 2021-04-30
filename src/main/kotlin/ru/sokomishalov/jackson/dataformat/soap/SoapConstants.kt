@file:Suppress(
    "unused",
    "HttpUrlsUsage"
)

package ru.sokomishalov.jackson.dataformat.soap

/**
 * @author sokomishalov
 */

object SoapConstants {
    const val XML_SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance"

    const val SOAP_11_ENVELOPE_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/"
    const val SOAP_12_ENVELOPE_NAMESPACE = "http://www.w3.org/2003/05/soap-envelope/"

    const val SOAP_ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing"
    const val SOAP_ADDRESSING_ANONYMOUS = "http://www.w3.org/2005/08/addressing/anonymous"
}