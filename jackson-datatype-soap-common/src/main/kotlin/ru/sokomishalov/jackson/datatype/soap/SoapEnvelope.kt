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
@file:Suppress("unused")

package ru.sokomishalov.jackson.datatype.soap

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