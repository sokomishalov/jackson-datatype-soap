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
package ru.sokomishalov.jackson.dataformat.soap.util

import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import kotlin.text.Charsets.UTF_8

/**
 * @author sokomishalov
 */

inline fun <reified T> random(): T = RANDOM.nextObject(T::class.java)

@PublishedApi
internal val RANDOM: EasyRandom = EasyRandom(
    EasyRandomParameters()
        .charset(UTF_8)
        .collectionSizeRange(1, 2)
        .stringLengthRange(5, 20)
        .scanClasspathForConcreteTypes(true)
        .overrideDefaultInitialization(false)
        .ignoreRandomizationErrors(true)
        .excludeField { it.name in NULL_FIELDS }
)

@JvmField
val NULL_FIELDS: List<String> = listOf("language", "locale", "deathDate", "deathDateStatus")