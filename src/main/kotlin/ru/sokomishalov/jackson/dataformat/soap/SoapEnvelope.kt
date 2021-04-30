package ru.sokomishalov.jackson.dataformat.soap

/**
 * @author sokomishalov
 */
data class SoapEnvelope<H, B> @JvmOverloads constructor(
    val header: H? = null,
    val body: B? = null
) {
    companion object {
        @JvmStatic
        fun <B> fromBody(body: B?): SoapEnvelope<Nothing, B?> = SoapEnvelope(header = null, body = body)
    }
}