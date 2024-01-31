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
@file:Suppress(
    "unused",
    "HttpUrlsUsage"
)

package ru.sokomishalov.jackson.datatype.soap

/**
 * @author sokomishalov
 */

object SoapConstants {
    const val XML_SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance"

    const val SOAP_11_ENVELOPE_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/"
    const val SOAP_12_ENVELOPE_NAMESPACE = "http://www.w3.org/2003/05/soap-envelope/"

    const val SOAP_ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing"
    const val SOAP_ADDRESSING_ANONYMOUS = "http://www.w3.org/2005/08/addressing/anonymous"
}