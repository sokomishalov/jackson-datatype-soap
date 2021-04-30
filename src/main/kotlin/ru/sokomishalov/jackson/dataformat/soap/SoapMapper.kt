package ru.sokomishalov.jackson.dataformat.soap

import com.fasterxml.jackson.dataformat.xml.XmlMapper


/**
 * @author sokomishalov
 */
class SoapMapper : XmlMapper() {
    init {
        registerModule(SoapModule())
    }
}