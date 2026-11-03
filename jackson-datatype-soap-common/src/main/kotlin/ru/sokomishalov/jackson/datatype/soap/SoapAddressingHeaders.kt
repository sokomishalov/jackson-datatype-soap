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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * WS-Addressing headers. Other headers next to them, and WS-Addressing elements not modelled here, are ignored on read.
 *
 * @author sokomishalov
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SoapAddressingHeaders @JvmOverloads constructor(
    val action: String? = null,
    val messageId: String? = null,
    val from: String? = null,
    val to: String? = null,
    val replyTo: Endpoint? = null,
    val faultTo: Endpoint? = null
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Endpoint @JvmOverloads constructor(
        val address: String? = null,
        val serviceName: String? = null
    )
}