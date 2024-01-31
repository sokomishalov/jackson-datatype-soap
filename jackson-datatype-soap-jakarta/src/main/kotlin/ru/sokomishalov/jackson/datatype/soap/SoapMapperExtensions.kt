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
package ru.sokomishalov.jackson.datatype.soap


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