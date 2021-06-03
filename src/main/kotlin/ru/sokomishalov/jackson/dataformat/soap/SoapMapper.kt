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