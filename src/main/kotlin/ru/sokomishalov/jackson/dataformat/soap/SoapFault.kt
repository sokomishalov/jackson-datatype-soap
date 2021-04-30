package ru.sokomishalov.jackson.dataformat.soap

/**
 * @author sokomishalov
 */
data class SoapFault @JvmOverloads constructor(
    override val message: String? = null,
    val code: String? = null
) : RuntimeException(message ?: "Unknown SOAP error occurred")