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

import com.fasterxml.jackson.databind.JsonNode

/**
 * SOAP 1.1 or SOAP 1.2 fault, thrown while reading an envelope whose body carries a `Fault`.
 *
 * @property message SOAP 1.1 `faultstring` or the first SOAP 1.2 `Reason/Text`
 * @property code SOAP 1.1 `faultcode` or SOAP 1.2 `Code/Value`
 * @property subcodes SOAP 1.2 `Subcode/Value` chain, outermost first
 * @property node SOAP 1.1 `faultactor` or SOAP 1.2 `Node`
 * @property role SOAP 1.2 `Role`
 * @property detail SOAP 1.1 `detail` or SOAP 1.2 `Detail` contents
 *
 * @author sokomishalov
 */
data class SoapFault @JvmOverloads constructor(
    override val message: String?,
    val code: String?,
    val subcodes: List<String> = emptyList(),
    val node: String? = null,
    val role: String? = null,
    val detail: JsonNode? = null
) : RuntimeException(message ?: "Unknown SOAP error occurred")
