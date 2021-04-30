package ru.sokomishalov.jackson.dataformat.soap

/**
 * @author sokomishalov
 */
data class SoapAddressingHeaders @JvmOverloads constructor(
    val action: String? = null,
    val messageId: String? = null,
    val from: String? = null,
    val to: String? = null,
    val replyTo: Endpoint? = null,
    val faultTo: Endpoint? = null
) {
    data class Endpoint @JvmOverloads constructor(
        val address: String? = null,
        val serviceName: String? = null
    )
}