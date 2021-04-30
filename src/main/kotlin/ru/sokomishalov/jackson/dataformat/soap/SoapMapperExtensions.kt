package ru.sokomishalov.jackson.dataformat.soap


/**
 * @author sokomishalov
 */

inline fun <reified H, reified B> SoapMapper.readValue(envelope: String): SoapEnvelope<H, B> {
    return readValue(
        envelope = envelope,
        bodyClass = B::class.java,
        headerClass = H::class.java
    )
}

inline fun <reified B> SoapMapper.readValueBody(envelope: String): B? {
    return readValue(
        envelope = envelope,
        bodyClass = B::class.java,
        headerClass = Nothing::class.java
    ).body
}